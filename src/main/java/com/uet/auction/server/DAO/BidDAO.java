package com.uet.auction.server.DAO;

import com.uet.auction.server.config.DatabaseConnection;
import java.sql.*;

public class BidDAO {
    // Xử lý một lượt đặt giá mới
    public synchronized boolean placeBid(int productId, String username, double bidAmount) {
        String checkSql = "SELECT starting_price FROM products WHERE id = ?";
        String updateSql = "UPDATE products SET starting_price = ?, owner_name = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Dùng Transaction để đảm bảo tính đồng bộ khi nhiều người cùng đặt giá
            conn.setAutoCommit(false);

            // 1. Kiểm tra giá hiện tại
            try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                pstmt.setInt(1, productId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    double currentPrice = rs.getDouble("starting_price");
                    if (bidAmount <= currentPrice) {
                        return false; // Giá đặt thấp hơn giá hiện tại
                    }
                }
            }

            // 2. Cập nhật giá mới và người giữ giá cao nhất
            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setDouble(1, bidAmount);
                pstmt.setString(2, username);
                pstmt.setInt(3, productId);
                pstmt.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}