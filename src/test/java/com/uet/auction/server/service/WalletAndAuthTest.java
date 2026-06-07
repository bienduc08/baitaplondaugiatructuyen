package com.uet.auction.server.service;

import com.uet.auction.common.Request.AuctionRequest;
import com.uet.auction.common.Request.LoginRequest;
import com.uet.auction.common.Response.AuctionResponse;
import com.uet.auction.server.DAO.UserDAO;
import com.uet.auction.server.config.DatabaseConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử tích hợp cho các chức năng Ví điện tử (nạp tiền)
 * và Xác thực (đăng nhập tài khoản bị khóa) của lớp AuthService.
 */
public class WalletAndAuthTest {

    private AuthService authService;
    private UserDAO userDAO;
    private boolean dbConnected = false;

    private final String testUserNormal = "user_test_wallet";
    private final String testUserLocked = "user_test_locked";

    @BeforeEach
    public void setUp() {
        authService = new AuthService();
        userDAO = new UserDAO();
        try {
            Connection c = DatabaseConnection.getConnection();
            if (c != null && !c.isClosed()) {
                dbConnected = true;
                c.setAutoCommit(true);
                prepareTestData();
            }
        } catch (Exception e) {
            System.out.println("[WARNING] WalletAndAuthTest: Không có CSDL. Bỏ qua các test database.");
            dbConnected = false;
        }
    }

    @AfterEach
    public void tearDown() {
        if (dbConnected) {
            try {
                Connection c = DatabaseConnection.getConnection();
                try (Statement stmt = c.createStatement()) {
                    stmt.executeUpdate("DELETE FROM users WHERE username IN ('" + testUserNormal + "', '" + testUserLocked + "')");
                }
            } catch (SQLException e) {
                System.err.println("Lỗi khi dọn dẹp dữ liệu test: " + e.getMessage());
            }
        }
    }

    private void prepareTestData() throws SQLException {
        // Tạo các tài khoản test bằng UserDAO
        userDAO.registerUser("Normal User Test", testUserNormal, "normal_wallet@test.com", "99990001", "password123", "USER");
        userDAO.registerUser("Locked User Test", testUserLocked, "locked_wallet@test.com", "99990002", "password123", "USER");

        // Khóa tài khoản testUserLocked
        Connection c = DatabaseConnection.getConnection();
        try (Statement stmt = c.createStatement()) {
            stmt.executeUpdate("UPDATE users SET status = 'LOCKED' WHERE username = '" + testUserLocked + "'");
        } catch (SQLException e) {
            System.err.println("Không thể thiết lập trạng thái LOCKED cho user test: " + e.getMessage());
        }
    }

    /**
     * Test đăng nhập thành công với tài khoản thông thường.
     */
    @Test
    public void testLogin_Success() {
        Assumptions.assumeTrue(dbConnected, "Bỏ qua test vì không có kết nối Database");

        AuctionRequest request = new AuctionRequest("LOGIN", new LoginRequest(testUserNormal, "password123"));
        AuctionResponse response = authService.login(request);

        assertTrue(response.isSuccess(), "Đăng nhập với tài khoản thường phải thành công");
        assertEquals("LOGIN_RESULT", response.getType());
    }

    /**
     * Test chặn đăng nhập khi tài khoản có trạng thái LOCKED.
     */
    @Test
    public void testLogin_LockedUser() {
        Assumptions.assumeTrue(dbConnected, "Bỏ qua test vì không có kết nối Database");

        AuctionRequest request = new AuctionRequest("LOGIN", new LoginRequest(testUserLocked, "password123"));
        AuctionResponse response = authService.login(request);

        assertFalse(response.isSuccess(), "Đăng nhập với tài khoản bị khóa phải thất bại");
        assertTrue(response.getMessage().contains("bị khóa") || response.getMessage().contains("Admin"),
                "Thông báo lỗi phải hiển thị thông tin tài khoản bị khóa");
    }

    /**
     * Test nạp tiền hợp lệ.
     */
    @Test
    public void testDeposit_Success() {
        Assumptions.assumeTrue(dbConnected, "Bỏ qua test vì không có kết nối Database");

        BigDecimal initialBalance = authService.getUserBalance(testUserNormal);
        BigDecimal depositAmount = new BigDecimal("200000.0");

        AuctionResponse response = authService.deposit(testUserNormal, depositAmount);

        assertTrue(response.isSuccess(), "Nạp tiền hợp lệ phải thành công");
        BigDecimal newBalance = authService.getUserBalance(testUserNormal);
        assertTrue(initialBalance.add(depositAmount).compareTo(newBalance) == 0, "Số dư tài khoản phải được cộng thêm chính xác");
    }

    /**
     * Test chặn nạp tiền với số tiền âm hoặc bằng 0.
     */
    @Test
    public void testDeposit_InvalidAmount() {
        Assumptions.assumeTrue(dbConnected, "Bỏ qua test vì không có kết nối Database");

        // Trường hợp nạp tiền bằng 0
        AuctionResponse responseZero = authService.deposit(testUserNormal, BigDecimal.ZERO);
        assertFalse(responseZero.isSuccess(), "Nạp 0 VNĐ phải bị từ chối");
        assertTrue(responseZero.getMessage().contains("lớn hơn 0"));

        // Trường hợp nạp tiền âm
        AuctionResponse responseNegative = authService.deposit(testUserNormal, new BigDecimal("-50000"));
        assertFalse(responseNegative.isSuccess(), "Nạp tiền âm phải bị từ chối");
    }

    /**
     * Test chặn nạp tiền vượt quá hạn mức quy định (500.000.000 VNĐ).
     */
    @Test
    public void testDeposit_LimitExceeded() {
        Assumptions.assumeTrue(dbConnected, "Bỏ qua test vì không có kết nối Database");

        AuctionResponse response = authService.deposit(testUserNormal, new BigDecimal("600000000.0"));
        assertFalse(response.isSuccess(), "Nạp vượt quá 500 triệu phải bị từ chối");
        assertTrue(response.getMessage().contains("tối đa 500.000.000"));
    }

    /**
     * Test chặn nạp tiền khi tài khoản bị khóa (LOCKED).
     */
    @Test
    public void testDeposit_LockedUser() {
        Assumptions.assumeTrue(dbConnected, "Bỏ qua test vì không có kết nối Database");

        AuctionResponse response = authService.deposit(testUserLocked, new BigDecimal("100000.0"));
        assertFalse(response.isSuccess(), "Tài khoản bị khóa không được phép nạp tiền");
        assertTrue(response.getMessage().contains("bị khóa"));
    }
}
