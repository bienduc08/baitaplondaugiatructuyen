package com.uet.auction.service;

import com.uet.auction.model.*;
import com.uet.auction.repository.DataStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AuctionService {

    private static final Logger log = LoggerFactory.getLogger(AuctionService.class);

    private final DataStorage dataStorage;
    private final List<AutoBidConfig> autoBidConfigs = new ArrayList<>();

    public AuctionService(DataStorage dataStorage) {
        this.dataStorage = dataStorage;
    }

    // ── Đặt giá ───────────────────────────────────────────────────────────────

    public boolean placeBid(User bidder, int auctionId, BigDecimal amount) {
        try {
            Optional<Auction> opt = dataStorage.findAuctionById(auctionId);
            if (opt.isEmpty()) {
                log.warn("Không tìm thấy phiên: auctionId={}", auctionId);
                return false;
            }
            Auction auction = opt.get();

            if (auction.getStatus() != AuctionStatus.OPEN
                    && auction.getStatus() != AuctionStatus.RUNNING) {
                log.warn("Phiên không còn mở: auctionId={}, status={}", auctionId, auction.getStatus());
                return false;
            }

            if (auction.getEndTime() != null && LocalDateTime.now().isAfter(auction.getEndTime())) {
                log.warn("Phiên đã hết giờ: auctionId={}", auctionId);
                return false;
            }

            BigDecimal currentPrice = auction.getCurrentPrice() != null
                    ? auction.getCurrentPrice() : auction.getStartingPrice();

            if (amount.compareTo(currentPrice) <= 0) {
                log.warn("Giá đặt [{}] không cao hơn giá hiện tại [{}]", amount, currentPrice);
                return false;
            }

            if (bidder.getBalance().compareTo(amount) < 0) {
                log.warn("Số dư không đủ: user={}, balance={}, amount={}",
                        bidder.getUsername(), bidder.getBalance(), amount);
                return false;
            }

            dataStorage.addBidTransaction(
                    new BidTransaction(0, LocalDateTime.now(), auctionId, bidder.getId(), amount, false));

            auction.setCurrentPrice(amount);
            auction.setHighestBidderId(bidder.getId());
            auction.setStatus(AuctionStatus.RUNNING);
            dataStorage.updateAuction(auction);

            log.info("Đặt giá thành công: user={}, auctionId={}, amount={}",
                    bidder.getUsername(), auctionId, amount);

            triggerAutoBid(auction, bidder.getId());
            return true;

        } catch (Exception e) {
            log.error("Lỗi placeBid: auctionId={}, error={}", auctionId, e.getMessage(), e);
            return false;
        }
    }

    // ── Auto-bid ──────────────────────────────────────────────────────────────

    public void registerAutoBid(AutoBidConfig config) {
        autoBidConfigs.removeIf(c -> c.getBidderId() == config.getBidderId()
                && c.getAuctionId() == config.getAuctionId());
        autoBidConfigs.add(config);
        log.info("Đăng ký auto-bid: bidderId={}, auctionId={}, maxBid={}",
                config.getBidderId(), config.getAuctionId(), config.getMaxBid());
    }

    public void cancelAutoBid(AutoBidConfig config) {
        boolean removed = autoBidConfigs.removeIf(c -> c.getBidderId() == config.getBidderId()
                && c.getAuctionId() == config.getAuctionId());
        log.info("Hủy auto-bid: bidderId={}, auctionId={}, kết quả={}",
                config.getBidderId(), config.getAuctionId(), removed ? "OK" : "Không tìm thấy");
    }

    private void triggerAutoBid(Auction auction, int lastBidderId) {
        BigDecimal currentPrice = auction.getCurrentPrice();
        autoBidConfigs.stream()
                .filter(c -> c.getAuctionId() == auction.getId() && c.getBidderId() != lastBidderId)
                .sorted()
                .forEach(config -> {
                    BigDecimal nextBid = config.calculateNextBid(currentPrice);
                    if (nextBid == null) return;
                    dataStorage.findUserById(config.getBidderId()).ifPresent(user -> {
                        dataStorage.addBidTransaction(new BidTransaction(
                                0, LocalDateTime.now(), auction.getId(), user.getId(), nextBid, true));
                        auction.setCurrentPrice(nextBid);
                        auction.setHighestBidderId(user.getId());
                        dataStorage.updateAuction(auction);
                        log.info("Auto-bid: bidderId={}, amount={}", config.getBidderId(), nextBid);
                    });
                });
    }

    // ── Admin ──────────────────────────────────────────────────────────────────

    public void processApproval(Item item, boolean isApproved, String reason) {
        try {
            if (isApproved) {
                dataStorage.approveItem(item);
                Auction auction = new Auction(
                        item.getId(),
                        Integer.parseInt(item.getSellerId()),
                        item.getStartingPrice(),
                        LocalDateTime.now(),
                        LocalDateTime.now().plusHours(24));
                dataStorage.addAuction(auction);
                notifyAllUsers("Sản phẩm mới [" + item.getName() + "] đã lên sàn!");
                log.info("Duyệt item: name={}", item.getName());
            } else {
                dataStorage.rejectItem(item);
                notify(item.getSellerId(), "Sản phẩm '" + item.getName()
                        + "' bị từ chối. Lý do: " + reason);
                log.info("Từ chối item: name={}, reason={}", item.getName(), reason);
            }
        } catch (Exception e) {
            log.error("Lỗi processApproval: name={}, error={}", item.getName(), e.getMessage(), e);
        }
    }

    public void updateAuctionTime(int itemId, LocalDateTime start, LocalDateTime end) {
        dataStorage.getAllAuctions().stream()
                .filter(a -> a.getItemId() == itemId)
                .findFirst()
                .ifPresentOrElse(auction -> {
                    auction.setStartTime(start);
                    auction.setEndTime(end);
                    dataStorage.updateAuction(auction);
                    dataStorage.findItemById(itemId).ifPresent(item ->
                            notifyAllUsers("Cập nhật giờ: [" + item.getName() + "] "
                                    + start + " → " + end));
                    log.info("Cập nhật thời gian: itemId={}, start={}, end={}", itemId, start, end);
                }, () -> log.warn("Không tìm thấy phiên cho itemId={}", itemId));
    }

    // ── Seller ────────────────────────────────────────────────────────────────

    public void submitItemForApproval(String name, String description,
                                      BigDecimal price, String sellerId,
                                      ItemCategory category) {
        Item item = new Item(name, description, price, sellerId, category) {
            @Override
            public void printInfo() {
                System.out.println("[" + category + "] " + name + " | Giá KĐ: " + price);
            }
        };
        dataStorage.addPendingItem(item);
        log.info("Seller gửi sản phẩm: name={}, category={}, seller={}", name, category, sellerId);
    }

    // ── Thông báo ─────────────────────────────────────────────────────────────

    public void notifyAllUsers(String message) {
        log.info("[BROADCAST] {}", message);
        dataStorage.getAllUsers().forEach(u ->
                System.out.println("📢 [" + u.getUsername() + "] " + message));
    }

    public void notify(String sellerId, String message) {
        log.info("[NOTIFY → sellerId={}] {}", sellerId, message);
        System.out.println("📩 [Seller " + sellerId + "] " + message);
    }
}