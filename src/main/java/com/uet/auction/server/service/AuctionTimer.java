package com.uet.auction.server.service;

import com.uet.auction.common.Response.AuctionResponse;
import com.uet.auction.server.DAO.ProductDAO;
import com.uet.auction.server.network.SocketServer;

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
                // openScheduledAuctions() trả về void nên ta dùng flag
                productDAO.openScheduledAuctions();

                // Anti-sniping: gia hạn nếu có bid trong 30 giây cuối
                productDAO.extendAuctionIfLastBid();
                // 2. Đóng các phiên OPEN đã hết giờ end_time → CLOSED
                productDAO.closeExpiredAuctions();

                // SỬA: luôn broadcast sau mỗi chu kỳ để client tự refresh
                // (an toàn hơn vì openScheduledAuctions/closeExpiredAuctions trả về void)
                SocketServer.broadcast(new AuctionResponse(true, "UPDATE_PRICE", null));

            } catch (Exception e) {
                System.err.println("Lỗi AuctionTimer: " + e.getMessage());
            }
        }, 0, 5, TimeUnit.SECONDS); // SỬA: 30 giây thay vì 1 giây (giảm tải DB)
    }

    public void stopChecking() {
        scheduler.shutdown();
    }
}