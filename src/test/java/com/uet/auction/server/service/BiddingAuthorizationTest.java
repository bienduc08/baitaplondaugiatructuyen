package com.uet.auction.server.service;

import com.uet.auction.common.Response.AuctionResponse;
import com.uet.auction.server.DAO.UserDAO;
import com.uet.auction.server.config.DatabaseConnection;
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
 * Lớp kiểm thử tích hợp cho chức năng phân quyền đấu giá thầu
 * của lớp AuctionService (chỉ cho phép USER đặt giá, chặn ADMIN/SELLER,
 * và kiểm tra số dư).
 */
public class BiddingAuthorizationTest {

    private AuctionService auctionService;
    private UserDAO userDAO;
    private boolean dbConnected = false;

    // Tên các tài khoản mẫu
    private final String testSeller = "seller_bidding_auth";
    private final String testAdmin = "admin_bidding_auth";
    private final String testUser = "user_bidding_auth";
    private int testProductId = -1;

    @BeforeEach
    public void setUp() {
        auctionService = AuctionService.getInstance();
        userDAO = new UserDAO();
        try {
            Connection c = DatabaseConnection.getConnection();
            if (c != null && !c.isClosed()) {
                dbConnected = true;
                c.setAutoCommit(true);
                prepareTestData();
            }
        } catch (Exception e) {
            System.out.println("[WARNING] BiddingAuthorizationTest: Không có CSDL. Bỏ qua các test database.");
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
                    stmt.executeUpdate("DELETE FROM users WHERE username IN ('" + testSeller + "', '" + testAdmin + "', '" + testUser + "')");
                }
            } catch (SQLException e) {
                System.err.println("Lỗi khi dọn dẹp dữ liệu test: " + e.getMessage());
            }
        }
    }

    private void prepareTestData() throws SQLException {
        // 1. Tạo 3 người dùng với các vai trò khác nhau
        userDAO.registerUser("Seller Auth Test", testSeller, "seller_auth@test.com", "99991001", "password123", "SELLER");
        userDAO.registerUser("Admin Auth Test", testAdmin, "admin_auth@test.com", "99991002", "password123", "ADMIN");
        userDAO.registerUser("User Auth Test", testUser, "user_auth@test.com", "99991003", "password123", "USER");

        // Đảm bảo số dư ban đầu của User Auth Test bằng 0 để kiểm tra lỗi số dư
        Connection c = DatabaseConnection.getConnection();
        try (Statement stmt = c.createStatement()) {
            stmt.executeUpdate("UPDATE users SET balance = 0.0 WHERE username = '" + testUser + "'");

            // 2. Tạo sản phẩm đấu giá đang mở (OPEN) của người bán mẫu
            String insertProductSql = "INSERT INTO products (name, description, starting_price, current_price, step_price, seller_name, owner_name, start_time, end_time, status) " +
                    "VALUES ('Product Auth Test', 'Test Description', 100000, 100000, 10000, '" + testSeller + "', NULL, " +
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
     * Test chặn vai trò ADMIN tham gia đặt giá.
     */
    @Test
    public void testPlaceBid_AdminBlocked() {
        Assumptions.assumeTrue(dbConnected, "Bỏ qua test vì không có kết nối Database");

        // Đặt giá cho sản phẩm test: giá khởi điểm là 100k, bước giá 10k -> giá yêu cầu tiếp theo là 110k
        AuctionResponse response = auctionService.placeBid(testProductId, testAdmin, new BigDecimal("110000.0"));

        assertFalse(response.isSuccess(), "Admin không được phép đặt giá");
        assertEquals("Tài khoản Người bán và Quản trị viên không được phép tham gia đấu giá!", response.getMessage());
    }

    /**
     * Test chặn vai trò SELLER tham gia đặt giá.
     */
    @Test
    public void testPlaceBid_SellerBlocked() {
        Assumptions.assumeTrue(dbConnected, "Bỏ qua test vì không có kết nối Database");

        AuctionResponse response = auctionService.placeBid(testProductId, testSeller, new BigDecimal("110000.0"));

        assertFalse(response.isSuccess(), "Seller không được phép đặt giá");
        assertEquals("Tài khoản Người bán và Quản trị viên không được phép tham gia đấu giá!", response.getMessage());
    }

    /**
     * Test chặn đặt giá nếu tài khoản USER không đủ số dư.
     */
    @Test
    public void testPlaceBid_InsufficientBalance() {
        Assumptions.assumeTrue(dbConnected, "Bỏ qua test vì không có kết nối Database");

        // Số dư hiện tại của testUser là 0.0, nhưng đặt giá 110.000 VNĐ
        AuctionResponse response = auctionService.placeBid(testProductId, testUser, new BigDecimal("110000.0"));

        assertFalse(response.isSuccess(), "Đặt giá phải thất bại khi số dư nhỏ hơn số tiền đặt");
        assertTrue(response.getMessage().contains("Số dư"), "Thông báo trả về phải hiển thị số dư không đủ");
    }

    /**
     * Test cho phép tài khoản USER đặt giá thành công khi có đủ số dư.
     */
    @Test
    public void testPlaceBid_Success() {
        Assumptions.assumeTrue(dbConnected, "Bỏ qua test vì không có kết nối Database");

        // Nạp tiền cho người dùng mẫu bằng UserDAO
        userDAO.deposit(testUser, new BigDecimal("200000.0"));

        // Đặt giá 110.000 VNĐ (thỏa mãn giá hiện tại 100.000 + bước giá 10.000)
        AuctionResponse response = auctionService.placeBid(testProductId, testUser, new BigDecimal("110000.0"));

        assertTrue(response.isSuccess(), "USER đủ điều kiện và đủ số dư đặt giá phải thành công");
        assertEquals("Đặt giá thành công!", response.getMessage());

        // Kiểm tra xem số dư người dùng có bị trừ đúng 110.000 VNĐ hay không (200.000 - 110.000 = 90.000)
        BigDecimal remainingBalance = userDAO.getBalance(testUser);
        assertTrue(new BigDecimal("90000.0").compareTo(remainingBalance) == 0, "Số dư tài khoản phải bị trừ chính xác");
    }
}
