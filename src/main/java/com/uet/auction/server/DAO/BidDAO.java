package com.uet.auction.server.DAO;

import com.uet.auction.common.DTO.BidDTO;
import com.uet.auction.server.config.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BidDAO {

    public synchronized boolean placeBid(int productId, String username, double bidAmount) {
        String checkSql  = "SELECT current_price, owner_name FROM products WHERE id = ? AND status = 'OPEN'";
        String updateSql = "UPDATE products SET current_price = ?, owner_name = ? WHERE id = ?";
        String insertSql = "INSERT INTO bids (product_id, bidder_name, amount, bid_time, status) "
                + "VALUES (?, ?, ?, NOW(), 'Hợp lệ')";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                pstmt.setInt(1, productId);
                ResultSet rs = pstmt.executeQuery();

                if (!rs.next()) {
                    System.err.println("[BidDAO] Sản phẩm id=" + productId + " không tồn tại hoặc không OPEN");
                    conn.rollback();
                    return false;
                }

                double currentPrice = rs.getDouble("current_price");
                if (rs.wasNull()) currentPrice = 0;

                String currentOwner = rs.getString("owner_name");

                // KIỂM TRA: đang giữ đỉnh thì không được đặt tiếp
                if (username.equals(currentOwner)) {
                    System.err.println("[BidDAO] " + username + " đang giữ đỉnh, không thể đặt thêm");
                    conn.rollback();
                    return false;
                }

                // KIỂM TRA: giá đặt phải lớn hơn giá hiện tại
                if (bidAmount <= currentPrice) {
                    System.err.println("[BidDAO] Giá đặt " + bidAmount + " <= giá hiện tại " + currentPrice);
                    conn.rollback();
                    return false;
                }

                System.out.println("[BidDAO] currentPrice=" + currentPrice
                        + " | owner=" + currentOwner + " | bidAmount=" + bidAmount);
            }

            // Cập nhật giá mới
            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setDouble(1, bidAmount);
                pstmt.setString(2, username);
                pstmt.setInt(3, productId);
                pstmt.executeUpdate();
            }

            // Ghi lịch sử
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
                list.add(new BidDTO(
                        rs.getInt("id"),
                        rs.getInt("product_id"),
                        rs.getString("bidder_name"),
                        rs.getDouble("amount"),
                        rs.getString("bid_time_str"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<BidDTO> getBidsByUsername(String username) {
        List<BidDTO> list = new ArrayList<>();
        String sql = "SELECT id, product_id, bidder_name, amount, "
                + "DATE_FORMAT(bid_time, '%d-%m-%Y %H:%i:%s') AS bid_time_str, status "
                + "FROM bids WHERE bidder_name = ? ORDER BY bid_time DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(new BidDTO(
                        rs.getInt("id"),
                        rs.getInt("product_id"),
                        rs.getString("bidder_name"),
                        rs.getDouble("amount"),
                        rs.getString("bid_time_str"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

}