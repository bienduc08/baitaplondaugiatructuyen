package com.uet.auction.server.DAO;

import com.uet.auction.common.DTO.UserDTO;
import com.uet.auction.server.config.DatabaseConnection;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashed = md.digest(password.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi hash mật khẩu", e);
        }
    }

    public UserDTO checkLogin(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, hashPassword(password));
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                UserDTO user = new UserDTO();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setRole(rs.getString("role"));
                try {
                    user.setFullName(rs.getString("full_name"));
                } catch (SQLException e) {
                    user.setFullName("");
                }
                try {
                    user.setGmail(rs.getString("email"));
                } catch (SQLException e) {
                    user.setGmail("");
                }
                try {
                    user.setBalance(rs.getDouble("balance"));
                } catch (SQLException e) {
                    user.setBalance(0.0);
                }
                try {
                    user.setStatus(rs.getString("status"));
                } catch (SQLException e) {
                    user.setStatus("ACTIVE"); // mặc định nếu cột chưa có
                }


                return user;
            }
        } catch (SQLException e) {
            System.err.println("Lỗi kiểm tra đăng nhập: " + e.getMessage());
        }
        return null;
    }

    public double getBalance(String username) {
        String sql = "SELECT balance FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getDouble("balance");
        } catch (SQLException e) {
            System.err.println("Lỗi lấy balance: " + e.getMessage());
        }
        return 0.0;
    }

    public String getStatus(String username) {
        String sql = "SELECT status FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                try {
                    return rs.getString("status");
                } catch (SQLException e) {
                    return "ACTIVE";
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi lấy status: " + e.getMessage());
        }
        return null;
    }

    /** Cộng tiền vào số dư; trả về số dư mới hoặc null nếu thất bại. */
    public Double deposit(String username, double amount) {
        String sql = "UPDATE users SET balance = balance + ? WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, amount);
            pstmt.setString(2, username);
            if (pstmt.executeUpdate() > 0) {
                return getBalance(username);
            }
        } catch (SQLException e) {
            System.err.println("Lỗi nạp tiền: " + e.getMessage());
        }
        return null;
    }

    public String getRole(String username) {
        String sql = "SELECT role FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getString("role");
        } catch (SQLException e) {
            System.err.println("Lỗi lấy role: " + e.getMessage());
        }
        return null;
    }

    public boolean registerUser(String fullname,String gmail,String username, String password, String role) {
        String checkSql  = "SELECT id FROM users WHERE username = ?";
        // Mặc định đăng ký xong thì status là ACTIVE
        String insertSql = "INSERT INTO users ( fullname, gmail,username, password, role, status) VALUES (?, ? ,?, ?, ?, 'ACTIVE')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkStmt  = conn.prepareStatement(checkSql);
             PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

            checkStmt.setString(1, username);
            if (checkStmt.executeQuery().next()) return false;

            insertStmt.setString(1, fullname);
            insertStmt.setString(2, gmail);
            insertStmt.setString(3, username);
            insertStmt.setString(4, hashPassword(password));
            insertStmt.setString(5, role);

            return insertStmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Lỗi đăng ký tài khoản: " + e.getMessage());
            return fallbackRegisterUser(fullname, gmail, username, password, role);
        }
    }

    public boolean updateRole(String username, String newRole) {
        String sql = "UPDATE users SET role = ? WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newRole);
            pstmt.setString(2, username);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi cập nhật role: " + e.getMessage());
            return false;
        }
    }

    // Hàm dự phòng khi bảng chưa có cột status
    private boolean fallbackRegisterUser(String fullname, String gmail,  String username, String password, String role) {
        String insertSql = "INSERT INTO users (fullname,gmail,username, password, role) VALUES (? , ? , ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            insertStmt.setString(1, fullname);
            insertStmt.setString(2, gmail);
            insertStmt.setString(3, username);
            insertStmt.setString(4, hashPassword(password));
            insertStmt.setString(5, role);
            return insertStmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi đăng ký (fallback): " + e.getMessage());
            return false;
        }
    }

    // =========================================================
    // 3 HÀM QUẢN LÝ NGƯỜI DÙNG CHO ADMIN
    // =========================================================

    public List<UserDTO> getAllUsers() {
        List<UserDTO> users = new ArrayList<>();
        String sql = "SELECT id,fullname,gmail, username, role, balance, status FROM users";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                UserDTO user = new UserDTO();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setRole(rs.getString("role"));
                user.setBalance(rs.getDouble("balance"));

                try { user.setStatus(rs.getString("status")); } catch (SQLException e) { user.setStatus("ACTIVE"); }
                try { user.setFullName(rs.getString("fullname")); } catch (SQLException e) { user.setFullName(""); }
                try { user.setGmail(rs.getString("gmail")); } catch (SQLException e) { user.setGmail(""); }

                users.add(user);
            }
        } catch (SQLException e) {
            System.err.println("Lỗi lấy danh sách người dùng: " + e.getMessage());
            return null;
        }
        return users;
    }

    public List<UserDTO> searchUser(String keyword) {
        List<UserDTO> users = new ArrayList<>();
        String sql = "SELECT id,fullname,gmail, username, role, balance, status FROM users WHERE username LIKE ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + keyword + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    UserDTO user = new UserDTO();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setRole(rs.getString("role"));
                    user.setBalance(rs.getDouble("balance"));

                    try { user.setStatus(rs.getString("status")); } catch (SQLException e) { user.setStatus("ACTIVE"); }
                    try { user.setFullName(rs.getString("fullname")); } catch (SQLException e) { user.setFullName(""); }
                    try { user.setGmail(rs.getString("gmail")); } catch (SQLException e) { user.setGmail(""); }

                    users.add(user);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi tìm kiếm người dùng: " + e.getMessage());
            return null;
        }
        return users;
    }

    public boolean changeUserStatus(int userId, String newStatus) {
        String sql = "UPDATE users SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newStatus);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Lỗi cập nhật trạng thái: " + e.getMessage());
            return false;
        }
    }
}