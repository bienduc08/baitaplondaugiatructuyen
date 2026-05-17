package com.uet.auction.server.DAO;

import com.uet.auction.common.DTO.UserDTO;
import com.uet.auction.server.config.DatabaseConnection;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
                    user.setBalance(rs.getDouble("balance"));
                } catch (SQLException e) {
                    user.setBalance(0.0);
                }
                return user;
            }
        } catch (SQLException e) {
            System.err.println("Lỗi kiểm tra đăng nhập: " + e.getMessage());
        }
        return null;
    }

    // THÊM: lấy số dư của user theo username
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

    // THÊM: lấy role của user theo username
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

    public boolean registerUser(String username, String password, String role) {
        String checkSql  = "SELECT id FROM users WHERE username = ?";
        String insertSql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkStmt  = conn.prepareStatement(checkSql);
             PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

            checkStmt.setString(1, username);
            if (checkStmt.executeQuery().next()) return false;

            insertStmt.setString(1, username);
            insertStmt.setString(2, hashPassword(password));
            insertStmt.setString(3, role);
            return insertStmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Lỗi đăng ký tài khoản: " + e.getMessage());
            return false;
        }
    }
}