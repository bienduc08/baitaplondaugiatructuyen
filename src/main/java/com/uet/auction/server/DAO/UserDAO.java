package com.uet.auction.server.DAO;

import com.uet.auction.common.DTO.UserDTO;
import com.uet.auction.server.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

    // 1. Kiểm tra đăng nhập
    public UserDTO checkLogin(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                UserDTO user = new UserDTO();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setRole(rs.getString("role")); // Lấy Role (ADMIN, SELLER, USER)
                return user;
            }
        } catch (SQLException e) {
            System.err.println("Lỗi kiểm tra đăng nhập: " + e.getMessage());
        }
        return null;
    }

    // 2. Đăng ký tài khoản mới
    public boolean registerUser(String username, String password, String role) {
        // Kiểm tra xem user đã tồn tại chưa
        String checkSql = "SELECT id FROM users WHERE username = ?";
        String insertSql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql);
             PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                return false; // User đã tồn tại
            }

            // Nếu chưa tồn tại thì thêm mới
            insertStmt.setString(1, username);
            insertStmt.setString(2, password);
            insertStmt.setString(3, role);

            return insertStmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Lỗi đăng ký tài khoản: " + e.getMessage());
            return false;
        }
    }
}