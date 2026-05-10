package com.uet.auction.server.DAO;

import com.uet.auction.common.DTO.BidDTO;
import com.uet.auction.server.config.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BidDAO {

    public synchronized boolean placeBid(int productId, String username, double bidAmount) {

        // FIX 1: đọc current_price (giá đang đấu), KHÔNG đọc starting_price (giá khởi điểm gốc)
        String checkSql  = "SELECT current_price FROM products WHERE id = ? AND status = 'OPEN'";

        // FIX 2: chỉ cập nhật current_price + owner_name, KHÔNG đụng starting_price
        String updateSql = "UPDATE products SET current_price = ?, owner_name = ? WHERE id = ?";

        String insertSql = "INSERT INTO bids (product_id, bidder_name, amount, bid_time, status) "
                + "VALUES (?, ?, ?, NOW(), 'Hợp lệ')";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // Bước 1: Kiểm tra sản phẩm OPEN và giá hợp lệ
            try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                pstmt.setInt(1, productId);
                ResultSet rs = pstmt.executeQuery();

                if (!rs.next()) {
                    // Kiểm tra xem sản phẩm có tồn tại không (không có điều kiện status)
                    try (PreparedStatement ps2 = conn.prepareStatement(
                            "SELECT id, status, current_price FROM products WHERE id = ?")) {
                        ps2.setInt(1, productId);
                        ResultSet rs2 = ps2.executeQuery();
                        if (rs2.next()) {
                            System.err.println("[BidDAO] Sản phẩm id=" + productId
                                    + " tồn tại nhưng status='" + rs2.getString("status")
                                    + "', current_price=" + rs2.getDouble("current_price"));
                        } else {
                            System.err.println("[BidDAO] Sản phẩm id=" + productId + " KHÔNG TỒN TẠI trong DB!");
                        }
                    }
                    conn.rollback();
                    return false;
                }

                double currentPrice = rs.getDouble("current_price");
                if (rs.wasNull()) currentPrice = 0;
                System.out.println("[BidDAO] currentPrice=" + currentPrice + " | bidAmount=" + bidAmount);
                System.out.println("[BidDAO] So sánh: " + bidAmount + " <= " + currentPrice + " = " + (bidAmount <= currentPrice));

                if (bidAmount <= currentPrice) {
                    System.err.println("[BidDAO] Giá đặt " + bidAmount + " <= giá hiện tại " + currentPrice);
                    conn.rollback();
                    return false;
                }
            }

            // Bước 2: Chỉ cập nhật current_price và owner_name
            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setDouble(1, bidAmount);   // current_price mới
                pstmt.setString(2, username);    // owner_name
                pstmt.setInt(3, productId);      // WHERE id
                pstmt.executeUpdate();
            }

            // Bước 3: Ghi lịch sử vào bảng bids
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setInt(1, productId);
                pstmt.setString(2, username);
                pstmt.setDouble(3, bidAmount);
                pstmt.executeUpdate();
            }

            conn.commit();
            System.out.println("[BidDAO] Đặt giá thành công: " + username + " - " + bidAmount);
            return true;

        } catch (SQLException e) {
            System.err.println("[BidDAO] SQLException: " + e.getMessage());
            e.printStackTrace();
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            return false;
        } finally {
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    public List<BidDTO> getBidsByProductId(int productId) {
        List<BidDTO> list = new ArrayList<>();
        String sql = "SELECT id, product_id, bidder_name, amount, "
                + "DATE_FORMAT(bid_time, '%d-%m-%Y %H:%i:%s') AS bid_time_str, status "
                + "FROM bids WHERE product_id = ? ORDER BY bid_time DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, productId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                BidDTO dto = new BidDTO(
                        rs.getInt("id"),
                        rs.getInt("product_id"),
                        rs.getString("bidder_name"),
                        rs.getDouble("amount"),
                        rs.getString("bid_time_str"),
                        rs.getString("status")
                );
                list.add(dto);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}