package com.uet.auction.server.service;

import com.uet.auction.common.DTO.BidDTO;
import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.common.Response.AuctionResponse;
import com.uet.auction.server.DAO.BidDAO;
import com.uet.auction.server.DAO.ProductDAO;
import com.uet.auction.server.DAO.UserDAO;

import java.time.LocalDateTime;
import java.util.List;

public class AuctionService {

    private final ProductDAO productDAO = new ProductDAO();
    private final BidDAO     bidDAO     = new BidDAO();
    private final UserDAO    userDAO    = new UserDAO();

    public AuctionResponse getProductsByStatus(String status) {
        List<ProductDTO> list = productDAO.getProductsByStatus(status);

        // =========================================================
        // LOG KIỂM TRA SERVER: Biết ngay Database có trả ra sản phẩm nào không
        // =========================================================
        System.out.println("[Server LOG] Thực hiện lấy sản phẩm trạng thái [" + status + "]. Tìm thấy: "
                + (list != null ? list.size() : 0) + " sản phẩm trong CSDL.");

        String resultType;
        if ("ALL".equals(status)) {
            resultType = "GET_ALL_PRODUCTS_RESULT";
        } else if ("PENDING".equals(status)) {
            resultType = "GET_PENDING_PRODUCTS_RESULT";
        } else {
            resultType = "GET_PRODUCTS_RESULT";
        }

        return new AuctionResponse(true, resultType, list);
    }

    public AuctionResponse getProductsBySeller(String sellerName) {
        List<ProductDTO> products = productDAO.getProductsBySeller(sellerName);
        if (products != null) {
            return new AuctionResponse(true, "GET_MY_PRODUCTS_RESULT", products);
        } else {
            return new AuctionResponse(false, "GET_MY_PRODUCTS_RESULT", "Không thể lấy danh sách sản phẩm!", null);
        }
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
        try {
            String sellerName = productDAO.getSellerName(productId);
            if (sellerName != null && sellerName.equals(bidderName)) {
                return new AuctionResponse(false, "BID_RESULT",
                        "Bạn không thể đấu giá sản phẩm do chính mình đăng bán!", null);
            }
        } catch (Exception e) {
            System.err.println("[AuctionService] " + e.getMessage());
        }

        try {
            double balance = userDAO.getBalance(bidderName);
            if (balance > 0 && balance < bidAmount) {
                return new AuctionResponse(false, "BID_RESULT",
                        String.format("Số dư không đủ! Số dư: %,.0f VNĐ", balance), null);
            }
        } catch (Exception e) {}

        boolean ok = bidDAO.placeBid(productId, bidderName, bidAmount);
        return ok
                ? new AuctionResponse(true,  "BID_RESULT", "Đặt giá thành công!", null)
                : new AuctionResponse(false, "BID_RESULT", "Đặt giá thất bại!", null);
    }

    public AuctionResponse getBidHistory(int productId) {
        try {
            List<BidDTO> list = bidDAO.getBidsByProductId(productId);
            return new AuctionResponse(true, "GET_BID_HISTORY_RESULT", list);
        } catch (Exception e) {
            return new AuctionResponse(false, "GET_BID_HISTORY_RESULT", "Lỗi: " + e.getMessage(), null);
        }
    }

    public AuctionResponse getMyBids(String username) {
        try {
            List<BidDTO> list = bidDAO.getBidsByUsername(username);
            return new AuctionResponse(true, "GET_MY_BIDS_RESULT", list);
        } catch (Exception e) {
            return new AuctionResponse(false, "GET_MY_BIDS_RESULT", "Lỗi: " + e.getMessage(), null);
        }
    }

    public AuctionResponse getJoinedProducts(String username) {
        try {
            List<ProductDTO> list = productDAO.getJoinedProductsByUsername(username);
            return new AuctionResponse(true, "GET_JOINED_PRODUCTS_RESULT", list);
        } catch (Exception e) {
            return new AuctionResponse(false, "GET_JOINED_PRODUCTS_RESULT", "Lỗi: " + e.getMessage(), null);
        }
    }
}