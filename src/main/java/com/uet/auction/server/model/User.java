package com.uet.auction.server.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List; // Thêm import List

public class User extends Entity implements Serializable {
    private String username;
    private String fullname;
    private String gmail;
    private String phonenumber;
    private String password;
    private BigDecimal balance;
    private UserRole role;

    // Thêm thuộc tính danh sách thông báo
    private List<String> notifications = new ArrayList<>();

    public User() {
        super();
    }

    public User(int id, LocalDateTime createdAt, String username, String password,
                BigDecimal balance, UserRole role) {
        super(id, createdAt);
        this.username = username;
        this.password = password;
        this.balance = balance;
        this.role = role;
    }

    public User(String username, UserRole role) {
        super();
        this.username = username;
        this.role = role;
    }

    // Viết nốt hàm xử lý thông báo
    public void addNotification(String message) {
        if (message != null && !message.trim().isEmpty()) {
            this.notifications.add(message);
            System.out.println("🔔 [" + this.username + "] Có thông báo mới: " + message);

            // LƯU Ý: Đây chỉ là lưu trên RAM của object User hiện tại.
            // Để thông báo tồn tại vĩnh viễn, ở tầng Service bạn cần gọi thêm:
            // notificationDAO.save(this.getId(), message);
        }
    }

    // Thêm getter để JavaFX có thể lấy danh sách thông báo ra hiển thị lên UI
    public List<String> getNotifications() {
        return notifications;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullname() { return fullname; }
    public void setFullname(String fullname) { this.fullname = fullname; }

    public String getGmail() { return gmail; }
    public void setGmail(String gmail) { this.gmail = gmail; }

    public String getPhonenumber() { return phonenumber; }
    public void setPhonenumber(String phonenumber) {
        this.phonenumber = phonenumber;
    }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    @Override
    public void printInfo() {
        System.out.println("[USER] " + username + " | Vai trò: " + role + " | Số dư: " + balance);
    }
}