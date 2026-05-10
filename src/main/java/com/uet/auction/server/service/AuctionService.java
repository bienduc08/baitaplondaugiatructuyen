package com.uet.auction.server.service;

import com.uet.auction.common.DTO.BidDTO;
import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.common.Response.AuctionResponse;
import com.uet.auction.server.DAO.BidDAO;
import com.uet.auction.server.DAO.ProductDAO;

import java.time.LocalDateTime;
import java.util.List;

public class AuctionService {

    private final ProductDAO productDAO = new ProductDAO();
    private final BidDAO     bidDAO     = new BidDAO();

    public AuctionResponse getProductsByStatus(String status) {
        List<ProductDTO> list = productDAO.getProductsByStatus(status);
        // ALL → trả về result type riêng cho Admin
        String resultType = "ALL".equals(status) ? "GET_ALL_PRODUCTS_RESULT" : "GET_PRODUCTS_RESULT";
        return new AuctionResponse(true, resultType, list);
    }

    public AuctionResponse getProductsBySeller(String sellerName) {
        List<ProductDTO> list = productDAO.getProductsByStatus("ALL");
        list.removeIf(p -> !sellerName.equals(p.getSellerName()));
        return new AuctionResponse(true, "GET_MY_PRODUCTS_RESULT", list);
    }

    public AuctionResponse changeProductStatus(int productId, String newStatus) {
        boolean ok = productDAO.updateProductStatus(productId, newStatus);
        return ok
                ? new AuctionResponse(true,  "CHANGE_STATUS_RESULT", "Cập nhật thành công!", null)
                : new AuctionResponse(false, "CHANGE_STATUS_RESULT", "Cập nhật thất bại!", null);
    }

    public AuctionResponse addProduct(ProductDTO product) {
        try {
            boolean ok = productDAO.addProduct(
                    product.getName(),
                    product.getStartingPrice(),
                    product.getSellerName(),
                    product.getStartTime() != null ? product.getStartTime() : LocalDateTime.now(),
                    product.getEndTime(),
                    product.getDescription()
            );
            if (ok) return new AuctionResponse(true, "ADD_PRODUCT_RESULT",
                    "Gửi yêu cầu đăng bán thành công! Chờ Admin duyệt.", null);
        } catch (Exception e) {
            System.err.println("[AuctionService.addProduct] " + e.getMessage());
        }
        return new AuctionResponse(false, "ADD_PRODUCT_RESULT", "Lỗi server khi đăng sản phẩm.", null);
    }

    public AuctionResponse placeBid(int productId, String bidderName, double bidAmount) {
        System.out.println("[AuctionService.placeBid] productId=" + productId
                + " bidder=" + bidderName + " amount=" + bidAmount
                + " (type check: " + ((Object)bidAmount).getClass().getName() + ")");
        boolean ok = bidDAO.placeBid(productId, bidderName, bidAmount);
        return ok
                ? new AuctionResponse(true,  "BID_RESULT", "Đặt giá thành công!", null)
                : new AuctionResponse(false, "BID_RESULT",
                "Đặt giá thất bại! Giá đã bị vượt qua hoặc phiên đóng.", null);
    }

    public AuctionResponse getBidHistory(int productId) {
        try {
            List<BidDTO> list = bidDAO.getBidsByProductId(productId);
            return new AuctionResponse(true, "GET_BID_HISTORY_RESULT", list);
        } catch (Exception e) {
            return new AuctionResponse(false, "GET_BID_HISTORY_RESULT",
                    "Lỗi khi tải lịch sử: " + e.getMessage(), null);
        }
    }
}