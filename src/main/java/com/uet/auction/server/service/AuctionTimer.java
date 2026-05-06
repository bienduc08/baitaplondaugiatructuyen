package com.uet.auction.server.service;

import com.uet.auction.server.DAO.ProductDAO;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionTimer {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ProductDAO productDAO = new ProductDAO();

    public void startChecking() {
        // Lập lịch cho code chạy lặp đi lặp lại
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // 1. Quét và Tự động MỞ các phiên đã được Admin duyệt và đến giờ
                productDAO.openScheduledAuctions();

                // 2. Quét và Tự động ĐÓNG các phiên đang mở nhưng đã hết giờ
                productDAO.closeExpiredAuctions();

            } catch (Exception e) {
                System.err.println("Lỗi luồng AuctionTimer: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.SECONDS); // 1 SECONDS: Cứ mỗi 1 giây sẽ chạy quét database 1 lần
    }

    // Đừng quên hàm tắt timer khi tắt Server
    public void stopChecking() {
        scheduler.shutdown();
    }
}