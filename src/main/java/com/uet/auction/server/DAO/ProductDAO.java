package com.uet.auction.server.DAO;

import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.server.config.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductDAO {

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
                list.add(mapRowToDTO(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ProductDAO.getProductsByStatus] " + e.getMessage());
        }
        return list;
    }

    public List<ProductDTO> getProductsBySeller(String sellerName) {
        List<ProductDTO> list = new ArrayList<>();
        String sql = "SELECT p.*, COUNT(b.id) AS bid_count " +
                "FROM products p " +
                "LEFT JOIN bids b ON b.product_id = p.id " +
                "WHERE p.seller_name = ? " +
                "GROUP BY p.id ORDER BY p.id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, sellerName);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                list.add(mapRowToDTO(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ProductDAO.getProductsBySeller] " + e.getMessage());
        }
        return list;
    }

    public List<ProductDTO> getJoinedProducts(String username) {
        List<ProductDTO> list = new ArrayList<>();
        String sql = "SELECT p.* FROM products p "
                + "WHERE p.id IN (SELECT DISTINCT b.product_id FROM bids b WHERE b.bidder_name = ?) "
                + "ORDER BY p.id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapRowToDTO(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ProductDAO.getJoinedProducts] " + e.getMessage());
        }
        return list;
    }

    public boolean addProduct(String name, String description, double startingPrice,
                              double stepPrice, String sellerName,
                              LocalDateTime startTime, LocalDateTime endTime,
                              String imageUrl) {

        String sql = "INSERT INTO products (name, description, starting_price, current_price, " +
                "step_price, start_time, end_time, seller_name, status, image_url) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            pstmt.setString(2, description);
            pstmt.setDouble(3, startingPrice);
            pstmt.setDouble(4, startingPrice); // current_price = starting_price lúc mới tạo

            pstmt.setDouble(5, stepPrice);

            if (startTime != null) {
                pstmt.setTimestamp(6, Timestamp.valueOf(startTime));
            } else {
                pstmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            }

            if (endTime != null) {
                pstmt.setTimestamp(7, Timestamp.valueOf(endTime));
            } else {
                pstmt.setTimestamp(7, null);
            }

            pstmt.setString(8, sellerName);
            pstmt.setString(9, imageUrl);

            boolean ok = pstmt.executeUpdate() > 0;
            if (ok) {
                System.out.println("[ProductDAO] Đã thêm sản phẩm thành công: " + name
                        + " | Giá: " + startingPrice + " | Bước giá: " + stepPrice
                        + " | Seller: " + sellerName);
            }
            return ok;

        } catch (SQLException e) {
            System.err.println("[ProductDAO.addProduct] Lỗi SQL: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Cập nhật trạng thái sản phẩm.
     * Đã xóa updateStatus() trùng lặp — chỉ giữ phương thức này.
     */
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

    /** Lấy tên người bán của sản phẩm dựa trên ID */
    public String getSellerOfProduct(int productId) {
        String sql = "SELECT seller_name FROM products WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("seller_name");
                }
            }
        } catch (SQLException e) {
            System.err.println("[ProductDAO.getSellerOfProduct] " + e.getMessage());
        }
        return null;
    }

    /** Tự động mở phiên APPROVED đến giờ start_time */
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

    /**
     * Tự động đóng các phiên OPEN đã hết giờ end_time.
     * Trả về danh sách phiên vừa đóng để AuctionTimer broadcast thông báo người thắng.
     * Mỗi phần tử trong list là một Map gồm: productId, productName, winner, finalPrice, sellerName.
     */
    public List<Map<String, Object>> closeExpiredAuctions() {
        List<Map<String, Object>> closedList = new ArrayList<>();

        String selectSql = "SELECT id, name, current_price, seller_name, owner_name " +
                "FROM products WHERE status = 'OPEN' AND end_time <= NOW()";
        String closeSql  = "UPDATE products SET status = 'CLOSED' WHERE id = ?";
        String paySql    = "UPDATE users SET balance = balance + ? WHERE username = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                ResultSet rs = selectStmt.executeQuery();
                while (rs.next()) {
                    int    productId   = rs.getInt("id");
                    String productName = rs.getString("name");
                    double finalPrice  = rs.getDouble("current_price");
                    String sellerName  = rs.getString("seller_name");
                    String winnerName  = rs.getString("owner_name"); // null nếu không ai đặt

                    // Đóng phiên
                    try (PreparedStatement closeStmt = conn.prepareStatement(closeSql)) {
                        closeStmt.setInt(1, productId);
                        closeStmt.executeUpdate();
                    }

                    // Cộng tiền cho seller nếu có người thắng
                    if (winnerName != null && !winnerName.isBlank()) {
                        try (PreparedStatement payStmt = conn.prepareStatement(paySql)) {
                            payStmt.setDouble(1, finalPrice);
                            payStmt.setString(2, sellerName);
                            payStmt.executeUpdate();
                            System.out.println("[closeExpiredAuctions] Đã đóng phiên id=" + productId
                                    + " | Người thắng: " + winnerName
                                    + " | Giá: " + finalPrice
                                    + " | Đã cộng tiền cho seller: " + sellerName);
                        }
                    } else {
                        System.out.println("[closeExpiredAuctions] Đã đóng phiên id=" + productId
                                + " | Không có người đặt giá.");
                    }

                    // Lưu thông tin để trả về cho AuctionTimer broadcast
                    Map<String, Object> info = new HashMap<>();
                    info.put("productId",   productId);
                    info.put("productName", productName);
                    info.put("winner",      winnerName);   // null = không ai thắng
                    info.put("finalPrice",  finalPrice);
                    info.put("sellerName",  sellerName);
                    closedList.add(info);
                }
            }

            conn.commit();
        } catch (SQLException e) {
            System.err.println("[closeExpiredAuctions] " + e.getMessage());
            if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }

        return closedList;
    }

    /**
     * Anti-sniping: gia hạn thêm 5 phút cho phiên OPEN có bid trong 2 phút qua
     * và còn dưới 30 giây trước khi kết thúc.
     * FIX: Chỉ gia hạn phiên CÓ BID GẦN ĐÂY (không gia hạn tất cả phiên sắp hết giờ).
     */
    public void extendAuctionIfLastBid() {
        String sql = "UPDATE products SET end_time = DATE_ADD(end_time, INTERVAL 5 MINUTE) "
                + "WHERE status = 'OPEN' "
                + "AND TIMESTAMPDIFF(SECOND, NOW(), end_time) BETWEEN 0 AND 30 "
                + "AND id IN ("
                + "  SELECT DISTINCT product_id FROM bids "
                + "  WHERE bid_time >= DATE_SUB(NOW(), INTERVAL 2 MINUTE)"
                + ")";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("[Anti-Sniping] Đã gia hạn " + affectedRows + " phiên có bid gần đây.");
            }
        } catch (SQLException e) {
            System.err.println("Lỗi anti-sniping: " + e.getMessage());
        }
    }

    /** Helper dùng chung để map ResultSet -> ProductDTO (tránh code trùng lặp) */
    private ProductDTO mapRowToDTO(ResultSet rs) throws SQLException {
        ProductDTO p = new ProductDTO();
        p.setId(rs.getInt("id"));
        p.setName(rs.getString("name"));
        p.setDescription(safeGetString(rs, "description"));
        p.setStartingPrice(rs.getDouble("starting_price"));
        p.setStepPrice(rs.getDouble("step_price"));

        // current_price = giá đang đấu; nếu NULL thì fallback về starting_price
        double cp = rs.getDouble("current_price");
        p.setCurrentPrice(rs.wasNull() ? rs.getDouble("starting_price") : cp);

        p.setSellerName(safeGetString(rs, "seller_name"));
        p.setOwnerName(safeGetString(rs, "owner_name"));
        p.setStatus(rs.getString("status"));
        p.setImageUrl(safeGetString(rs, "image_url"));

        try {
            Timestamp start = rs.getTimestamp("start_time");
            if (start != null) p.setStartTime(start.toLocalDateTime());
        } catch (Exception ignored) {}

        try {
            Timestamp end = rs.getTimestamp("end_time");
            if (end != null) p.setEndTime(end.toLocalDateTime());
        } catch (Exception ignored) {}

        try {
            p.setBidCount(rs.getInt("bid_count"));
        } catch (SQLException ignored) {}

        return p;
    }

    private String safeGetString(ResultSet rs, String col) {
        try { return rs.getString(col); } catch (Exception e) { return null; }
    }
}