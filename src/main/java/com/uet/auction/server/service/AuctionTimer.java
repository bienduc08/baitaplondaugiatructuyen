package com.uet.auction.server.service;

import com.uet.auction.common.Response.AuctionResponse;
import com.uet.auction.server.DAO.NotificationDAO;
import com.uet.auction.server.DAO.ProductDAO;
import com.uet.auction.server.network.SocketServer;

import java.math.BigDecimal;
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
                //  Mở các phiên đến giờ
                productDAO.openScheduledAuctions();

                //  Anti-sniping: gia hạn trước khi đóng, để bid cuối cùng kịp extend
                //  KÍCH HOẠT AUTO-BID TRƯỚC KHI CHỐT SỔ ĐÓNG PHIÊN
                AuctionService.getInstance().triggerAllAutoBids();
                productDAO.extendAuctionIfLastBid();

                //  Đóng các phiên hết giờ
                List<Map<String, Object>> closedAuctions = productDAO.closeExpiredAuctions();

                if (closedAuctions == null || closedAuctions.isEmpty()) return;

                NotificationDAO notifDAO = new NotificationDAO();

                for (Map<String, Object> info : closedAuctions) {
                    String winner      = (String) info.get("winner");
                    String productName = (String) info.get("productName");
                    BigDecimal finalPrice  = (BigDecimal) info.get("finalPrice");

                    String message;
                    if (winner != null && !winner.isBlank()) {
                        message = String.format(
                                "Phiên \"%s\" đã kết thúc!\n🏆 Người thắng: %s\n💰 Giá trúng: %,.0f VNĐ",
                                productName, winner, finalPrice
                        );
                        try {
                            notifDAO.insertNotification(winner,
                                    String.format("Chúc mừng! Bạn đã thắng phiên đấu giá sản phẩm \"%s\" với mức giá %,.0f VNĐ.",
                                            productName, finalPrice),
                                    "AUCTION_WON");
                        } catch (Exception e) {
                            System.err.println("[AuctionTimer] Lỗi lưu thông báo thắng cuộc: " + e.getMessage());
                        }
                    } else {
                        message = String.format(
                                "Phiên \"%s\" đã kết thúc.\nKhông có người tham gia đặt giá.",
                                productName
                        );
                    }

                    SocketServer.broadcastToLoggedInUsers(
                            new AuctionResponse(true, "AUCTION_ENDED", message, info)
                    );
                }

            } catch (Exception e) {
                System.err.println("Lỗi AuctionTimer: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void stopChecking() {
        scheduler.shutdown();
    }
}