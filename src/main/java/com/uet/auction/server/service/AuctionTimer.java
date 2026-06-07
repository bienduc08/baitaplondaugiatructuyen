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
                // Bước 1: Mở các phiên APPROVED đã đến giờ start_time → OPEN
                productDAO.openScheduledAuctions();

                // Bước 2: Anti-sniping — gia hạn phiên có bid trong 30 giây cuối
                productDAO.extendAuctionIfLastBid();

                // Bước 3: Auto-bid — phải chạy TRƯỚC khi đóng phiên
                // để không tốn query trên phiên đã CLOSED
                AuctionService.getInstance().triggerAllAutoBids();

                // Bước 4: Đóng các phiên OPEN đã hết giờ → CLOSED
                // Nhận lại danh sách phiên vừa đóng để broadcast người thắng
                List<Map<String, Object>> closedAuctions = productDAO.closeExpiredAuctions();

                // Bước 4.5: Dọn registry auto-bid cho các phiên vừa đóng
                List<Integer> closedIds = new java.util.ArrayList<>();
                for (Map<String, Object> info : closedAuctions) {
                    closedIds.add((Integer) info.get("productId"));
                }
                AuctionService.getInstance().clearClosedAuctions(closedIds);

                // Bước 5: Broadcast riêng AUCTION_ENDED cho từng phiên vừa đóng
                boolean hasClosedAuction = !closedAuctions.isEmpty();
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

                    SocketServer.broadcastToLoggedInUsers(new AuctionResponse(true, "AUCTION_ENDED", message, info));
                }

                // Bước 6: Broadcast UPDATE_PRICE để client refresh danh sách
                // Nếu đã gửi AUCTION_ENDED thì client đã reload rồi — chỉ gửi khi không có phiên đóng
                // hoặc gửi thêm để đảm bảo tất cả controller đều cập nhật
                SocketServer.broadcastToLoggedInUsers(new AuctionResponse(true, "UPDATE_PRICE", null));

            } catch (Exception e) {
                System.err.println("[AuctionTimer] Lỗi: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    public void stopChecking() {
        scheduler.shutdown();
    }
}