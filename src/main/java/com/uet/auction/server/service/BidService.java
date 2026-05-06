package com.uet.auction.server.service;

import com.uet.auction.server.DAO.BidDAO;
import com.uet.auction.common.Response.AuctionResponse;
import com.uet.auction.server.network.SocketServer;

public class BidService {
    private BidDAO bidDAO = new BidDAO();

    public AuctionResponse processBid(int productId, String user, double amount) {
        boolean success = bidDAO.placeBid(productId, user, amount);
        if (success) {
            AuctionResponse res = new AuctionResponse(true, "BID_SUCCESS", null);
            // Sau khi đặt giá thành công, báo cho tất cả mọi người cập nhật UI
            SocketServer.broadcast(new AuctionResponse(true, "UPDATE_PRICE", null));
            return res;
        }
        return new AuctionResponse(false, "Giá đặt không hợp lệ hoặc lỗi DB", null);
    }
}
