package com.uet.auction.server.DAO;

import com.uet.auction.server.config.DatabaseConnection;
import com.uet.auction.common.DTO.ProductDTO;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {
    // Lấy danh sách sản phẩm đang đấu giá
    public List<ProductDTO> getAllProducts() {
        List<ProductDTO> list = new ArrayList<>();
        String sql = "SELECT * FROM products";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                ProductDTO p = new ProductDTO();
                p.setId(rs.getInt("id"));
                p.setName(rs.getString("name"));
                p.setCurrentPrice(rs.getDouble("starting_price"));
                // Map thêm các trường khác như start_time, end_time nếu cần
                list.add(p);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    // Thêm hàm này vào com.uet.auction.server.DAO.ProductDAO
    public List<ProductDTO> getProductsByStatus(String status) {
        List<ProductDTO> list = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE status = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                ProductDTO p = new ProductDTO();
                p.setId(rs.getInt("id"));
                p.setName(rs.getString("name"));
                p.setCurrentPrice(rs.getDouble("starting_price"));
                p.setSellerName(rs.getString("seller_name"));
                p.setOwnerName(rs.getString("owner_name"));

                if (rs.getTimestamp("start_time") != null) {
                    p.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
                }
                if (rs.getTimestamp("end_time") != null) {
                    p.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
                }
                p.setStatus(rs.getString("status"));

                list.add(p);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Cập nhật giá mới khi có người đặt thầu
    public boolean updatePrice(int productId, double newPrice) {
        String sql = "UPDATE products SET starting_price = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, newPrice);
            pstmt.setInt(2, productId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    // Thêm vào trong class ProductDAO
    public void closeExpiredAuctions() {
        String sql = "UPDATE products SET status = 'CLOSED' WHERE end_time <= NOW() AND status = 'OPEN'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println(">>> Đã đóng " + affectedRows + " phiên đấu giá hết hạn.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    // Trong com.uet.auction.server.DAO.ProductDAO

    // 1. Dành cho Seller: Thêm sản phẩm với trạng thái PENDING
    public boolean addProduct(String name, double startingPrice, String sellerName, LocalDateTime startTime, LocalDateTime endTime) {
        String sql = "INSERT INTO products (name, starting_price, seller_name, start_time, end_time, status) VALUES (?, ?, ?, ?, ?, 'PENDING')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            pstmt.setDouble(2, startingPrice);
            pstmt.setString(3, sellerName);
            pstmt.setTimestamp(4, Timestamp.valueOf(startTime));
            pstmt.setTimestamp(5, Timestamp.valueOf(endTime));

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 2. Dành cho Admin: Cập nhật trạng thái sản phẩm (Duyệt hoặc Từ chối)
    public boolean updateProductStatus(int productId, String newStatus) {
        String sql = "UPDATE products SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, productId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }
    public void openScheduledAuctions() {
        // Chuyển trạng thái từ APPROVED sang OPEN khi đến giờ
        String sql = "UPDATE products SET status = 'OPEN' WHERE status = 'APPROVED' AND start_time <= NOW()";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println(">>> Đã mở tự động " + affectedRows + " phiên đấu giá đến giờ!");
            }

        } catch (SQLException e) {
            System.err.println("Lỗi khi mở phiên đấu giá tự động: " + e.getMessage());
        }
    }
}