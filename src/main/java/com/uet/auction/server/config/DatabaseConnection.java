package com.uet.auction.server.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    // Không dùng biến static Connection ở đây nữa

    private static final String URL = "jdbc:mysql://localhost:3306/auction_db?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";

    // Nạp Driver 1 lần duy nhất vào bộ nhớ khi class khởi tạo
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Không tìm thấy MySQL Driver: " + e.getMessage());
        }
    }

    private DatabaseConnection() {
        // Private constructor
    }

    // Trả về một kết nối MỚI HOÀN TOÀN mỗi khi được gọi
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }
}