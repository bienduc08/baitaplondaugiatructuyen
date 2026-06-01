package com.uet.auction.server.service;

import com.uet.auction.common.DTO.BidDTO;
import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.common.Response.AuctionResponse;
import com.uet.auction.server.DAO.BidDAO;
import com.uet.auction.server.DAO.ProductDAO;
import com.uet.auction.server.DAO.UserDAO;
import com.uet.auction.server.model.AutoBidConfig;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionService {

    // =====================================================================
    // SINGLETON — đảm bảo autoBidRegistry dùng chung toàn server
    // =====================================================================
    private static final AuctionService INSTANCE = new AuctionService();
    public static AuctionService getInstance() { return INSTANCE; }
    private AuctionService() {}

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
            System.out.println("[AuctionService] Nhận yêu cầu đăng sản phẩm: " + product.getName()
                    + " | Seller: " + product.getSellerName()
                    + " | Giá: " + product.getStartingPrice()
                    + " | Bước giá: " + product.getStepPrice());

            // ====== VALIDATION PHÍA SERVER ======
            if (product.getName() == null || product.getName().trim().isEmpty()) {
                return new AuctionResponse(false, "ADD_PRODUCT_RESULT", "Tên sản phẩm không được để trống!", null);
            }
            if (product.getStartingPrice() <= 0) {
                return new AuctionResponse(false, "ADD_PRODUCT_RESULT", "Giá khởi điểm phải lớn hơn 0!", null);
            }
            if (product.getStepPrice() <= 0) {
                return new AuctionResponse(false, "ADD_PRODUCT_RESULT", "Bước giá phải lớn hơn 0!", null);
            }
            if (product.getEndTime() == null) {
                return new AuctionResponse(false, "ADD_PRODUCT_RESULT", "Thời gian kết thúc không được để trống!", null);
            }
            if (product.getSellerName() == null || product.getSellerName().trim().isEmpty()) {
                return new AuctionResponse(false, "ADD_PRODUCT_RESULT", "Không xác định được người bán!", null);
            }

            // ====== XỬ LÝ LƯU ẢNH ======
            String imageUrl = "images/default-product.png";

            if (product.getImageBytes() != null && product.getImageBytes().length > 0) {
                String uploadDir = "images";
                java.io.File dir = new java.io.File(uploadDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                String fileName = "sp_" + System.currentTimeMillis() + ".jpg";
                java.io.File imageFile = new java.io.File(uploadDir + "/" + fileName);

                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(imageFile)) {
                    fos.write(product.getImageBytes());
                }

                imageUrl = uploadDir + "/" + fileName;
                product.setImageBytes(null);
            }
            product.setImageUrl(imageUrl);

            // ====== GỌI DAO ĐỂ LƯU VÀO DATABASE ======
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

            if (ok) {
                System.out.println("[AuctionService] Đăng sản phẩm thành công: " + product.getName());
                return new AuctionResponse(true, "ADD_PRODUCT_RESULT",
                        "Gửi yêu cầu đăng bán thành công! Chờ Admin duyệt.", null);
            } else {
                System.err.println("[AuctionService] DAO trả về false khi thêm sản phẩm: " + product.getName());
                return new AuctionResponse(false, "ADD_PRODUCT_RESULT",
                        "Lỗi khi lưu sản phẩm vào cơ sở dữ liệu!", null);
            }
        } catch (Exception e) {
            System.err.println("[AuctionService.addProduct] Lỗi nghiêm trọng: " + e.getMessage());
            e.printStackTrace();
            return new AuctionResponse(false, "ADD_PRODUCT_RESULT",
                    "Lỗi server: " + e.getMessage(), null);
        }
    }

    public AuctionResponse placeBid(int productId, String bidderName, double bidAmount) {
        try {
            String role = userDAO.getRole(bidderName);
            if ("ADMIN".equals(role) || "SELLER".equals(role)) {
                return new AuctionResponse(false, "BID_RESULT", "Tài khoản Người bán và Quản trị viên không được phép tham gia đấu giá!", null);
            }
        } catch (Exception e) {
            System.err.println("[AuctionService] " + e.getMessage());
        }

        try {
            double balance = userDAO.getBalance(bidderName);
            if (balance < bidAmount) {
                return new AuctionResponse(false, "BID_RESULT",
                        String.format("Số dư không đủ! Số dư: %,.0f VNĐ", balance), null);
            }

        } catch (Exception e) {
            System.err.println("[AuctionService.placeBid] Lỗi kiểm tra số dư: " + e.getMessage());
        }

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
    // =====================================================================
    // TÍNH NĂNG 14: Đấu thầu tự động (Auto-Bid)
    // Lưu trữ cấu hình auto-bid theo productId → danh sách người đăng ký
    // =====================================================================
    private final Map<Integer, List<AutoBidConfig>> autoBidRegistry = new ConcurrentHashMap<>();

    /**
     * Đăng ký cấu hình đấu tự động cho một người dùng trong một phiên đấu giá.
     * Nếu người dùng đã đăng ký trước đó, cấu hình cũ sẽ bị cập nhật.
     */
    public synchronized AuctionResponse registerAutoBid(AutoBidConfig config) {
        int productId = config.getAuctionId();
        autoBidRegistry.putIfAbsent(productId, new ArrayList<>());
        List<AutoBidConfig> configs = autoBidRegistry.get(productId);

        // Cập nhật nếu user đã đăng ký trước
        configs.removeIf(c -> c.getBidderId() == config.getBidderId());
        configs.add(config);

        System.out.println("[AutoBid] Đăng ký thành công: user=" + config.getBidderUsername()
                + " | phiên=" + productId
                + " | giới hạn=" + config.getMaxBid()
                + " | bước=" + config.getIncrement());

        return new AuctionResponse(true, "REGISTER_AUTO_BID_RESULT",
                "Đăng ký đấu tự động thành công!", null);
    }

    /**
     * Kích hoạt đấu tự động cho một phiên sau khi có bid mới.
     * Tìm người có maxBid cao nhất (chưa giữ đỉnh) và đặt giá tự động.
     *
     * @param productId  ID phiên đấu giá
     * @param lastBidder Người vừa đặt giá (để bỏ qua trong vòng này)
     */
    public synchronized void triggerAutoBid(int productId, String lastBidder) {
        List<AutoBidConfig> configs = autoBidRegistry.get(productId);
        if (configs == null || configs.isEmpty()) return;

        // Giới hạn số vòng để tránh loop vô tận (tối đa bằng số người đăng ký)
        int maxRounds = configs.size() * 2;
        int round = 0;

        String currentOwner = lastBidder;

        while (round++ < maxRounds) {
            // Lấy giá hiện tại trực tiếp theo productId (không fetch toàn bộ)
            ProductDTO product = productDAO.getProductById(productId);
            if (product == null || !"OPEN".equals(product.getStatus())) break;

            double currentPrice = product.getCurrentPrice();
            currentOwner = product.getOwnerName();

            // Sắp xếp theo maxBid giảm dần
            PriorityQueue<AutoBidConfig> queue = new PriorityQueue<>(configs);

            boolean anyBid = false;
            for (AutoBidConfig cfg : queue) {
                String username = cfg.getBidderUsername();

                if (!cfg.isActive()) continue;
                if (username.equals(currentOwner)) continue; // đang giữ đỉnh

                BigDecimal nextBid = cfg.calculateNextBid(BigDecimal.valueOf(currentPrice));
                if (nextBid == null) {
                    cfg.setActive(false);
                    System.out.println("[AutoBid] " + username + " đạt giới hạn, vô hiệu hóa.");
                    continue;
                }

                boolean ok = bidDAO.placeBid(productId, username, nextBid.doubleValue());
                if (ok) {
                    System.out.println("[AutoBid] " + username + " → " + nextBid + " VNĐ | phiên=" + productId);
                    anyBid = true;
                    break; // Fetch lại giá mới nhất ở vòng while tiếp
                }
            }

            // Không ai bid thêm được → dừng
            if (!anyBid) break;
        }
    }


}