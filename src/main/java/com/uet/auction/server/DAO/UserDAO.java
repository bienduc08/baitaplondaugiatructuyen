package com.uet.auction.server.DAO;

import com.uet.auction.server.config.DatabaseConnection;
import java.sql.*;

public class UserDAO {
    public boolean checkLogin(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();

            return rs.next(); // Trả về true nếu tìm thấy tài khoản
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    // Thêm vào class UserDAO
    public boolean registerUser(String username, String password) {
        String sql = "INSERT INTO users (username, password, balance) VALUES (?, ?, 5000000.0)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password); // Thực tế nên mã hóa băm mật khẩu (Hash)
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false; // Trùng tên đăng nhập hoặc lỗi DB
        }
    }
}