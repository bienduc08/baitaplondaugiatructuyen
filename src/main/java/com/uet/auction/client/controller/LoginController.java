package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.SceneManager;
import com.uet.auction.client.util.SessionManager;
import com.uet.auction.common.DTO.UserDTO;
import com.uet.auction.common.Request.AuctionRequest;
import com.uet.auction.common.Response.AuctionResponse;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

public class LoginController {

    public static LoginController instance;

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        instance = this;
    }

    @FXML
    private void handleLoginButton(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        statusLabel.setText("Đang gửi yêu cầu đăng nhập...");

        SocketClient.sendRequest(new AuctionRequest("LOGIN", new Object[]{username, password}));
    }

    public void handleLoginResponse(AuctionResponse res) {
        Platform.runLater(() -> {
            if (res.isSuccess()) {
                try {
                    UserDTO user = (UserDTO) res.getData();
                    SessionManager.setCurrentUser(user);

                    String role = user.getRole();
                    if ("ADMIN".equals(role)) {
                        SceneManager.switchScene("/com/uet/auction/view/Admin.fxml", "Quản trị viên");
                    } else if ("SELLER".equals(role)) {
                        SceneManager.switchScene("/com/uet/auction/view/Seller.fxml", "Người bán");
                    } else {
                        SceneManager.switchScene("/com/uet/auction/view/User.fxml", "Người dùng");
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    statusLabel.setText("Lỗi: Không tìm thấy file giao diện!");
                }
            } else {
                statusLabel.setText("Đăng nhập thất bại: " + res.getMessage());
            }
        });
    }

    @FXML
    private void handleRegisterButton(ActionEvent event) {
        try {
            SceneManager.switchScene("/com/uet/auction/view/Register.fxml", "Đăng ký tài khoản");
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Không thể mở màn hình đăng ký!");
        }
    }
}