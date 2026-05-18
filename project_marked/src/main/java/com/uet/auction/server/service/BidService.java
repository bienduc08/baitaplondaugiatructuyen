package com.uet.auction.server.service;

import com.uet.auction.common.Response.AuctionResponse;
import com.uet.auction.server.DAO.BidDAO;
import com.uet.auction.server.network.SocketServer;

public class BidService {

    private final BidDAO bidDAO = new BidDAO();

    /**
     * Lưu ý: BidService.processBid() hiện không được gọi từ ClientHandler.
     * ClientHandler đang dùng AuctionService.placeBid() (đã được sửa gọi BidDAO).
     * File này giữ lại để tham khảo, có thể dùng sau nếu muốn tách logic.
     *
     * SỬA: constructor AuctionResponse sai (tham số 2 là TYPE, không phải message)
     */
    public AuctionResponse processBid(int productId, String user, double amount) {
        boolean success = bidDAO.placeBid(productId, user, amount);
        if (success) {
            SocketServer.broadcast(new AuctionResponse(true, "UPDATE_PRICE", null));
            return new AuctionResponse(true, "BID_RESULT", "Đặt giá thành công!", null);
        }
        // SỬA: đổi từ new AuctionResponse(false, "Giá đặt không hợp lệ...", null)
        // thành đúng constructor 4 tham số
        return new AuctionResponse(false, "BID_RESULT",
                "Giá đặt không hợp lệ hoặc lỗi DB.", null);
    }
}