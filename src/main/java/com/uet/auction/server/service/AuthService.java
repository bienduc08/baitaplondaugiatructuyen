package com.uet.auction.server.service;

import com.uet.auction.common.DTO.UserDTO;
import com.uet.auction.common.Request.AuctionRequest;
import com.uet.auction.common.Request.LoginRequest;
import com.uet.auction.common.Response.AuctionResponse;
import com.uet.auction.server.DAO.UserDAO;

import java.util.List;

public class AuthService {

    private final UserDAO userDAO = new UserDAO();

    // =========================================================
    // XỬ LÝ LỆNH ĐĂNG NHẬP / ĐĂNG KÝ
    // =========================================================

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

    public AuctionResponse register(String fullname,String username,String gmail,String phonenumber, String password, String role) {
        boolean success = userDAO.registerUser(fullname,username,gmail,phonenumber, password, role);
        if (success) {
            return new AuctionResponse(true, "REGISTER_RESULT", "Đăng ký thành công!", null);
        } else {
            return new AuctionResponse(false, "REGISTER_RESULT", "Tên đăng nhập đã tồn tại hoặc lỗi CSDL!", null);
        }
    }

    // =========================================================
    // BỔ SUNG 3 HÀM QUẢN LÝ NGƯỜI DÙNG CHO ADMIN
    // =========================================================

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

        // Trả về type tương ứng cho Client dễ bắt (LOCK hay UNLOCK)
        String type = newStatus.equals("LOCKED") ? "LOCK_USER_RESULT" : "UNLOCK_USER_RESULT";

        if (success) {
            String msg = newStatus.equals("LOCKED") ? "Đã khóa tài khoản thành công!" : "Đã mở khóa tài khoản thành công!";
            return new AuctionResponse(true, type, msg, null);
        } else {
            return new AuctionResponse(false, type, "Cập nhật trạng thái thất bại!", null);
        }
    }

    public AuctionResponse deposit(String username, double amount) {
        if (username == null || username.isBlank()) {
            return new AuctionResponse(false, "DEPOSIT_RESULT", "Phiên đăng nhập không hợp lệ!", null);
        }
        if (amount <= 0) {
            return new AuctionResponse(false, "DEPOSIT_RESULT", "Số tiền nạp phải lớn hơn 0!", null);
        }
        if (amount > 500_000_000) {
            return new AuctionResponse(false, "DEPOSIT_RESULT", "Mỗi lần nạp tối đa 500.000.000 VNĐ!", null);
        }

        String status = userDAO.getStatus(username);
        if (status == null) {
            return new AuctionResponse(false, "DEPOSIT_RESULT", "Không tìm thấy tài khoản!", null);
        }
        if ("LOCKED".equalsIgnoreCase(status)) {
            return new AuctionResponse(false, "DEPOSIT_RESULT", "Tài khoản bị khóa, không thể nạp tiền!", null);
        }

        Double newBalance = userDAO.deposit(username, amount);
        if (newBalance == null) {
            return new AuctionResponse(false, "DEPOSIT_RESULT", "Nạp tiền thất bại!", null);
        }
        return new AuctionResponse(true, "DEPOSIT_RESULT",
                String.format("Nạp thành công %,.0f VNĐ!\nSố dư mới: %,.0f VNĐ", amount, newBalance),
                newBalance);
    }

    public double getUserBalance(String username) {
        return userDAO.getBalance(username);
    }
    // Thêm hàm này vào AuthService.java
    public AuctionResponse updateProfile(String username, String fullName, String phone, String oldPass, String newPass) {
        // 1. Nếu người dùng có nhập mật khẩu mới, bắt buộc phải kiểm tra mật khẩu cũ
        if (newPass != null && !newPass.trim().isEmpty()) {
            if (oldPass == null || oldPass.trim().isEmpty()) {
                return new AuctionResponse(false, "UPDATE_PROFILE_FAILED", "Vui lòng nhập mật khẩu cũ để đổi mật khẩu!", null);
            }
            UserDTO checkUser = userDAO.checkLogin(username, oldPass);
            if (checkUser == null) {
                return new AuctionResponse(false, "UPDATE_PROFILE_FAILED", "Mật khẩu cũ không chính xác!", null);
            }
        }

        // 2. Gọi DAO để lưu xuống Database
        boolean success = userDAO.updateProfile(username, fullName, phone, newPass);

        if (success) {
            List<UserDTO> result = userDAO.searchUser(username);
            if (result == null || result.isEmpty()) {
                return new AuctionResponse(false, "UPDATE_PROFILE_FAILED", "Không tìm thấy tài khoản sau khi cập nhật!", null);
            }
            UserDTO updatedUser = result.get(0);
            return new AuctionResponse(true, "UPDATE_PROFILE_SUCCESS", "Cập nhật hồ sơ thành công!", updatedUser);
        } else {
            return new AuctionResponse(false, "UPDATE_PROFILE_FAILED", "Cập nhật thất bại. Vui lòng thử lại!", null);
        }
    }
}