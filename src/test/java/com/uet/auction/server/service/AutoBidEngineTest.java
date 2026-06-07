package com.uet.auction.server.service;

import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.common.Response.AuctionResponse;
import com.uet.auction.server.DAO.ProductDAO;
import com.uet.auction.server.DAO.UserDAO;
import com.uet.auction.server.config.DatabaseConnection;
import com.uet.auction.server.model.AutoBidConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import java.math.BigDecimal;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử cho chức năng tự động đấu thầu (Auto-bid Engine).
 * Bao gồm unit test cho lớp mô hình AutoBidConfig và integration test
 * cho luồng đấu giá tự động (AuctionService.triggerAutoBid).
 */
public class AutoBidEngineTest {

    private AuctionService auctionService;
    private ProductDAO productDAO;
    private UserDAO userDAO;
    private boolean dbConnected = false;

    // Các thông tin kiểm thử
    private final String testSeller = "seller_autobid_test";
    private final String testBidder1 = "bidder_autobid_1";
    private final String testBidder2 = "bidder_autobid_2";
    private int testProductId = -1;

    @BeforeEach
    public void setUp() {
        auctionService = AuctionService.getInstance();
        productDAO = new ProductDAO();
        userDAO = new UserDAO();
        try {
            Connection c = DatabaseConnection.getConnection();
            if (c != null && !c.isClosed()) {
                dbConnected = true;
                c.setAutoCommit(true);
                prepareTestData();
            }
        } catch (Exception e) {
            System.out.println("[WARNING] AutoBidEngineTest: Không có CSDL. Bỏ qua các test database.");
            dbConnected = false;
        }
    }

    @AfterEach
    public void tearDown() {
        if (dbConnected) {
            try {
                Connection c = DatabaseConnection.getConnection();
                try (Statement stmt = c.createStatement()) {
                    if (testProductId != -1) {
                        stmt.executeUpdate("DELETE FROM bids WHERE product_id = " + testProductId);
                        stmt.executeUpdate("DELETE FROM products WHERE id = " + testProductId);
                    }
                    stmt.executeUpdate("DELETE FROM users WHERE username IN ('" + testSeller + "', '" + testBidder1 + "', '" + testBidder2 + "')");
                }
            } catch (SQLException e) {
                System.err.println("Lỗi khi dọn dẹp dữ liệu test: " + e.getMessage());
            }
        }
    }

