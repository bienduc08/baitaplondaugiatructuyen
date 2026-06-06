package com.uet.auction.server.service;

import com.uet.auction.common.DTO.BidDTO;
import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.common.Response.AuctionResponse;
import com.uet.auction.server.DAO.BidDAO;
import com.uet.auction.server.DAO.ProductDAO;
import com.uet.auction.server.DAO.UserDAO;
import com.uet.auction.server.model.AutoBidConfig;
import com.uet.auction.server.network.SocketServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionService {
    private static final AuctionService INSTANCE = new AuctionService();
    public static AuctionService getInstance() { return INSTANCE; }
    private AuctionService() {}

    private final ProductDAO productDAO = new ProductDAO();
    private final BidDAO     bidDAO     = new BidDAO();
    private final UserDAO    userDAO    = new UserDAO();
    private final Map<Integer, List<AutoBidConfig>> autoBidRegistry = new ConcurrentHashMap<>();

    // =====================================================================
    // NHÓM 1: SẢN PHẨM (PRODUCT LOGIC)
    // =====================================================================

    public AuctionResponse getProductsByStatus(String status) {
        List<ProductDTO> list = productDAO.getProductsByStatus(status);
        String resultType;
        switch (status) {
            case "ALL":
                resultType = "GET_ALL_PRODUCTS_RESULT";
                break;
            case "PENDING":
                resultType = "GET_PENDING_PRODUCTS_RESULT";
                break;
            case "CLOSED":
                resultType = "GET_CLOSED_PRODUCTS_RESULT"; // Thêm dòng này để tách riêng kết quả
                break;
            default:
                resultType = "GET_PRODUCTS_RESULT"; // Mặc định cho OPEN
                break;
        }
        return new AuctionResponse(true, resultType, list);
    }

    public AuctionResponse getProductsBySeller(String sellerName) {
        List<ProductDTO> products = productDAO.getProductsBySeller(sellerName);
        return products != null ? new AuctionResponse(true, "GET_MY_PRODUCTS_RESULT", products)
                : new AuctionResponse(false, "GET_MY_PRODUCTS_RESULT", "Lỗi!", null);
    }

    public AuctionResponse getJoinedProducts(String username) {
        try {
            return new AuctionResponse(true, "GET_JOINED_PRODUCTS_RESULT", productDAO.getJoinedProducts(username));
        } catch (Exception e) {
            return new AuctionResponse(false, "GET_JOINED_PRODUCTS_RESULT", "Lỗi: " + e.getMessage(), null);
        }
    }

    public AuctionResponse changeProductStatus(int productId, String newStatus) {
        boolean ok = productDAO.updateProductStatus(productId, newStatus);
        return ok ? new AuctionResponse(true,  "CHANGE_STATUS_RESULT", "Cập nhật thành công!", null)
                : new AuctionResponse(false, "CHANGE_STATUS_RESULT", "Cập nhật thất bại!", null);
    }

    public AuctionResponse addProduct(ProductDTO product) {
        try {
            if (product.getName() == null || product.getName().trim().isEmpty())
                return new AuctionResponse(false, "ADD_PRODUCT_RESULT", "Tên trống!", null);
            if (product.getStartingPrice() <= 0)
                return new AuctionResponse(false, "ADD_PRODUCT_RESULT", "Giá <= 0!", null);
            if (product.getEndTime() == null)
                return new AuctionResponse(false, "ADD_PRODUCT_RESULT", "Chưa nhập thời gian!", null);

            String imageUrl = "images/default-product.png";
            if (product.getImageBytes() != null && product.getImageBytes().length > 0) {
                String uploadDir = "images";
                new java.io.File(uploadDir).mkdirs();
                String fileName = "sp_" + System.currentTimeMillis() + ".jpg";
                java.io.File imageFile = new java.io.File(uploadDir + "/" + fileName);
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(imageFile)) {
                    fos.write(product.getImageBytes());
                }
                imageUrl = uploadDir + "/" + fileName;
                product.setImageBytes(null);
            }
            product.setImageUrl(imageUrl);

            boolean ok = productDAO.addProduct(product.getName(), product.getDescription(),
                    product.getStartingPrice(), product.getStepPrice(), product.getSellerName(),
                    product.getStartTime(), product.getEndTime(), imageUrl);

            return ok ? new AuctionResponse(true, "ADD_PRODUCT_RESULT", "Chờ Admin duyệt.", null)
                    : new AuctionResponse(false, "ADD_PRODUCT_RESULT", "Lỗi Database!", null);
        } catch (Exception e) {
            return new AuctionResponse(false, "ADD_PRODUCT_RESULT", "Lỗi server!", null);
        }
    }
    public AuctionResponse updateProduct(ProductDTO product) {
        try {
            if (product.getName() == null || product.getName().trim().isEmpty())
                return new AuctionResponse(false, "UPDATE_PRODUCT_RESULT", "Tên sản phẩm không được để trống!", null);
            if (product.getStartingPrice() <= 0)
                return new AuctionResponse(false, "UPDATE_PRODUCT_RESULT", "Giá khởi điểm phải lớn hơn 0!", null);
            if (product.getEndTime() == null)
                return new AuctionResponse(false, "UPDATE_PRODUCT_RESULT", "Chưa nhập thời gian kết thúc!", null);

            // Xử lý nếu người dùng có chọn ảnh mới
            if (product.getImageBytes() != null && product.getImageBytes().length > 0) {
                String uploadDir = "images";
                new java.io.File(uploadDir).mkdirs();
                String fileName = "sp_update_" + System.currentTimeMillis() + ".jpg";
                java.io.File imageFile = new java.io.File(uploadDir + "/" + fileName);
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(imageFile)) {
                    fos.write(product.getImageBytes());
                }
                product.setImageUrl(uploadDir + "/" + fileName); // Ghi đè đường dẫn ảnh mới
                product.setImageBytes(null); // Giải phóng bộ nhớ
            }

            // Gọi DAO để cập nhật DB
            boolean ok = productDAO.updateProduct(product);

            return ok ? new AuctionResponse(true, "UPDATE_PRODUCT_RESULT", "Cập nhật thành công! Sản phẩm đang chờ duyệt lại.", null)
                    : new AuctionResponse(false, "UPDATE_PRODUCT_RESULT", "Lỗi cập nhật Database!", null);
        } catch (Exception e) {
            return new AuctionResponse(false, "UPDATE_PRODUCT_RESULT", "Lỗi server: " + e.getMessage(), null);
        }
    }

    // =====================================================================
    // NHÓM 2: ĐẶT GIÁ (BID LOGIC)
    // =====================================================================

    public AuctionResponse placeBid(int productId, String bidderName, double bidAmount) {
        try {
            String role = userDAO.getRole(bidderName);
            if ("ADMIN".equals(role) || "SELLER".equals(role)) {
                return new AuctionResponse(false, "BID_RESULT", "Không có quyền đấu giá!", null);
            }
            double balance = userDAO.getBalance(bidderName);
            if (balance < bidAmount) {
                return new AuctionResponse(false, "BID_RESULT", String.format("Số dư: %,.0f VNĐ", balance), null);
            }
        } catch (Exception e) {
            return new AuctionResponse(false, "BID_RESULT", "Lỗi máy chủ khi lấy số dư!", null);
        }

        boolean ok = bidDAO.placeBid(productId, bidderName, bidAmount);
        return ok ? new AuctionResponse(true,  "BID_RESULT", "Đặt giá thành công!", null)
                : new AuctionResponse(false, "BID_RESULT", "Đặt giá thất bại!", null);
    }

    public AuctionResponse getBidHistory(int productId) {
        try {
            return new AuctionResponse(true, "GET_BID_HISTORY_RESULT", bidDAO.getBidsByProductId(productId));
        } catch (Exception e) {
            return new AuctionResponse(false, "GET_BID_HISTORY_RESULT", "Lỗi!", null);
        }
    }

    public AuctionResponse getMyBids(String username) {
        try {
            return new AuctionResponse(true, "GET_MY_BIDS_RESULT", bidDAO.getBidsByUsername(username));
        } catch (Exception e) {
            return new AuctionResponse(false, "GET_MY_BIDS_RESULT", "Lỗi!", null);
        }
    }

    // =====================================================================
    // NHÓM 3: ĐẤU TỰ ĐỘNG (AUTO-BID LOGIC)
    // =====================================================================

    public synchronized AuctionResponse registerAutoBid(AutoBidConfig config) {
        int productId = config.getAuctionId();
        autoBidRegistry.putIfAbsent(productId, new ArrayList<>());
        List<AutoBidConfig> configs = autoBidRegistry.get(productId);

        configs.removeIf(c -> c.getBidderId() == config.getBidderId());
        configs.add(config);
        return new AuctionResponse(true, "REGISTER_AUTO_BID_RESULT", "Đăng ký auto-bid thành công!", null);
    }

    public synchronized void triggerAutoBid(int productId, String lastBidder) {
        List<AutoBidConfig> configs = autoBidRegistry.get(productId);
        if (configs == null || configs.isEmpty()) return;

        boolean keepGoing = true;
        while (keepGoing) {
            keepGoing = false;

            ProductDTO product = productDAO.getProductById(productId);
            if (product == null || !"OPEN".equals(product.getStatus())) break;

            double currentPrice = product.getCurrentPrice();
            String currentOwner = product.getOwnerName();

            PriorityQueue<AutoBidConfig> queue = new PriorityQueue<>(configs);
            for (AutoBidConfig cfg : queue) {
                String username = cfg.getBidderUsername();
                if (username.equals(currentOwner) || username.equals(lastBidder) || !cfg.isActive()) continue;

                Double nextBid = cfg.calculateNextBid(currentPrice);
                if (nextBid == null) {
                    cfg.setActive(false);
                    continue;
                }

                if (bidDAO.placeBid(productId, username, nextBid)) {
                    SocketServer.broadcast(new AuctionResponse(true, "UPDATE_PRICE", null));
                    keepGoing = true;
                    break;
                }
            }
        }
    }

    public synchronized void triggerAllAutoBids() {
        for (Integer productId : autoBidRegistry.keySet()) triggerAutoBid(productId, null);
    }
    public AuctionResponse getDashboardStats() {
        try {
            // 1. Đếm tổng sản phẩm và tính tổng doanh thu (chỉ tính các phiên đã CLOSED và có người mua)
            List<ProductDTO> allProducts = productDAO.getProductsByStatus("ALL");
            int totalAuctions = allProducts.size();
            double totalRevenue = 0;

            for (ProductDTO p : allProducts) {
                if ("CLOSED".equals(p.getStatus()) && p.getOwnerName() != null && !p.getOwnerName().isBlank()) {
                    totalRevenue += p.getCurrentPrice();
                }
            }

            // 2. Đếm tổng số user (Dùng SQL đếm thẳng từ Database cho nhanh)
            int totalUsers = 0;
            String sql = "SELECT COUNT(*) FROM users WHERE role IN ('USER', 'SELLER')";
            try (java.sql.Connection conn = com.uet.auction.server.config.DatabaseConnection.getConnection();
                 java.sql.PreparedStatement pstmt = conn.prepareStatement(sql);
                 java.sql.ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) totalUsers = rs.getInt(1);
            }

            // 3. Đóng gói kết quả gửi về Client
            java.util.Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("totalUsers", totalUsers);
            stats.put("totalAuctions", totalAuctions);
            stats.put("totalRevenue", totalRevenue);

            return new AuctionResponse(true, "GET_STATS_SUCCESS", stats);
        } catch (Exception e) {
            return new AuctionResponse(false, "GET_STATS_SUCCESS", "Lỗi: " + e.getMessage(), null);
        }
    }
}