package com.uet.auction.common.Response;

import com.uet.auction.common.dto.UserDTO;
import java.io.Serializable;

public class LoginResponse implements Serializable {
    private boolean success;
    private String message;
    private UserDTO user; // Gửi kèm thông tin người dùng nếu đăng nhập thành công

    public LoginResponse(boolean success, String message, UserDTO user) {
        this.success = success;
        this.message = message;
        this.user = user;
    }

    // Getter
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public UserDTO getUser() { return user; }
}