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
                // 1. Mở các phiên APPROVED đã đến giờ start_time → OPEN
                productDAO.openScheduledAuctions();

                // 2. Anti-sniping: gia hạn nếu có bid trong 30 giây cuối
                productDAO.extendAuctionIfLastBid();

                // ĐÃ SỬA LỖI 4: Kích hoạt Auto-bid TRƯỚC khi chốt sổ đóng phiên.
                // Tránh việc phiên vừa bị update thành CLOSED ở bước dưới,
                // Auto-bid lại mò vào đặt giá tiếp sinh ra lỗi logic.
                AuctionService.getInstance().triggerAllAutoBids();

                // 3. Đóng các phiên OPEN đã hết giờ → CLOSED
                List<Map<String, Object>> closedAuctions = productDAO.closeExpiredAuctions();

                // ĐÃ SỬA LỖI 5: Gộp logic gửi Broadcast để tránh Client bị reload 2 lần
                boolean hasClosedAuctions = closedAuctions != null && !closedAuctions.isEmpty();

                if (hasClosedAuctions) {
                    // 4a. Nếu có phiên kết thúc, chỉ gửi AUCTION_ENDED
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
                    // KHÔNG gọi gửi UPDATE_PRICE ở đây nữa vì AUCTION_ENDED đã làm client tự reload lại List rồi.
                } else {
                    // 4b. Chỉ khi không có phiên nào đóng, ta mới gửi UPDATE_PRICE định kỳ 5s
                    // để các client đồng bộ đếm ngược thời gian.
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