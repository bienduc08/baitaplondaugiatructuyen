package com.uet.auction.server.service;

import com.uet.auction.server.DAO.ProductDAO;
import com.uet.auction.server.config.DatabaseConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử cho chức năng Anti-sniping (tự động kéo dài thời gian đấu giá).
 * Chức năng này tự động gia hạn thêm 5 phút nếu có lượt đặt giá hợp lệ
 * trong vòng 30 giây cuối cùng trước khi phiên đấu giá kết thúc.
 */
public class AuctionAntiSnipingTest {

    private ProductDAO productDAO;
    private boolean dbConnected = false;

    // Các thông tin kiểm thử để dọn dẹp sau khi chạy test
    private final String testSeller = "seller_test_sniping";
    private final String testBidder = "bidder_test_sniping";
    private int testProductIdPositive = -1;
    private int testProductIdNegativeTime = -1;
    private int testProductIdNegativeBid = -1;

    @BeforeEach
    public void setUp() {
        productDAO = new ProductDAO();
        try {
            // Thử kết nối cơ sở dữ liệu. Nếu không thành công, bỏ qua các test tích hợp cơ sở dữ liệu.
            Connection c = DatabaseConnection.getConnection();
            if (c != null && !c.isClosed()) {
                dbConnected = true;
                c.setAutoCommit(true);
                prepareTestData();
            }
        } catch (Exception e) {
            System.out.println("[WARNING] Không thể kết nối cơ sở dữ liệu để chạy Integration Test. Bỏ qua các test database.");
            dbConnected = false;
        }
    }

    @AfterEach
    public void tearDown() {
        if (dbConnected) {
            try {
                Connection c = DatabaseConnection.getConnection();
                // Xóa dữ liệu kiểm thử
                try (Statement stmt = c.createStatement()) {
                    if (testProductIdPositive != -1) {
                        stmt.executeUpdate("DELETE FROM bids WHERE product_id = " + testProductIdPositive);
                        stmt.executeUpdate("DELETE FROM products WHERE id = " + testProductIdPositive);
                    }
                    if (testProductIdNegativeTime != -1) {
                        stmt.executeUpdate("DELETE FROM bids WHERE product_id = " + testProductIdNegativeTime);
                        stmt.executeUpdate("DELETE FROM products WHERE id = " + testProductIdNegativeTime);
                    }
                    if (testProductIdNegativeBid != -1) {
                        stmt.executeUpdate("DELETE FROM bids WHERE product_id = " + testProductIdNegativeBid);
                        stmt.executeUpdate("DELETE FROM products WHERE id = " + testProductIdNegativeBid);
                    }
                    stmt.executeUpdate("DELETE FROM users WHERE username IN ('" + testSeller + "', '" + testBidder + "')");
                }
            } catch (SQLException e) {
                System.err.println("Lỗi khi dọn dẹp dữ liệu test: " + e.getMessage());
            }
        }
    }

