package com.uet.auction.server.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử đơn vị cho lớp mô hình User (Người dùng).
 * Đảm bảo các thông tin của người dùng được khởi tạo và cập nhật chính xác,
 * đồng thời kiểm tra logic xử lý thông báo trên RAM.
 */
public class UserTest {

    /**
     * Kiểm thử Constructor mặc định (không tham số).
     * Đảm bảo đối tượng được sinh ra có các giá trị mặc định hợp lệ.
     */
    @Test
    public void testDefaultConstructor() {
        User user = new User();
        assertNotNull(user, "Đối tượng User không được null");
        assertNull(user.getUsername(), "Username mặc định phải là null");
        assertNull(user.getRole(), "Role mặc định phải là null");
        assertNull(user.getBalance(), "Số dư mặc định phải là null");
        assertNotNull(user.getNotifications(), "Danh sách thông báo phải được khởi tạo");
        assertTrue(user.getNotifications().isEmpty(), "Danh sách thông báo ban đầu phải rỗng");
    }

    /**
     * Kiểm thử Constructor có đầy đủ tham số.
     * Xác minh các giá trị truyền vào constructor được gán chính xác cho các thuộc tính.
     */
    @Test
    public void testParameterizedConstructor() {
        LocalDateTime now = LocalDateTime.now();
        // Khởi tạo User với ID, thời gian tạo, tên đăng nhập, mật khẩu, số dư, và vai trò
        User user = new User(101, now, "john_doe", "password123", new BigDecimal("1500000.0"), UserRole.BIDDER);

        assertEquals(101, user.getId(), "ID phải khớp với giá trị truyền vào");
        assertEquals(now, user.getCreatedAt(), "Thời gian tạo phải khớp với giá trị truyền vào");
        assertEquals("john_doe", user.getUsername(), "Username phải khớp");
        assertEquals("password123", user.getPassword(), "Mật khẩu phải khớp");
        assertEquals(new BigDecimal("1500000.0"), user.getBalance(), "Số dư phải khớp");
        assertEquals(UserRole.BIDDER, user.getRole(), "Vai trò phải là BIDDER");
    }

    /**
     * Kiểm thử Constructor rút gọn (chỉ truyền Username và Vai trò).
     * Phù hợp cho việc tạo nhanh đối tượng User để xử lý nghiệp vụ cơ bản.
     */
    @Test
    public void testUsernameRoleConstructor() {
        User user = new User("admin_user", UserRole.ADMIN);
        assertEquals("admin_user", user.getUsername(), "Username phải khớp");
        assertEquals(UserRole.ADMIN, user.getRole(), "Vai trò phải là ADMIN");
        assertNotNull(user.getNotifications(), "Danh sách thông báo vẫn phải được khởi tạo");
    }

    /**
     * Kiểm thử các hàm Getter và Setter.
     * Đảm bảo việc thay đổi thông tin người dùng qua hàm set và lấy ra qua hàm get hoạt động đúng.
     */
    @Test
    public void testSettersAndGetters() {
        User user = new User();
        user.setUsername("alice");
        user.setFullname("Alice Nguyen");
        user.setGmail("alice@gmail.com");
        user.setPhonenumber("0987654321");
        user.setPassword("securepass");
        user.setBalance(new BigDecimal("250000.0"));
        user.setRole(UserRole.SELLER);

        assertEquals("alice", user.getUsername());
        assertEquals("Alice Nguyen", user.getFullname());
        assertEquals("alice@gmail.com", user.getGmail());
        assertEquals("0987654321", user.getPhonenumber());
        assertEquals("securepass", user.getPassword());
        assertEquals(new BigDecimal("250000.0"), user.getBalance());
        assertEquals(UserRole.SELLER, user.getRole());
    }

    /**
     * Kiểm thử trường hợp thêm thông báo hợp lệ.
     * Đảm bảo thông báo được chèn thành công vào danh sách và có thể lấy ra để hiển thị.
     */
    @Test
    public void testAddNotificationValid() {
        User user = new User("test_user", UserRole.BIDDER);
        user.addNotification("Phiên đấu giá của bạn đã kết thúc!");

        List<String> notifications = user.getNotifications();
        assertEquals(1, notifications.size(), "Danh sách phải chứa đúng 1 thông báo");
        assertEquals("Phiên đấu giá của bạn đã kết thúc!", notifications.get(0), "Nội dung thông báo phải khớp");
    }

    /**
     * Kiểm thử trường hợp thêm thông báo không hợp lệ (chuỗi null hoặc rỗng).
     * Đảm bảo hệ thống tự động lọc bỏ và không lưu các thông báo không có nội dung.
     */
    @Test
    public void testAddNotificationInvalid() {
        User user = new User("test_user", UserRole.BIDDER);

        // Trường hợp 1: Thêm thông báo là null
        user.addNotification(null);
        assertTrue(user.getNotifications().isEmpty(), "Không được lưu thông báo null");

        // Trường hợp 2: Thêm thông báo là chuỗi rỗng / toàn khoảng trắng
        user.addNotification("   ");
        assertTrue(user.getNotifications().isEmpty(), "Không được lưu thông báo chỉ chứa khoảng trắng");
    }
}
