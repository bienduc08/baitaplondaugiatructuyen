package com.uet.auction.server;

import com.uet.auction.server.network.SocketServer;
import com.uet.auction.server.util.Logger;
import com.uet.auction.server.service.AuctionTimer;

public class ServerApplication {
    public static void main(String[] args) {
        int port = 8080;
        SocketServer server = new SocketServer(port);
        Logger.info("Auction Server đang khởi động tại port " + port + "...");
        server.start();
        SocketServer server = new SocketServer(8080);

        // 2. Khởi động bộ đếm thời gian đấu giá
        AuctionTimer timer = new AuctionTimer();
        timer.startChecking();

        Logger.info("Auction Server & Timer đang chạy...");
        server.start();
    }
}