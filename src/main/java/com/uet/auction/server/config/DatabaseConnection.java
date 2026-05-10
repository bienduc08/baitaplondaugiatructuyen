package com.uet.auction.server.config; // Hoặc package phù hợp với dự án của bạn

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // Thay 'auction_db' bằng tên database bạn tạo trong MySQL/XAMPP
    private static final String URL = "jdbc:mysql://localhost:3306/auction_db";
    private static final String USERNAME = "root";
    private static final String PASSWORD = ""; // Mặc định của XAMPP thường để trống

    public static Connection getConnection() throws SQLException {
        try {
            // Đảm bảo Driver đã được tải
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USERNAME, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Không tìm thấy MySQL Driver!", e);
        }
    }
}