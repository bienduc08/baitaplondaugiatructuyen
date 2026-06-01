package com.uet.auction.server.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    // 1. Biến static lưu trữ instance duy nhất
    private static Connection connection = null;

    private static final String URL = "jdbc:mysql://localhost:3306/auction_db";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";

    // 2. Constructor Private để chặn việc dùng từ khóa 'new' từ bên ngoài
    private DatabaseConnection() {
    }

    // 3. Hàm lấy kết nối Singleton
    public static Connection getConnection() throws SQLException {
        try {
            // Chỉ tạo kết nối mới nếu nó chưa từng được khởi tạo, hoặc kết nối cũ đã bị đóng
            if (connection == null || connection.isClosed()) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            }
        } catch (ClassNotFoundException e) {
            throw new SQLException("Không tìm thấy MySQL Driver!", e);
        }

        // Trả về instance duy nhất
        return connection;
    }
}