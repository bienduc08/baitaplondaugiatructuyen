package com.uet.auction.server.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Cung cấp kết nối JDBC cho từng thread độc lập.
 * Không dùng Singleton vì 50 thread dùng chung 1 Connection gây lỗi thread-safety.
 * Mỗi lời gọi getConnection() trả về một Connection riêng — caller phải tự đóng trong finally/try-with-resources.
 */
public class DatabaseConnection {

    private static final String URL      = "jdbc:mysql://localhost:3306/auction_db?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";

    private DatabaseConnection() {}

    /**
     * Tạo và trả về một Connection mới cho thread hiện tại.
     * Luôn đóng connection sau khi dùng xong bằng try-with-resources hoặc finally.
     */
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USERNAME, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Không tìm thấy MySQL Driver!", e);
        }
    }
}