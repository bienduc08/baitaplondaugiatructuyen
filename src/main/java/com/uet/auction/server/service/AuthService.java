package com.uet.auction.server.service;

import com.uet.auction.common.Request.LoginRequest;
import com.uet.auction.common.Response.AuctionResponse;
import com.uet.auction.common.DTO.UserDTO;
import com.uet.auction.server.DAO.UserDAO;
import com.uet.auction.server.network.SocketServer;

public class AuthService {
    private UserDAO userDAO = new UserDAO();

    public AuctionResponse authenticate(LoginRequest loginData) {
        boolean success = userDAO.checkLogin(loginData.getUsername(), loginData.getPassword());
        if (success) {
            // Giả sử lấy số dư mặc định hoặc từ DB
            UserDTO user = new UserDTO(loginData.getUsername(), 5000.0);
            return new AuctionResponse(true, "LOGIN_SUCCESS", user);
        }
        return new AuctionResponse(false, "Sai tài khoản hoặc mật khẩu", null);
    }
    // Trong com.uet.auction.server.service.AuctionService

    public AuctionResponse getProductsByStatus(String status) {
        List<ProductDTO> list = productDAO.getProductsByStatus(status);
        return new AuctionResponse(true, "GET_PRODUCTS_RESULT", list);
    }

    public AuctionResponse changeProductStatus(int productId, String newStatus) {
        boolean success = productDAO.updateProductStatus(productId, newStatus);
        if (success) {
            // Khi Admin duyệt xong, thông báo cho mọi người làm mới giao diện
            SocketServer.broadcast(new AuctionResponse(true, "UPDATE_PRICE", null));
            return new AuctionResponse(true, "CHANGE_STATUS_RESULT", "Đã cập nhật trạng thái thành: " + newStatus);
        }
        return new AuctionResponse(false, "CHANGE_STATUS_RESULT", "Lỗi CSDL khi cập nhật trạng thái.");
    }
}