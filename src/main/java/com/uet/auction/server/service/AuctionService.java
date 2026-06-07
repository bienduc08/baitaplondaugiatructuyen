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

    // Auto-bid registry: productId → danh sách người đã đăng ký
    private final Map<Integer, List<AutoBidConfig>> autoBidRegistry = new ConcurrentHashMap<>();

    // =========================================================
    // ĐĂNG KÝ AUTO-BID
    // =========================================================

    /**
     * Đăng ký hoặc cập nhật cấu hình đấu tự động cho một user trong một phiên.
     */
    public synchronized AuctionResponse registerAutoBid(AutoBidConfig config) {
        int productId = config.getAuctionId();
        autoBidRegistry.putIfAbsent(productId, new ArrayList<>());
        List<AutoBidConfig> configs = autoBidRegistry.get(productId);

        // Cập nhật nếu user đã đăng ký trước
        configs.removeIf(c -> c.getBidderId() == config.getBidderId());
        configs.add(config);

        System.out.println("[AutoBid] Đăng ký: user=" + config.getBidderUsername()
                + " | phiên=" + productId
                + " | giới hạn=" + config.getMaxBid()
                + " | bước=" + config.getIncrement());

        return new AuctionResponse(true, "REGISTER_AUTO_BID_RESULT", "Đăng ký đấu tự động thành công!", null);
    }

    // =========================================================
    // KÍCH HOẠT AUTO-BID (sau mỗi bid thủ công)
    // =========================================================

    /**
     * Kích hoạt auto-bid cho một phiên cụ thể sau khi có bid mới.
     * Bỏ qua người vừa đặt thủ công (lastBidder) để tránh phản ứng ngay lập tức.
     * Dùng getProductById thay vì getProductsByStatus để tránh query toàn bảng.
     *
     * @param productId  ID phiên đấu giá
     * @param lastBidder Username vừa đặt giá (null nếu gọi từ Timer)
     */
    public synchronized void triggerAutoBid(int productId, String lastBidder) {
        List<AutoBidConfig> configs = autoBidRegistry.get(productId);
        if (configs == null || configs.isEmpty()) return;

        boolean keepGoing = true;
        while (keepGoing) {
            keepGoing = false;

            // FIX: dùng getProductById thay vì kéo toàn bộ sản phẩm OPEN
            ProductDTO product = productDAO.getProductById(productId);
            if (product == null || !"OPEN".equals(product.getStatus())) break;

            double currentPrice = product.getCurrentPrice();
            String currentOwner = product.getOwnerName();

            PriorityQueue<AutoBidConfig> queue = new PriorityQueue<>(configs);
            for (AutoBidConfig cfg : queue) {
                String username = cfg.getBidderUsername();

                // FIX: bỏ qua người đang giữ đỉnh HOẶC người vừa đặt thủ công
                if (username.equals(currentOwner)) continue;
                if (lastBidder != null && username.equals(lastBidder)) continue;
                if (!cfg.isActive()) continue;

                Double nextBid = cfg.calculateNextBid(currentPrice);
                if (nextBid == null) {
                    cfg.setActive(false);
                    // Thông báo cho user biết auto-bid đã hết giới hạn
                    System.out.println("[AutoBid] User=" + username
                            + " đã hết maxBid=" + cfg.getMaxBid()
                            + " tại phiên=" + productId);
                    continue;
                }

                boolean ok = bidDAO.placeBid(productId, username, nextBid);
                if (ok) {
                    SocketServer.broadcastToLoggedInUsers(new AuctionResponse(true, "UPDATE_PRICE", null));
                    keepGoing = true;
                    break;
                }
            }
        }
    }

    /**
     * Kích hoạt auto-bid cho TẤT CẢ phiên còn đang OPEN trong registry.
     * Chỉ gọi từ AuctionTimer — trước khi closeExpiredAuctions().
     */
    public synchronized void triggerAllAutoBids() {
        for (Integer productId : autoBidRegistry.keySet()) {
            ProductDTO product = productDAO.getProductById(productId);
            // Chỉ trigger nếu phiên còn OPEN — bỏ qua phiên đã CLOSED
            if (product != null && "OPEN".equals(product.getStatus())) {
                triggerAutoBid(productId, null);
            }
        }
    }

    // =========================================================
    // LẤY SẢN PHẨM
    // =========================================================

    public AuctionResponse getProductsByStatus(String status) {
        List<ProductDTO> list = productDAO.getProductsByStatus(status);

        System.out.println("[Server] Lấy sản phẩm [" + status + "]: "
                + (list != null ? list.size() : 0) + " kết quả.");

        String resultType;
        if ("ALL".equals(status))         resultType = "GET_ALL_PRODUCTS_RESULT";
        else if ("PENDING".equals(status)) resultType = "GET_PENDING_PRODUCTS_RESULT";
        else                               resultType = "GET_PRODUCTS_RESULT";

        return new AuctionResponse(true, resultType, list);
    }

    public AuctionResponse getProductsBySeller(String sellerName) {
        List<ProductDTO> products = productDAO.getProductsBySeller(sellerName);
        if (products != null) {
            return new AuctionResponse(true, "GET_MY_PRODUCTS_RESULT", products);
        }
        return new AuctionResponse(false, "GET_MY_PRODUCTS_RESULT", "Không thể lấy danh sách sản phẩm!", null);
    }

    public AuctionResponse changeProductStatus(int productId, String newStatus) {
        boolean ok = productDAO.updateProductStatus(productId, newStatus);
        return ok
                ? new AuctionResponse(true,  "CHANGE_STATUS_RESULT", "Cập nhật thành công!", null)
                : new AuctionResponse(false, "CHANGE_STATUS_RESULT", "Cập nhật thất bại!", null);
    }

    // =========================================================
    // THÊM SẢN PHẨM
    // =========================================================

    public AuctionResponse addProduct(ProductDTO product) {
        try {
            System.out.println("[AuctionService] Đăng sản phẩm: " + product.getName()
                    + " | Seller: " + product.getSellerName()
                    + " | Giá: " + product.getStartingPrice()
                    + " | Bước: " + product.getStepPrice());

            // Validation phía server
            if (product.getName() == null || product.getName().trim().isEmpty())
                return new AuctionResponse(false, "ADD_PRODUCT_RESULT", "Tên sản phẩm không được để trống!", null);
            if (product.getStartingPrice() <= 0)
                return new AuctionResponse(false, "ADD_PRODUCT_RESULT", "Giá khởi điểm phải lớn hơn 0!", null);
            if (product.getStepPrice() <= 0)
                return new AuctionResponse(false, "ADD_PRODUCT_RESULT", "Bước giá phải lớn hơn 0!", null);
            if (product.getEndTime() == null)
                return new AuctionResponse(false, "ADD_PRODUCT_RESULT", "Thời gian kết thúc không được để trống!", null);
            if (product.getSellerName() == null || product.getSellerName().trim().isEmpty())
                return new AuctionResponse(false, "ADD_PRODUCT_RESULT", "Không xác định được người bán!", null);

            // Lưu ảnh
            String imageUrl = "images/default-product.png";
            if (product.getImageBytes() != null && product.getImageBytes().length > 0) {
                java.io.File dir = new java.io.File("images");
                if (!dir.exists()) dir.mkdirs();

                String fileName = "sp_" + System.currentTimeMillis() + ".jpg";
                java.io.File imageFile = new java.io.File("images/" + fileName);
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(imageFile)) {
                    fos.write(product.getImageBytes());
                }
                imageUrl = "images/" + fileName;
                product.setImageBytes(null);
            }
            product.setImageUrl(imageUrl);

            // Lưu vào DB
            boolean ok = productDAO.addProduct(
                    product.getName(), product.getDescription(),
                    product.getStartingPrice(), product.getStepPrice(),
                    product.getSellerName(), product.getStartTime(),
                    product.getEndTime(), imageUrl);

            if (ok) {
                System.out.println("[AuctionService] Đăng thành công: " + product.getName());
                return new AuctionResponse(true, "ADD_PRODUCT_RESULT", "Gửi yêu cầu đăng bán thành công! Chờ Admin duyệt.", null);
            }
            System.err.println("[AuctionService] DAO lỗi khi thêm: " + product.getName());
            return new AuctionResponse(false, "ADD_PRODUCT_RESULT", "Lỗi khi lưu sản phẩm vào cơ sở dữ liệu!", null);

        } catch (Exception e) {
            System.err.println("[AuctionService.addProduct] " + e.getMessage());
            e.printStackTrace();
            return new AuctionResponse(false, "ADD_PRODUCT_RESULT", "Lỗi server: " + e.getMessage(), null);
        }
    }

    // =========================================================
    // ĐẶT GIÁ
    // =========================================================

    /**
     * Xử lý đặt giá thủ công:
     * 1. Kiểm tra role (Admin/Seller không được đặt)
     * 2. Kiểm tra số dư
     * 3. Gọi BidDAO.placeBid() — transaction đầy đủ trong DAO
     * Hai bước 1 và 2 gộp vào một try-catch để lỗi DB không bị nuốt im.
     */
    public AuctionResponse placeBid(int productId, String bidderName, double bidAmount) {
        // Bước 1 + 2: kiểm tra role và số dư — gộp vào một khối try
        try {
            String role = userDAO.getRole(bidderName);
            if ("ADMIN".equals(role) || "SELLER".equals(role)) {
                return new AuctionResponse(false, "BID_RESULT",
                        "Tài khoản Người bán và Quản trị viên không được phép tham gia đấu giá!", null);
            }

            double balance = userDAO.getBalance(bidderName);
            if (balance < bidAmount) {
                return new AuctionResponse(false, "BID_RESULT",
                        String.format("Số dư không đủ! Số dư hiện tại: %,.0f VNĐ", balance), null);
            }
        } catch (Exception e) {
            System.err.println("[AuctionService.placeBid] Lỗi kiểm tra role/balance: " + e.getMessage());
            return new AuctionResponse(false, "BID_RESULT", "Lỗi kết nối máy chủ. Vui lòng thử lại!", null);
        }

        // Bước 3: đặt giá
        boolean ok = bidDAO.placeBid(productId, bidderName, bidAmount);
        return ok
                ? new AuctionResponse(true,  "BID_RESULT", "Đặt giá thành công!", null)
                : new AuctionResponse(false, "BID_RESULT",
                "Đặt giá thất bại! Giá phải cao hơn giá hiện tại + bước giá, hoặc bạn đang giữ đỉnh.", null);
    }

    // =========================================================
    // LỊCH SỬ ĐẤU GIÁ
    // =========================================================

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