package com.uet.auction.controller;

import com.uet.auction.model.AutoBidConfig;
import com.uet.auction.model.User;
import com.uet.auction.service.AuctionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuctionController {

    private static final Logger log = LoggerFactory.getLogger(AuctionController.class);

    private final AuctionService auctionService;

    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    public Object processRequest(String action, Object request) {
        try {
            return switch (action) {
                case "BID_PLACE"        -> handleBid((BidRequest) request);
                case "AUTOBID_REGISTER" -> handleAutoBidRegister((AutoBidConfig) request);
                case "AUTOBID_CANCEL"   -> handleAutoBidCancel((AutoBidConfig) request);
                default -> {
                    log.warn("Action không hợp lệ: {}", action);
                    yield "ACTION_UNKNOWN";
                }
            };
        } catch (Exception e) {
            log.error("Lỗi xử lý action [{}]: {}", action, e.getMessage(), e);
            return false;
        }
    }

    private boolean handleBid(BidRequest request) {
        User user = request.getUser();
        if (!user.canBid()) {
            log.warn("User [{}] không có quyền đấu giá (role={})",
                    user.getUsername(), user.getRole());
            return false;
        }
        log.info("Xử lý đặt giá: user={}, auctionId={}, amount={}",
                user.getUsername(), request.getAuctionId(), request.getAmount());
        boolean result = auctionService.placeBid(user, request.getAuctionId(), request.getAmount());
        log.info("Kết quả đặt giá [{}]: {}", user.getUsername(), result ? "Thành công" : "Thất bại");
        return result;
    }

    private String handleAutoBidRegister(AutoBidConfig config) {
        log.info("Đăng ký auto-bid: bidderId={}, auctionId={}, maxBid={}",
                config.getBidderId(), config.getAuctionId(), config.getMaxBid());
        auctionService.registerAutoBid(config);
        return "AUTOBID_REGISTER_SUCCESS";
    }

    private String handleAutoBidCancel(AutoBidConfig config) {
        log.info("Hủy auto-bid: bidderId={}, auctionId={}",
                config.getBidderId(), config.getAuctionId());
        auctionService.cancelAutoBid(config);
        return "AUTOBID_CANCEL_SUCCESS";
    }
}