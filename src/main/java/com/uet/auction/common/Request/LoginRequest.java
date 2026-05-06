package com.uet.auction.common.Request;

import java.io.Serializable;

// Dùng Serializable để có thể truyền đối tượng qua luồng (Stream)
public class LoginRequest implements Serializable {
    private String username;
    private String password;

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getter và Setter
    public String getUsername() { return username; }
    public String getPassword() { return password; }
}