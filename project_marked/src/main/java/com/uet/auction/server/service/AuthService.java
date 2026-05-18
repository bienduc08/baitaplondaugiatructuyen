package com.uet.auction.server.service;

import com.uet.auction.common.DTO.UserDTO;
import com.uet.auction.common.Request.AuctionRequest;
import com.uet.auction.common.Request.LoginRequest;
import com.uet.auction.common.Response.AuctionResponse;
import com.uet.auction.server.DAO.UserDAO;

import java.util.List;

public class AuthService {

    private UserDAO userDAO = new UserDAO();

    public AuctionResponse login(AuctionRequest request) {
        LoginRequest loginReq = (LoginRequest) request.getData();
        UserDTO user = userDAO.checkLogin(loginReq.getUsername(), loginReq.getPassword());
        if (user != null) {
            // [THÊM MỚI] Chặn tài khoản bị khóa đăng nhập
            // File gốc không có đoạn kiểm tra này, user LOCKED vẫn login được bình thường
            if ("LOCKED".equalsIgnoreCase(user.getStatus())) {
                return new AuctionResponse(false, "LOGIN_RESULT",
                        "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ Admin!", null);
            }
            // [KẾT THÚC THÊM MỚI]
            return new AuctionResponse(true, "LOGIN_RESULT", user);
        } else {
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

    // Các hàm getAllUsers, searchUser, changeUserStatus bên dưới
    // đã có sẵn trong file gốc, KHÔNG thay đổi gì

    public AuctionResponse getAllUsers() {
        List<UserDTO> users = userDAO.getAllUsers();
        if (users != null) {
            return new AuctionResponse(true, "GET_ALL_USERS_RESULT", users);
        } else {
            return new AuctionResponse(false, "GET_ALL_USERS_RESULT", "Lỗi khi lấy danh sách người dùng", null);
        }
    }

    public AuctionResponse searchUser(String keyword) {
        List<UserDTO> users = userDAO.searchUser(keyword);
        if (users != null) {
            return new AuctionResponse(true, "SEARCH_USER_RESULT", users);
        } else {
            return new AuctionResponse(false, "SEARCH_USER_RESULT", "Lỗi khi tìm kiếm người dùng", null);
        }
    }

    public AuctionResponse changeUserStatus(int userId, String newStatus) {
        boolean success = userDAO.changeUserStatus(userId, newStatus);
        String type = newStatus.equals("LOCKED") ? "LOCK_USER_RESULT" : "UNLOCK_USER_RESULT";
        if (success) {
            String msg = newStatus.equals("LOCKED") ? "Đã khóa tài khoản thành công!" : "Đã mở khóa tài khoản thành công!";
            return new AuctionResponse(true, type, msg, null);
        } else {
            return new AuctionResponse(false, type, "Cập nhật trạng thái thất bại!", null);
        }
    }
}
