package com.uet.auction.server.service;

import com.uet.auction.common.DTO.UserDTO;
import com.uet.auction.common.Request.AuctionRequest;
import com.uet.auction.common.Request.LoginRequest;
import com.uet.auction.common.Response.AuctionResponse;
import com.uet.auction.server.DAO.UserDAO;

import java.util.List;

public class AuthService {

    private UserDAO userDAO = new UserDAO();

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

    public AuctionResponse register(String fullname,String gmail,String username, String password, String role) {
        // Chỉ cho phép đăng ký tài khoản USER; ADMIN/SELLER không được tạo qua app
        boolean success = userDAO.registerUser(fullname,gmail,username, password, "USER");
        if (success) {
            return new AuctionResponse(true, "REGISTER_RESULT", "Đăng ký thành công!", null);
        } else {
            return new AuctionResponse(false, "REGISTER_RESULT", "Tên đăng nhập đã tồn tại hoặc lỗi CSDL!", null);
        }
    }

    public AuctionResponse upgradeToSeller(String username) {
        try {
            String currentRole = userDAO.getRole(username);
            if (currentRole == null) {
                return new AuctionResponse(false, "UPGRADE_TO_SELLER_RESULT", "Không tìm thấy tài khoản!", null);
            }
            if ("SELLER".equals(currentRole)) {
                return new AuctionResponse(false, "UPGRADE_TO_SELLER_RESULT", "Bạn đã là Người bán!", null);
            }
            if ("ADMIN".equals(currentRole)) {
                return new AuctionResponse(false, "UPGRADE_TO_SELLER_RESULT", "Tài khoản quản trị không thể đổi vai trò tại đây!", null);
            }
            if (!"USER".equals(currentRole) && !"BIDDER".equals(currentRole)) {
                return new AuctionResponse(false, "UPGRADE_TO_SELLER_RESULT", "Không thể nâng cấp vai trò này!", null);
            }

            boolean ok = userDAO.updateRole(username, "SELLER");
            if (ok) {
                return new AuctionResponse(true, "UPGRADE_TO_SELLER_RESULT",
                        "Đã đăng ký Người bán! Đăng nhập lại hoặc vào giao diện Người bán.", null);
            }
            return new AuctionResponse(false, "UPGRADE_TO_SELLER_RESULT", "Cập nhật vai trò thất bại!", null);
        } catch (Exception e) {
            return new AuctionResponse(false, "UPGRADE_TO_SELLER_RESULT", "Lỗi: " + e.getMessage(), null);
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
}