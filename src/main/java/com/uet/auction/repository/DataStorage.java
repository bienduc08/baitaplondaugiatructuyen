package com.uet.auction.repository;

import com.uet.auction.model.Auction;
import com.uet.auction.model.AuctionStatus;
import com.uet.auction.model.BidTransaction;
import com.uet.auction.model.Item;
import com.uet.auction.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DataStorage {

    private static final Logger log = LoggerFactory.getLogger(DataStorage.class);

    private final List<User>           users           = new ArrayList<>();
    private final List<Item>           pendingItems    = new ArrayList<>();
    private final List<Item>           approvedItems   = new ArrayList<>();
    private final List<Auction>        auctions        = new ArrayList<>();
    private final List<BidTransaction> bidTransactions = new ArrayList<>();

    // ── User ──────────────────────────────────────────────────────────────────

    public void addUser(User user) {
        users.add(user);
        log.info("Thêm user: {}", user.getUsername());
    }

    public Optional<User> findUserByUsername(String username) {
        return users.stream().filter(u -> u.getUsername().equals(username)).findFirst();
    }

    public Optional<User> findUserById(int id) {
        return users.stream().filter(u -> u.getId() == id).findFirst();
    }

    public List<User> getAllUsers() { return new ArrayList<>(users); }

    public void updateUser(User updated) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId() == updated.getId()) {
                users.set(i, updated);
                log.info("Cập nhật user: {}", updated.getUsername());
                return;
            }
        }
        log.warn("Không tìm thấy user id={} để cập nhật", updated.getId());
    }

    // ── Item ──────────────────────────────────────────────────────────────────

    public void addPendingItem(Item item) {
        pendingItems.add(item);
        log.info("Item chờ duyệt: name={}", item.getName());
    }

    public List<Item> getPendingItems() { return new ArrayList<>(pendingItems); }

    public void approveItem(Item item) {
        pendingItems.remove(item);
        approvedItems.add(item);
        log.info("Duyệt item: name={}", item.getName());
    }

    public void rejectItem(Item item) {
        pendingItems.remove(item);
        log.info("Từ chối item: name={}", item.getName());
    }

    public List<Item> getApprovedItems() { return new ArrayList<>(approvedItems); }

    public Optional<Item> findItemById(int id) {
        return approvedItems.stream().filter(i -> i.getId() == id).findFirst();
    }

    // ── Auction ───────────────────────────────────────────────────────────────

    public void addAuction(Auction auction) {
        auctions.add(auction);
        log.info("Thêm phiên đấu giá: itemId={}", auction.getItemId());
    }

    public List<Auction> getActiveAuctions() {
        return auctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.OPEN
                        || a.getStatus() == AuctionStatus.RUNNING)
                .collect(Collectors.toList());
    }

    public List<Auction> getAllAuctions() { return new ArrayList<>(auctions); }

    public Optional<Auction> findAuctionById(int id) {
        return auctions.stream().filter(a -> a.getId() == id).findFirst();
    }

    public void updateAuction(Auction updated) {
        for (int i = 0; i < auctions.size(); i++) {
            if (auctions.get(i).getId() == updated.getId()) {
                auctions.set(i, updated);
                log.info("Cập nhật phiên đấu giá: id={}", updated.getId());
                return;
            }
        }
        log.warn("Không tìm thấy auction id={} để cập nhật", updated.getId());
    }

    // ── BidTransaction ────────────────────────────────────────────────────────

    public void addBidTransaction(BidTransaction tx) {
        bidTransactions.add(tx);
        log.info("Ghi nhận đặt giá: auctionId={}, bidderId={}, amount={}",
                tx.getAuctionId(), tx.getBidderId(), tx.getAmount());
    }

    public List<BidTransaction> getBidsByAuction(int auctionId) {
        return bidTransactions.stream()
                .filter(t -> t.getAuctionId() == auctionId)
                .collect(Collectors.toList());
    }

    public List<BidTransaction> getBidsByBidder(int bidderId) {
        return bidTransactions.stream()
                .filter(t -> t.getBidderId() == bidderId)
                .collect(Collectors.toList());
    }
}