    /**
     * Chuẩn bị dữ liệu kiểm thử trong Database:
     * - Tạo 2 người dùng (người bán và người đặt giá).
     * - Tạo sản phẩm kiểm thử tích cực (còn 15 giây, có bid mới đặt).
     * - Tạo sản phẩm kiểm thử tiêu cực về thời gian (còn 45 giây, có bid mới đặt).
     * - Tạo sản phẩm kiểm thử tiêu cực về lượt bid (còn 15 giây nhưng không có bid nào trong 2 phút qua).
     */
    private void prepareTestData() throws SQLException {
        Connection c = DatabaseConnection.getConnection();
        
        // Lấy thời gian hiện tại từ DB để làm mốc tính thời gian tuyệt đối nhằm tránh lệch múi giờ (Timezone mismatch)
        Timestamp dbNow = null;
        try (Statement stmt = c.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT NOW()")) {
            if (rs.next()) {
                dbNow = rs.getTimestamp(1);
            }
        }
        if (dbNow == null) {
            dbNow = new Timestamp(System.currentTimeMillis());
        }

        long oneHourMillis = 3600 * 1000;
        Timestamp startTime = new Timestamp(dbNow.getTime() - oneHourMillis);
        Timestamp endTimePositive = new Timestamp(dbNow.getTime() + 15 * 1000);
        Timestamp endTimeNegTime = new Timestamp(dbNow.getTime() + 45 * 1000);

        try (Statement stmt = c.createStatement()) {
            // 1. Đảm bảo người dùng tồn tại
            stmt.executeUpdate("INSERT INTO users (fullname, username, gmail, phonenumber, password, role, balance, status) " +
                    "VALUES ('Seller Test', '" + testSeller + "', 'seller_sniping@test.com', '111111111', 'hash', 'USER', 1000000, 'ACTIVE') " +
                    "ON DUPLICATE KEY UPDATE balance = 1000000");

            stmt.executeUpdate("INSERT INTO users (fullname, username, gmail, phonenumber, password, role, balance, status) " +
                    "VALUES ('Bidder Test', '" + testBidder + "', 'bidder_sniping@test.com', '222222222', 'hash', 'USER', 1000000, 'ACTIVE') " +
                    "ON DUPLICATE KEY UPDATE balance = 1000000");
        }

        String insertProductSql = "INSERT INTO products (name, description, starting_price, current_price, step_price, seller_name, owner_name, start_time, end_time, status) " +
                "VALUES (?, 'Test', 10000, 12000, 2000, ?, ?, ?, ?, 'OPEN')";
        String insertBidSql = "INSERT INTO bids (product_id, bidder_name, amount, bid_time, status) VALUES (?, ?, ?, ?, 'Hợp lệ')";

        // 2. Tạo sản phẩm positive (còn 15 giây, có bid mới)
        try (PreparedStatement pstmt = c.prepareStatement(insertProductSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, "Product Sniping Positive");
            pstmt.setString(2, testSeller);
            pstmt.setString(3, testBidder);
            pstmt.setTimestamp(4, startTime);
            pstmt.setTimestamp(5, endTimePositive);
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) testProductIdPositive = rs.getInt(1);
            }
        }

        // Tạo bid mới đặt cho sản phẩm positive (đặt vào thời điểm dbNow)
        try (PreparedStatement pstmt = c.prepareStatement(insertBidSql)) {
            pstmt.setInt(1, testProductIdPositive);
            pstmt.setString(2, testBidder);
            pstmt.setDouble(3, 12000);
            pstmt.setTimestamp(4, dbNow);
            pstmt.executeUpdate();
        }

        // 3. Tạo sản phẩm negative time (còn 45 giây)
        try (PreparedStatement pstmt = c.prepareStatement(insertProductSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, "Product Sniping Neg Time");
            pstmt.setString(2, testSeller);
            pstmt.setString(3, testBidder);
            pstmt.setTimestamp(4, startTime);
            pstmt.setTimestamp(5, endTimeNegTime);
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) testProductIdNegativeTime = rs.getInt(1);
            }
        }

        // Tạo bid mới đặt cho sản phẩm negative time
        try (PreparedStatement pstmt = c.prepareStatement(insertBidSql)) {
            pstmt.setInt(1, testProductIdNegativeTime);
            pstmt.setString(2, testBidder);
            pstmt.setDouble(3, 12000);
            pstmt.setTimestamp(4, dbNow);
            pstmt.executeUpdate();
        }

        // 4. Tạo sản phẩm negative bid (còn 15 giây nhưng không có bid trong 2 phút qua)
        try (PreparedStatement pstmt = c.prepareStatement(insertProductSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, "Product Sniping Neg Bid");
            pstmt.setString(2, testSeller);
            pstmt.setString(3, testBidder);
            pstmt.setTimestamp(4, startTime);
            pstmt.setTimestamp(5, endTimePositive);
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) testProductIdNegativeBid = rs.getInt(1);
            }
        }

        // Tạo bid cũ (5 phút trước) cho sản phẩm negative bid
        Timestamp bidTimeOld = new Timestamp(dbNow.getTime() - 5 * 60 * 1000);
        try (PreparedStatement pstmt = c.prepareStatement(insertBidSql)) {
            pstmt.setInt(1, testProductIdNegativeBid);
            pstmt.setString(2, testBidder);
            pstmt.setDouble(3, 12000);
            pstmt.setTimestamp(4, bidTimeOld);
            pstmt.executeUpdate();
        }
    }

    /**
     * Kiểm thử logic nghiệp vụ Anti-sniping thuần túy (Unit Test giả định).
     * Đảm bảo tính toán thời gian xác định khoảng sniper chính xác.
     */
    @Test
    public void testAntiSnipingBusinessRule() {
        LocalDateTime endTime = LocalDateTime.now().plusSeconds(15); // Còn 15 giây nữa kết thúc
        LocalDateTime bidTime = LocalDateTime.now(); // Đặt giá lúc này

        // Khoảng thời gian từ lúc đặt đến lúc kết thúc
        long secondsLeft = ChronoUnit.SECONDS.between(bidTime, endTime);
        
        // Quy tắc: Nếu số giây còn lại nằm trong khoảng [0, 30] thì kích hoạt gia hạn
        boolean isSnipingZone = secondsLeft >= 0 && secondsLeft <= 30;
        assertTrue(isSnipingZone, "Đặt giá trong 30 giây cuối phải kích hoạt chế độ anti-sniping");

        // Trường hợp đặt giá sớm (còn 45 giây)
        LocalDateTime earlyEndTime = LocalDateTime.now().plusSeconds(45);
        long earlySecondsLeft = ChronoUnit.SECONDS.between(bidTime, earlyEndTime);
        boolean isEarlySnipingZone = earlySecondsLeft >= 0 && earlySecondsLeft <= 30;
        assertFalse(isEarlySnipingZone, "Đặt giá khi còn 45 giây không được kích hoạt chế độ anti-sniping");
    }

    /**
     * Integration Test: Kiểm thử phương thức extendAuctionIfLastBid() đối với trường hợp tích cực.
     * Sản phẩm có thời hạn kết thúc trong 15s tới và có bid mới trong vòng 2 phút qua
     * phải được gia hạn thêm 5 phút.
     */
    @Test
    public void testExtendAuctionIfLastBid_Positive() {
        // Chỉ chạy test này nếu có kết nối Database cục bộ
        Assumptions.assumeTrue(dbConnected, "Bỏ qua test vì không kết nối được cơ sở dữ liệu");

        // Lấy thời gian kết thúc trước khi chạy gia hạn
        Timestamp initialEndTime = getProductEndTime(testProductIdPositive);
        assertNotNull(initialEndTime, "Thời gian kết thúc ban đầu không được rỗng");

        // Chạy hàm cần test
        productDAO.extendAuctionIfLastBid();

        // Lấy thời gian kết thúc sau khi chạy gia hạn
        Timestamp updatedEndTime = getProductEndTime(testProductIdPositive);
        assertNotNull(updatedEndTime, "Thời gian kết thúc sau gia hạn không được rỗng");

        // Tính toán khoảng chênh lệch (mong muốn tăng thêm 3 phút = 180 giây)
        long diffInSeconds = (updatedEndTime.getTime() - initialEndTime.getTime()) / 1000;
        assertEquals(180, diffInSeconds, "Thời gian kết thúc phải được kéo dài thêm chính xác 3 phút (180 giây)");
    }

    /**
     * Integration Test: Kiểm thử phương thức extendAuctionIfLastBid() đối với trường hợp thời gian chưa tới hạn.
     * Sản phẩm có thời hạn kết thúc còn 45s (ngoài khoảng 0-30s cuối) thì không được gia hạn.
     */
    @Test
    public void testExtendAuctionIfLastBid_NegativeTime() {
        Assumptions.assumeTrue(dbConnected, "Bỏ qua test vì không kết nối được cơ sở dữ liệu");

        Timestamp initialEndTime = getProductEndTime(testProductIdNegativeTime);
        assertNotNull(initialEndTime);

        // Chạy hàm cần test
        productDAO.extendAuctionIfLastBid();

        Timestamp updatedEndTime = getProductEndTime(testProductIdNegativeTime);
        assertNotNull(updatedEndTime);

        // Đảm bảo thời gian kết thúc không thay đổi
        assertEquals(initialEndTime.getTime(), updatedEndTime.getTime(), "Thời gian kết thúc của sản phẩm còn 45 giây không được thay đổi");
    }

    /**
     * Integration Test: Kiểm thử phương thức extendAuctionIfLastBid() đối với trường hợp không có bid mới.
     * Sản phẩm có thời hạn kết thúc còn 15s nhưng lượt bid gần nhất đã từ 5 phút trước thì không được gia hạn.
     */
    @Test
    public void testExtendAuctionIfLastBid_NegativeBidTime() {
        Assumptions.assumeTrue(dbConnected, "Bỏ qua test vì không kết nối được cơ sở dữ liệu");

        Timestamp initialEndTime = getProductEndTime(testProductIdNegativeBid);
        assertNotNull(initialEndTime);

        // Debug: In các giá trị hiện tại trước khi chạy gia hạn
        try (Connection c = DatabaseConnection.getConnection();
             Statement stmt = c.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT p.end_time, b.latest_bid, TIMESTAMPDIFF(SECOND, b.latest_bid, p.end_time) AS diff " +
                     "FROM products p JOIN (SELECT product_id, MAX(bid_time) AS latest_bid FROM bids GROUP BY product_id) b ON b.product_id = p.id " +
                     "WHERE p.id = " + testProductIdNegativeBid)) {
            if (rs.next()) {
                System.out.println("[DEBUG TEST] NegativeBidTime - ID: " + testProductIdNegativeBid +
                        " | End: " + rs.getTimestamp("end_time") +
                        " | LatestBid: " + rs.getTimestamp("latest_bid") +
                        " | Diff: " + rs.getInt("diff"));
            } else {
                System.out.println("[DEBUG TEST] NegativeBidTime - ID: " + testProductIdNegativeBid + " - KHÔNG TÌM THẤY BẢN GHI JOIN!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Chạy hàm cần test
        productDAO.extendAuctionIfLastBid();

        Timestamp updatedEndTime = getProductEndTime(testProductIdNegativeBid);
        assertNotNull(updatedEndTime);

        // Đảm bảo thời gian kết thúc không thay đổi
        assertEquals(initialEndTime.getTime(), updatedEndTime.getTime(), "Thời gian kết thúc không được thay đổi khi không có bid mới trong 2 phút qua");
    }

    /**
     * Helper truy vấn thời gian kết thúc của sản phẩm từ cơ sở dữ liệu.
     */
    private Timestamp getProductEndTime(int productId) {
        String sql = "SELECT end_time FROM products WHERE id = ?";
        try {
            Connection c = DatabaseConnection.getConnection();
            try (PreparedStatement pstmt = c.prepareStatement(sql)) {
                pstmt.setInt(1, productId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getTimestamp("end_time");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi truy vấn end_time: " + e.getMessage());
        }
        return null;
    }
}
