package com.uet.auction.server.DAO;

import com.uet.auction.common.DTO.BidDTO;
import com.uet.auction.server.config.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BidDAO {

    // Lock dùng chung cho toàn bộ class (static) để synchronized thực sự có tác dụng
    // khi nhiều ClientHandler cùng gọi placeBid đồng thời
    private static final Object BID_LOCK = new Object();

    public boolean placeBid(int productId, String username, double bidAmount) {
        synchronized (BID_LOCK) {
            String checkSql  = "SELECT current_price, step_price, owner_name, seller_name FROM products WHERE id = ? AND status = 'OPEN'";
            String updateSql = "UPDATE products SET current_price = ?, owner_name = ? WHERE id = ?";
            String insertSql = "INSERT INTO bids (product_id, bidder_name, amount, bid_time, status) "
                    + "VALUES (?, ?, ?, NOW(), 'Hợp lệ')";
            // Trừ tiền người đặt giá mới (và hoàn tiền người giữ đỉnh cũ nếu có)
            String deductSql = "UPDATE users SET balance = balance - ? WHERE username = ? AND balance >= ?";
            String refundSql = "UPDATE users SET balance = balance + ? WHERE username = ?";

            Connection conn = null;
            try {
                conn = DatabaseConnection.getConnection();
                conn.setAutoCommit(false);

                double currentPrice;
                double stepPrice;
                String currentOwner;
                String sellerName;

                try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                    pstmt.setInt(1, productId);
                    ResultSet rs = pstmt.executeQuery();

                    if (!rs.next()) {
                        System.err.println("[BidDAO] Sản phẩm id=" + productId + " không tồn tại hoặc không OPEN");
                        conn.rollback();
                        return false;
                    }

                    currentPrice = rs.getDouble("current_price");
                    if (rs.wasNull()) currentPrice = 0;
                    stepPrice    = rs.getDouble("step_price");
                    currentOwner = rs.getString("owner_name");
                    sellerName   = rs.getString("seller_name");
                }

                // Chặn người bán tự đặt giá sản phẩm của mình
                if (sellerName != null && sellerName.equals(username)) {
                    System.err.println("[BidDAO] " + username + " không thể đấu giá sản phẩm của chính mình");
                    conn.rollback();
                    return false;
                }

                // Chặn người đang giữ đỉnh đặt tiếp
                if (username.equals(currentOwner)) {
                    System.err.println("[BidDAO] " + username + " đang giữ đỉnh, không thể đặt thêm");
                    conn.rollback();
                    return false;
                }

                // Kiểm tra: giá đặt phải >= giá hiện tại + bước giá (thống nhất với client)
                double minRequired = currentPrice + stepPrice;
                if (bidAmount < minRequired) {
                    System.err.println("[BidDAO] Giá đặt " + bidAmount + " < giá tối thiểu " + minRequired
                            + " (giá hiện tại " + currentPrice + " + bước giá " + stepPrice + ")");
                    conn.rollback();
                    return false;
                }

                System.out.println("[BidDAO] currentPrice=" + currentPrice + " stepPrice=" + stepPrice
                        + " minRequired=" + minRequired + " | owner=" + currentOwner + " | bidAmount=" + bidAmount);

                // Hoàn tiền cho người đang giữ đỉnh (nếu có)
                if (currentOwner != null && !currentOwner.isBlank()) {
                    try (PreparedStatement refundStmt = conn.prepareStatement(refundSql)) {
                        refundStmt.setDouble(1, currentPrice);
                        refundStmt.setString(2, currentOwner);
                        refundStmt.executeUpdate();
                        System.out.println("[BidDAO] Hoàn " + currentPrice + " VNĐ cho " + currentOwner);
                    }
                }

                // Trừ tiền người đặt giá mới
                try (PreparedStatement deductStmt = conn.prepareStatement(deductSql)) {
                    deductStmt.setDouble(1, bidAmount);
                    deductStmt.setString(2, username);
                    deductStmt.setDouble(3, bidAmount);
                    int affected = deductStmt.executeUpdate();
                    if (affected == 0) {
                        System.err.println("[BidDAO] Không trừ được tiền của " + username);
                        conn.rollback();
                        return false;
                    }
                }

                // Cập nhật giá mới và người giữ đỉnh
                try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                    pstmt.setDouble(1, bidAmount);
                    pstmt.setString(2, username);
                    pstmt.setInt(3, productId);
                    pstmt.executeUpdate();
                }

                // Ghi lịch sử đấu giá
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
                if (conn != null) try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    public List<BidDTO> getBidsByProductId(int productId) {
        List<BidDTO> list = new ArrayList<>();
        String sql = "SELECT id, product_id, bidder_name, amount, "
                + "DATE_FORMAT(bid_time, '%d-%m-%Y %H:%i:%s') AS bid_time_str, status "
                + "FROM bids WHERE product_id = ? ORDER BY bid_time DESC, id DESC";
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
                + "FROM bids WHERE bidder_name = ? ORDER BY bid_time DESC, id DESC";
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