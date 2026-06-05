package com.uet.auction.server.service;

import com.uet.auction.common.Response.AuctionResponse;
import com.uet.auction.server.DAO.ProductDAO;
import com.uet.auction.server.network.SocketServer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionTimer {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ProductDAO productDAO = new ProductDAO();

    public void startChecking() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // 1. Mở các phiên đến giờ
                productDAO.openScheduledAuctions();

                // 2. Anti-sniping: gia hạn
                productDAO.extendAuctionIfLastBid();

                // 3. KÍCH HOẠT AUTO-BID TRƯỚC KHI CHỐT SỔ ĐÓNG PHIÊN
                AuctionService.getInstance().triggerAllAutoBids();

                // 4. Đóng các phiên hết giờ
                List<Map<String, Object>> closedAuctions = productDAO.closeExpiredAuctions();
                boolean hasClosedAuctions = closedAuctions != null && !closedAuctions.isEmpty();

                if (hasClosedAuctions) {
                    for (Map<String, Object> info : closedAuctions) {
                        String winner      = (String) info.get("winner");
                        String productName = (String) info.get("productName");
                        double finalPrice  = (Double) info.get("finalPrice");

                        String message;
                        if (winner != null && !winner.isBlank()) {
                            message = String.format(
                                    "Phiên \"%s\" đã kết thúc!\n🏆 Người thắng: %s\n💰 Giá trúng: %,.0f VNĐ",
                                    productName, winner, finalPrice
                            );
                        } else {
                            message = String.format(
                                    "Phiên \"%s\" đã kết thúc.\nKhông có người tham gia đặt giá.",
                                    productName
                            );
                        }
                        SocketServer.broadcast(new AuctionResponse(true, "AUCTION_ENDED", message, info));
                    }
                } else {
                    // Nếu không có phiên đóng mới gửi UPDATE_PRICE để tránh client load 2 lần
                    SocketServer.broadcast(new AuctionResponse(true, "UPDATE_PRICE", null));
                }

            } catch (Exception e) {
                System.err.println("Lỗi AuctionTimer: " + e.getMessage());
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    public void stopChecking() {
        scheduler.shutdown();
    }
}