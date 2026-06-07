package com.uet.auction.server.DAO;

import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.server.config.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductDAO {

    // =========================================================================
    // CRUD CƠ BẢN & FILTER QUERIES
    // =========================================================================

    public ProductDTO getProductById(int productId) {
        String sql = "SELECT * FROM products WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return mapRowToDTO(rs);
        } catch (SQLException e) {
            System.err.println("[ProductDAO.getProductById] " + e.getMessage());
        }
        return null;
    }

    public List<ProductDTO> getAllProducts() { return getProductsByStatus("ALL"); }

    public List<ProductDTO> getProductsByStatus(String status) {
        List<ProductDTO> list = new ArrayList<>();
        String sql = "ALL".equals(status) ? "SELECT * FROM products ORDER BY id DESC"
                : "SELECT * FROM products WHERE status = ? ORDER BY id DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (!"ALL".equals(status)) pstmt.setString(1, status);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) list.add(mapRowToDTO(rs));
        } catch (SQLException e) {
            System.err.println("[ProductDAO.getProductsByStatus] " + e.getMessage());
        }
        return list;
    }

    public List<ProductDTO> getProductsBySeller(String sellerName) {
        List<ProductDTO> list = new ArrayList<>();
        String sql = "SELECT p.*, COUNT(b.id) AS bid_count FROM products p " +
                "LEFT JOIN bids b ON b.product_id = p.id WHERE p.seller_name = ? GROUP BY p.id ORDER BY p.id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sellerName);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) list.add(mapRowToDTO(rs));
        } catch (SQLException e) {
            System.err.println("[ProductDAO.getProductsBySeller] " + e.getMessage());
        }
        return list;
    }

    public List<ProductDTO> getJoinedProducts(String username) {
        List<ProductDTO> list = new ArrayList<>();
        String sql = "SELECT p.* FROM products p WHERE p.id IN (SELECT DISTINCT b.product_id FROM bids b WHERE b.bidder_name = ?) ORDER BY p.id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) list.add(mapRowToDTO(rs));
        } catch (SQLException e) {
            System.err.println("[ProductDAO.getJoinedProducts] " + e.getMessage());
        }
        return list;
    }

    public String getSellerOfProduct(int productId) {
        String sql = "SELECT seller_name FROM products WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getString("seller_name");
            }
        } catch (SQLException e) {
            System.err.println("[ProductDAO.getSellerOfProduct] " + e.getMessage());
        }
        return null;
    }

    public boolean addProduct(String name, String description, BigDecimal startingPrice, BigDecimal stepPrice, String sellerName, LocalDateTime startTime, LocalDateTime endTime, String imageUrl) {
        String sql = "INSERT INTO products (name, description, starting_price, current_price, step_price, start_time, end_time, seller_name, status, image_url) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', ?)";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, description);
            pstmt.setBigDecimal(3, startingPrice);
            pstmt.setBigDecimal(4, startingPrice);
            pstmt.setBigDecimal(5, stepPrice);
            pstmt.setTimestamp(6, Timestamp.valueOf(startTime != null ? startTime : LocalDateTime.now()));
            pstmt.setTimestamp(7, endTime != null ? Timestamp.valueOf(endTime) : null);
            pstmt.setString(8, sellerName);
            pstmt.setString(9, imageUrl);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean updateProductStatus(int productId, String newStatus) {
        String sql = "UPDATE products SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, productId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }
    public boolean updateProduct(ProductDTO p) {
        String sql = "UPDATE products SET name = ?, description = ?, starting_price = ?, current_price = ?, "
                + "step_price = ?, status = 'PENDING', owner_name = NULL WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, p.getName());
            pstmt.setString(2, p.getDescription());
            pstmt.setBigDecimal(3, p.getStartingPrice());
            pstmt.setBigDecimal(4, p.getStartingPrice()); // giá hiện tại reset về giá khởi điểm
            pstmt.setBigDecimal(5, p.getStepPrice());
            pstmt.setInt(6, p.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ProductDAO] Lỗi khi cập nhật sản phẩm: " + e.getMessage());
            return false;
        }
    }

    // =========================================================================
    // SCHEDULED OPERATIONS
    // =========================================================================

    public void openScheduledAuctions() {
        String sql = "UPDATE products SET status = 'OPEN' WHERE status = 'APPROVED' AND start_time <= NOW()";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.executeUpdate();
        } catch (SQLException ignored) {}
    }

    public List<Map<String, Object>> closeExpiredAuctions() {
        List<Map<String, Object>> closedList = new ArrayList<>();
        String selectSql = "SELECT id, name, current_price, seller_name, owner_name FROM products WHERE status = 'OPEN' AND end_time <= NOW()";
        String closeSql  = "UPDATE products SET status = 'CLOSED' WHERE id = ?";
        String paySql    = "UPDATE users SET balance = balance + ? WHERE username = ?";

        // Dùng try-with-resources để không rò rỉ RAM/Kết nối DB
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                ResultSet rs = selectStmt.executeQuery();
                while (rs.next()) {
                    int    productId   = rs.getInt("id");
                    String productName = rs.getString("name");
                    BigDecimal finalPrice  = rs.getBigDecimal("current_price");
                    String sellerName  = rs.getString("seller_name");
                    String winnerName  = rs.getString("owner_name");

                    try (PreparedStatement closeStmt = conn.prepareStatement(closeSql)) {
                        closeStmt.setInt(1, productId);
                        closeStmt.executeUpdate();
                    }

                    if (winnerName != null && !winnerName.isBlank()) {
                        try (PreparedStatement payStmt = conn.prepareStatement(paySql)) {
                            payStmt.setBigDecimal(1, finalPrice);
                            payStmt.setString(2, sellerName);
                            payStmt.executeUpdate();
                        }
                    }

                    Map<String, Object> info = new HashMap<>();
                    info.put("productId",   productId);
                    info.put("productName", productName);
                    info.put("winner",      winnerName);
                    info.put("finalPrice",  finalPrice);
                    info.put("sellerName",  sellerName);
                    closedList.add(info);
                }
            }
            conn.commit();
        } catch (SQLException e) {
            System.err.println("[closeExpiredAuctions] Lỗi: " + e.getMessage());
        }
        return closedList;
    }

    public void extendAuctionIfLastBid() {
        // Anti-sniping: gia han 5 phut neu co bid trong 30 giay cuoi.
        // Dung last_extended_at de dam bao moi lan bid chi trigger 1 lan gia han,
        // tranh timer chay moi giay gay gia han lien tuc.
        // Chi gia han neu: thoi gian con lai TAI THOI DIEM BID duoc dat < 30 giay
        // (KHONG dung NOW() de tranh timer chay muon van trigger)
        String sql = "UPDATE products p "
                + "JOIN ("
                + "  SELECT product_id, MAX(bid_time) AS latest_bid "
                + "  FROM bids "
                + "  GROUP BY product_id"
                + ") b ON b.product_id = p.id "
                + "SET p.end_time = DATE_ADD(p.end_time, INTERVAL 3 MINUTE), "
                + "    p.extension_count = COALESCE(p.extension_count, 0) + 1, "
                + "    p.last_extended_at = NOW(3) "
                + "WHERE p.status = 'OPEN' "
                + "AND COALESCE(p.extension_count, 0) < 3 "
                + "AND (p.last_extended_at IS NULL OR p.last_extended_at < b.latest_bid) "
                // Dieu kien duy nhat: luc bid duoc dat, con lai < 30 giay
                // end_time o day la end_time CHUA duoc gia han (vi latest_bid < last_extended_at da bi filter o tren)
                + "AND TIMESTAMPDIFF(SECOND, b.latest_bid, p.end_time) BETWEEN 0 AND 29";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException ignored) {}
    }

    // =========================================================================
    // HELPER
    // =========================================================================

    private ProductDTO mapRowToDTO(ResultSet rs) throws SQLException {
        ProductDTO p = new ProductDTO();
        p.setId(rs.getInt("id"));
        p.setName(rs.getString("name"));
        p.setDescription(safeGetString(rs, "description"));
        p.setStartingPrice(rs.getBigDecimal("starting_price"));
        p.setStepPrice(rs.getBigDecimal("step_price"));

        BigDecimal currentPrice = rs.getBigDecimal("current_price");
        p.setCurrentPrice(currentPrice == null ? rs.getBigDecimal("starting_price") : currentPrice);

        p.setSellerName(safeGetString(rs, "seller_name"));
        p.setOwnerName(safeGetString(rs, "owner_name"));
        p.setStatus(rs.getString("status"));
        p.setImageUrl(safeGetString(rs, "image_url"));

        try { Timestamp start = rs.getTimestamp("start_time"); if (start != null) p.setStartTime(start.toLocalDateTime()); } catch (Exception ignored) {}
        try { Timestamp end = rs.getTimestamp("end_time"); if (end != null) p.setEndTime(end.toLocalDateTime()); } catch (Exception ignored) {}
        try { p.setBidCount(rs.getInt("bid_count")); } catch (Exception ignored) {}
        return p;
    }

    private String safeGetString(ResultSet rs, String col) {
        try { return rs.getString(col); } catch (Exception e) { return null; }
    }
}