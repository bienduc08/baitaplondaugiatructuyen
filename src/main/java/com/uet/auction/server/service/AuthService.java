package com.uet.auction.server.service;

import com.uet.auction.common.DTO.UserDTO;
import com.uet.auction.common.Request.AuctionRequest;
import com.uet.auction.common.Request.LoginRequest;
import com.uet.auction.common.Response.AuctionResponse;
import com.uet.auction.server.DAO.UserDAO;

public class AuthService {

    private UserDAO userDAO = new UserDAO();

    // Xử lý lệnh Đăng Nhập

    public AuctionResponse login(AuctionRequest request) {
        LoginRequest loginReq = (LoginRequest) request.getData();
        UserDTO user = userDAO.checkLogin(loginReq.getUsername(), loginReq.getPassword());
        if (user != null) {
            return new AuctionResponse(true, "LOGIN_RESULT", user);
        } else {
            // SỬA: dùng constructor 4 tham số — tham số 3 là message, tham số 4 là data
            return new AuctionResponse(false, "LOGIN_RESULT", "Sai tên đăng nhập hoặc mật khẩu!", null);
        }
    }

    public AuctionResponse register(String username, String password, String role) {
        boolean success = userDAO.registerUser(username, password, role);
        if (success) {
            return new AuctionResponse(true, "REGISTER_RESULT", "Đăng ký thành công!", null);
        } else {
            return new AuctionResponse(false, "REGISTER_RESULT", "Tên đăng nhập đã tồn tại hoặc lỗi CSDL!", null);
        }
    }
}