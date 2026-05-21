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

    // Trong file AuctionService.java, tìm phương thức addProduct và thay thế bằng đoạn code sau:

    public AuctionResponse addProduct(ProductDTO product) {
        try {
            // --- BẮT ĐẦU XỬ LÝ LƯU ẢNH ---
            String imageUrl = "server_images/default.png"; // Đường dẫn mặc định nếu không có ảnh

            if (product.getImageBytes() != null && product.getImageBytes().length > 0) {
                // 1. Tạo thư mục nếu chưa tồn tại
                String uploadDir = "server_images";
                java.io.File dir = new java.io.File(uploadDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                // 2. Tạo tên file duy nhất
                String fileName = "sp_" + System.currentTimeMillis() + ".jpg";
                java.io.File imageFile = new java.io.File(uploadDir + "/" + fileName);

                // 3. Ghi mảng byte ra file
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(imageFile)) {
                    fos.write(product.getImageBytes());
                }

                // 4. Lấy đường dẫn tương đối để lưu vào DB
                imageUrl = uploadDir + "/" + fileName;

                // 5. Giải phóng bộ nhớ
                product.setImageBytes(null);
            }
            // Gán imageUrl vào productDTO (nếu bạn muốn lưu trữ lại trong object, dù không bắt buộc cho DAO)
            product.setImageUrl(imageUrl);
            // --- KẾT THÚC XỬ LÝ LƯU ẢNH ---

            // Gọi DAO, truyền thêm tham số imageUrl
            boolean ok = productDAO.addProduct(
                    product.getName(),
                    product.getDescription(),
                    product.getStartingPrice(),
                    product.getStepPrice(),
                    product.getSellerName(),
                    product.getStartTime(),
                    product.getEndTime(),
                    imageUrl
            );
            if (ok) return new AuctionResponse(true, "ADD_PRODUCT_RESULT",
                    "Gửi yêu cầu đăng bán thành công! Chờ Admin duyệt.", null);
        } catch (Exception e) {
            System.err.println("[AuctionService.addProduct] Lỗi: " + e.getMessage());
            e.printStackTrace();
        }
        return new AuctionResponse(false, "ADD_PRODUCT_RESULT", "Lỗi server khi đăng sản phẩm.", null);
    }

    public AuctionResponse placeBid(int productId, String bidderName, double bidAmount) {
        try {
            String role = userDAO.getRole(bidderName);
            // THÊM ADMIN VÀO ĐỂ CHẶN Ở TẦNG SERVER
            if ("SELLER".equals(role) || "ADMIN".equals(role)) {
                return new AuctionResponse(false, "BID_RESULT", "Quản trị viên và Người bán không được phép tham gia đấu giá!", null);
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
            List<ProductDTO> list = productDAO.getJoinedProducts(username);
            return new AuctionResponse(true, "GET_JOINED_PRODUCTS_RESULT", list);
        } catch (Exception e) {
            return new AuctionResponse(false, "GET_JOINED_PRODUCTS_RESULT", "Lỗi: " + e.getMessage(), null);
        }
    }

}