package com.uet.auction.server.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class UserTest {

    @Test
    public void testDefaultConstructor() {
        User user = new User();
        assertNotNull(user);
        assertNull(user.getUsername());
        assertNull(user.getRole());
        assertEquals(0.0, user.getBalance());
        assertNotNull(user.getNotifications());
        assertTrue(user.getNotifications().isEmpty());
    }

    @Test
    public void testParameterizedConstructor() {
        LocalDateTime now = LocalDateTime.now();
        User user = new User(101, now, "john_doe", "password123", 1500000.0, UserRole.BIDDER);

        assertEquals(101, user.getId());
        assertEquals(now, user.getCreatedAt());
        assertEquals("john_doe", user.getUsername());
        assertEquals("password123", user.getPassword());
        assertEquals(1500000.0, user.getBalance());
        assertEquals(UserRole.BIDDER, user.getRole());
    }

    @Test
    public void testUsernameRoleConstructor() {
        User user = new User("admin_user", UserRole.ADMIN);
        assertEquals("admin_user", user.getUsername());
        assertEquals(UserRole.ADMIN, user.getRole());
        assertNotNull(user.getNotifications());
    }

    @Test
    public void testSettersAndGetters() {
        User user = new User();
        user.setUsername("alice");
        user.setFullname("Alice Nguyen");
        user.setGmail("alice@gmail.com");
        user.setPhonenumber("0987654321");
        user.setPassword("securepass");
        user.setBalance(250000.0);
        user.setRole(UserRole.SELLER);

        assertEquals("alice", user.getUsername());
        assertEquals("Alice Nguyen", user.getFullname());
        assertEquals("alice@gmail.com", user.getGmail());
        assertEquals("0987654321", user.getPhonenumber());
        assertEquals("securepass", user.getPassword());
        assertEquals(250000.0, user.getBalance());
        assertEquals(UserRole.SELLER, user.getRole());
    }

    @Test
    public void testAddNotificationValid() {
        User user = new User("test_user", UserRole.BIDDER);
        user.addNotification("Phiên đấu giá của bạn đã kết thúc!");

        List<String> notifications = user.getNotifications();
        assertEquals(1, notifications.size());
        assertEquals("Phiên đấu giá của bạn đã kết thúc!", notifications.get(0));
    }

    @Test
    public void testAddNotificationInvalid() {
        User user = new User("test_user", UserRole.BIDDER);

        // Test with null message
        user.addNotification(null);
        assertTrue(user.getNotifications().isEmpty());

        // Test with empty/blank message
        user.addNotification("   ");
        assertTrue(user.getNotifications().isEmpty());
    }
}
