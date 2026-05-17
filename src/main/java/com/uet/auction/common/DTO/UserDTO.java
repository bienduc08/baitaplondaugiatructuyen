package com.uet.auction.common.DTO;

import java.io.Serializable;

public class UserDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String username;
    private String role;
    private String message;
    private double balance;
    private String status; // ĐÃ KHÔI PHỤC: Biến trạng thái để Admin khóa/mở khóa tài khoản

    public UserDTO() {}

    public UserDTO(int id, String username, String role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    // ĐÃ KHÔI PHỤC: Getter và Setter cho status
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "UserDTO{id=" + id + ", username='" + username + "', role='" + role +
                "', balance=" + balance + ", status='" + status + "'}";
    }
}