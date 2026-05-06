package com.uet.auction.server.service;

import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.common.Response.AuctionResponse;
import com.uet.auction.server.DAO.ProductDAO;
import com.uet.auction.server.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;

public class AuctionService {

    private ProductDAO productDAO = new ProductDAO();

    public AuctionResponse getProductsByStatus(String status) {
        List<ProductDTO> list = productDAO.getProductsByStatus(status);
        return new AuctionResponse(true, "GET_PRODUCTS_RESULT", list);
    }

    public AuctionResponse changeProductStatus(int productId, String newStatus) {
        boolean success = productDAO.updateProductStatus(productId, newStatus);
        if (success) {
            return new AuctionResponse(true, "CHANGE_STATUS_RESULT", "Cập nhật thành công!");
        }
        return new AuctionResponse(false, "CHANGE_STATUS_RESULT", "Cập nhật thất bại!");
    }

    public AuctionResponse addProduct(Object[] data) {
        try {
            String name = (String) data[0];
            double price = (Double) data[1];
            String sellerName = (String) data[2];
            LocalDateTime startTime = (LocalDateTime) data[3];
            LocalDateTime endTime = (LocalDateTime) data[4];

            boolean success = productDAO.addProduct(name, price, sellerName, startTime, endTime);
            if (success) {
                return new AuctionResponse(true, "ADD_PRODUCT_RESULT", "Gửi yêu cầu đăng bán thành công, chờ Admin duyệt!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new AuctionResponse(false, "ADD_PRODUCT_RESULT", "Lỗi server khi đăng sản phẩm.");
    }

    public AuctionResponse placeBid(int productId, String bidderName, double bidAmount) {
        String sql = "UPDATE products SET current_price = ?, owner_name = ? WHERE id = ? AND status = 'OPEN' AND current_price < ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, bidAmount);
            pstmt.setString(2, bidderName);
            pstmt.setInt(3, productId);
            pstmt.setDouble(4, bidAmount);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                return new AuctionResponse(true, "BID_RESULT", "Đặt giá thành công!");
            } else {
                return new AuctionResponse(false, "BID_RESULT", "Đặt giá thất bại! Giá đã bị vượt qua hoặc phiên đóng.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new AuctionResponse(false, "BID_RESULT", "Lỗi Server.");
        }
    }
}