    private void prepareTestData() throws SQLException {
        // 1. Tạo 3 người dùng
        userDAO.registerUser("Seller Autobid", testSeller, "seller_auto@test.com", "99992001", "password123", "SELLER");
        userDAO.registerUser("Bidder Autobid 1", testBidder1, "bidder_auto1@test.com", "99992002", "password123", "USER");
        userDAO.registerUser("Bidder Autobid 2", testBidder2, "bidder_auto2@test.com", "99992003", "password123", "USER");

        // Nạp nhiều tiền cho 2 người tham gia đấu giá tự động
        userDAO.deposit(testBidder1, new BigDecimal("1000000.0"));
        userDAO.deposit(testBidder2, new BigDecimal("1000000.0"));

        // 2. Tạo sản phẩm đấu giá đang mở (OPEN)
        Connection c = DatabaseConnection.getConnection();
        try (Statement stmt = c.createStatement()) {
            String insertProductSql = "INSERT INTO products (name, description, starting_price, current_price, step_price, seller_name, owner_name, start_time, end_time, status) " +
                    "VALUES ('Product Autobid Test', 'Test', 100000, 100000, 10000, '" + testSeller + "', NULL, " +
                    "DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 1 HOUR), 'OPEN')";
            
            stmt.executeUpdate(insertProductSql, Statement.RETURN_GENERATED_KEYS);
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    testProductId = rs.getInt(1);
                }
            }
        }
    }

    /**
     * Unit Test: Kiểm tra logic tính toán mức giá đặt tiếp theo của AutoBidConfig.
     */
    @Test
    public void testAutoBidConfig_NextBidCalculation() {
        // Cấu hình: Max bid = 150.000, bước giá tăng thêm = 10.000
        AutoBidConfig config = new AutoBidConfig(1, "bidder1", 101, new BigDecimal("150000.0"), new BigDecimal("10000.0"));

        // Giá hiện tại: 100.000 -> Giá tiếp theo: 110.000 (Hợp lệ)
        BigDecimal nextBid = config.calculateNextBid(new BigDecimal("100000.0"));
        assertNotNull(nextBid);
        assertEquals(new BigDecimal("110000.0"), nextBid);

        // Giá hiện tại: 140.000 -> Giá tiếp theo: 150.000 (Đúng mức tối đa)
        nextBid = config.calculateNextBid(new BigDecimal("140000.0"));
        assertNotNull(nextBid);
        assertEquals(new BigDecimal("150000.0"), nextBid);

        // Giá hiện tại: 145.000 -> Giá tiếp theo: 155.000 (Vượt quá max 150.000 -> trả về null)
        nextBid = config.calculateNextBid(new BigDecimal("145000.0"));
        assertNull(nextBid, "Giá tiếp theo vượt quá giá thầu tối đa phải trả về null để dừng auto-bid");
    }

    /**
     * Integration Test: Đăng ký Auto-bid thành công trên hệ thống.
     */
    @Test
    public void testRegisterAutoBid() {
        Assumptions.assumeTrue(dbConnected, "Bỏ qua test vì không có kết nối Database");

        AutoBidConfig config = new AutoBidConfig(10, testBidder1, testProductId, new BigDecimal("150000.0"), new BigDecimal("10000.0"));
        AuctionResponse response = auctionService.registerAutoBid(config);

        assertTrue(response.isSuccess());
        assertEquals("REGISTER_AUTO_BID_RESULT", response.getType());
    }

    /**
     * Integration Test: Luồng chạy của Auto-bid Engine.
     * Mô phỏng cuộc chiến giá thầu tự động giữa 2 người tham gia đấu giá tự động:
     * - Bidder 1: max 150k, bước giá 10k
     * - Bidder 2: max 180k, bước giá 10k
     * Mong muốn: Cuộc đấu thầu tự động diễn ra cho tới khi chạm mốc 150k (Bidder 2 thắng ở mốc 150k, Bidder 1 bỏ cuộc do vượt quá giới hạn 150k).
     */
    @Test
    public void testAutoBidBiddingWar() {
        Assumptions.assumeTrue(dbConnected, "Bỏ qua test vì không có kết nối Database");

        // 1. Đăng ký Auto-bid cho Bidder 1 (Max 150.000, Increment 10.000)
        AutoBidConfig config1 = new AutoBidConfig(1, testBidder1, testProductId, new BigDecimal("150000.0"), new BigDecimal("10000.0"));
        auctionService.registerAutoBid(config1);

        // 2. Đăng ký Auto-bid cho Bidder 2 (Max 180.000, Increment 10.000)
        AutoBidConfig config2 = new AutoBidConfig(2, testBidder2, testProductId, new BigDecimal("180000.0"), new BigDecimal("10000.0"));
        auctionService.registerAutoBid(config2);

        // 3. Kích hoạt Auto-bid Engine cho sản phẩm
        auctionService.triggerAutoBid(testProductId, null);

        // 4. Kiểm chứng kết quả cuối cùng từ cơ sở dữ liệu
        ProductDTO updatedProduct = productDAO.getProductById(testProductId);
        assertNotNull(updatedProduct);

        // Kết quả mong muốn:
        // Giá hiện tại của sản phẩm phải là 150.000 VNĐ
        // Người giữ đỉnh cuối cùng phải là bidder_autobid_2
        assertTrue(new BigDecimal("150000.0").compareTo(updatedProduct.getCurrentPrice()) == 0,
                "Giá thầu cuối cùng phải dừng ở 150.000 VNĐ (mức cao nhất mà bidder 1 có thể cạnh tranh)");
        assertEquals(testBidder2, updatedProduct.getOwnerName(),
                "Bidder 2 phải là người chiến thắng cuộc đua thầu tự động");
        
        // Kiểm tra xem cấu hình Auto-bid của Bidder 1 có tự động bị vô hiệu hóa (active = false) không
        assertFalse(config1.isActive(), "Cấu hình của Bidder 1 phải bị đánh dấu inactive do giá vượt quá giới hạn");
        assertTrue(config2.isActive(), "Cấu hình của Bidder 2 vẫn giữ nguyên trạng thái active");
    }
}
