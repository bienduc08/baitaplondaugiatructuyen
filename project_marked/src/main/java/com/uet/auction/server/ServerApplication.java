package com.uet.auction.server;

import com.uet.auction.server.network.SocketServer;
import com.uet.auction.server.service.AuctionTimer;
import com.uet.auction.server.util.Logger;

public class ServerApplication {
    public static void main(String[] args) {
        int port = 8080;

        // BƯỚC 1: Workflow kiểm tra thời gian (Chạy ngầm)
        AuctionTimer auctionTimer = new AuctionTimer();
        auctionTimer.startChecking(); // Tự động chuyển PENDING -> OPEN -> CLOSED

        // BƯỚC 2: Workflow nhận kết nối (Socket)
        SocketServer server = new SocketServer(); // Khởi tạo không tham số
        Logger.info("Server đang đợi kết nối tại cổng " + port);

        server.start(port); // Truyền port vào hàm start
    }
}