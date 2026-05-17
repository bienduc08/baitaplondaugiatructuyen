package com.uet.auction.server.DAO;

import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.server.config.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    public List<ProductDTO> getAllProducts() {
        return getProductsByStatus("ALL");
    }

    public List<ProductDTO> getProductsByStatus(String status) {
        List<ProductDTO> list = new ArrayList<>();
        String sql = "ALL".equals(status)
                ? "SELECT * FROM products ORDER BY id DESC"
                : "SELECT * FROM products WHERE status = ? ORDER BY id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (!"ALL".equals(status)) pstmt.setString(1, status);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                ProductDTO p = new ProductDTO();
                p.setId(rs.getInt("id"));
                p.setName(rs.getString("name"));  // cột đúng là "name"
                p.setStartingPrice(rs.getDouble("starting_price"));

                // current_price = giá đang đấu; nếu NULL thì fallback về starting_price
                double cp = rs.getDouble("current_price");
                p.setCurrentPrice(rs.wasNull() ? rs.getDouble("starting_price") : cp);
                p.setDescription(safeGetString(rs, "description"));
                p.setSellerName(safeGetString(rs, "seller_name"));
                p.setOwnerName(safeGetString(rs, "owner_name"));
                p.setStatus(rs.getString("status"));

                try { if (rs.getTimestamp("start_time") != null) p.setStartTime(rs.getTimestamp("start_time").toLocalDateTime()); } catch (Exception ignored) {}
                try { if (rs.getTimestamp("end_time")   != null) p.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());   } catch (Exception ignored) {}
                list.add(p);
            }
        } catch (SQLException e) {
            System.err.println("[ProductDAO.getProductsByStatus] " + e.getMessage());
        }
        return list;
    }

    // =========================================================
    // HÀM MỚI BỔ SUNG: Lấy danh sách sản phẩm theo người bán
    // =========================================================
    public List<ProductDTO> getProductsBySeller(String sellerName) {
        List<ProductDTO> list = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE seller_name = ? ORDER BY id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, sellerName);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                ProductDTO p = new ProductDTO();
                p.setId(rs.getInt("id"));
                p.setName(rs.getString("name"));
                p.setStartingPrice(rs.getDouble("starting_price"));

                double cp = rs.getDouble("current_price");
                p.setCurrentPrice(rs.wasNull() ? rs.getDouble("starting_price") : cp);
                p.setDescription(safeGetString(rs, "description"));
                p.setSellerName(safeGetString(rs, "seller_name"));
                p.setOwnerName(safeGetString(rs, "owner_name"));
                p.setStatus(rs.getString("status"));

                try { if (rs.getTimestamp("start_time") != null) p.setStartTime(rs.getTimestamp("start_time").toLocalDateTime()); } catch (Exception ignored) {}
                try { if (rs.getTimestamp("end_time")   != null) p.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());   } catch (Exception ignored) {}

                list.add(p);
            }
        } catch (SQLException e) {
            System.err.println("[ProductDAO.getProductsBySeller] " + e.getMessage());
        }
        return list;
    }

    public boolean addProduct(String name, double startingPrice, String sellerName,
                              LocalDateTime startTime, LocalDateTime endTime, String description) {

        String sql = "INSERT INTO products " +
                "(name, description, starting_price, current_price, " +
                " seller_name, start_time, end_time, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            pstmt.setString(2, description != null ? description : "");
            pstmt.setDouble(3, startingPrice);
            pstmt.setDouble(4, startingPrice); // current_price = starting_price ban đầu
            pstmt.setString(5, sellerName);
            pstmt.setTimestamp(6, Timestamp.valueOf(startTime));
            pstmt.setTimestamp(7, Timestamp.valueOf(endTime));

            boolean ok = pstmt.executeUpdate() > 0;
            if (ok) System.out.println("[ProductDAO] Đã thêm sản phẩm: " + name);
            return ok;

        } catch (SQLException e) {
            System.err.println("[ProductDAO.addProduct] " + e.getMessage());
            return false;
        }
    }

    public boolean updateProductStatus(int productId, String newStatus) {
        String sql = "UPDATE products SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, productId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ProductDAO.updateProductStatus] " + e.getMessage());
            return false;
        }
    }

    public void openScheduledAuctions() {
        String sql = "UPDATE products SET status = 'OPEN' WHERE status = 'APPROVED' AND start_time <= NOW()";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int n = pstmt.executeUpdate();
            if (n > 0) System.out.println(">>> Đã mở tự động " + n + " phiên.");
        } catch (SQLException e) {
            System.err.println("[openScheduledAuctions] " + e.getMessage());
        }
    }

    public void closeExpiredAuctions() {
        String sql = "UPDATE products SET status = 'CLOSED' WHERE status = 'OPEN' AND end_time <= NOW()";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int n = pstmt.executeUpdate();
            if (n > 0) System.out.println(">>> Đã đóng tự động " + n + " phiên.");
        } catch (SQLException e) {
            System.err.println("[closeExpiredAuctions] " + e.getMessage());
        }
    }

    private String safeGetString(ResultSet rs, String col) {
        try { return rs.getString(col); } catch (Exception e) { return null; }
    }
    public void extendAuctionIfLastBid() {
        // Ví dụ một câu lệnh SQL để tự động gia hạn thêm 5 phút cho các sản phẩm
        // đang ở trạng thái OPEN, có người vừa bid và thời gian còn lại dưới 30 giây.
        String sql = "UPDATE products SET end_time = DATE_ADD(end_time, INTERVAL 5 MINUTE) " +
                "WHERE status = 'OPEN' AND TIMESTAMPDIFF(SECOND, NOW(), end_time) BETWEEN 0 AND 30";

        try (Connection conn = DatabaseConnection.getConnection(); // Sử dụng class kết nối DB của bạn
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("[Anti-Sniping] Đã kích hoạt gia hạn cho " + affectedRows + " sản phẩm.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Lỗi khi thực hiện anti-sniping: " + e.getMessage());
        }
    }
}