package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.SceneManager;
import com.uet.auction.common.Request.AuctionRequest;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.io.IOException;

public class RegisterController {

    public static RegisterController instance;

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField fullNameField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        instance = this;
        roleComboBox.getItems().addAll("USER", "SELLER");
        roleComboBox.setValue("USER");
    }

    @FXML
    private void handleRegisterAction(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String confirm  = confirmPasswordField.getText().trim();
        String role     = roleComboBox.getValue();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu!");
            return;
        }
        if (!password.equals(confirm)) {
            statusLabel.setText("Mật khẩu xác nhận không khớp!");
            return;
        }
        if (role == null) {
            statusLabel.setText("Vui lòng chọn vai trò!");
            return;
        }

        statusLabel.setText("Đang xử lý...");

        Object[] regData = new Object[]{username, password, role};
        SocketClient.sendRequest(new AuctionRequest("REGISTER", regData));
    }

    public void handleRegisterResponse(boolean success, String message) {
        Platform.runLater(() -> {
            if (success) {
                statusLabel.setStyle("-fx-text-fill: green;");
                statusLabel.setText("Đăng ký thành công! Quay lại đăng nhập.");
            } else {
                statusLabel.setStyle("-fx-text-fill: red;");
                statusLabel.setText("Lỗi: " + message);
            }
        });
    }

    @FXML
    private void backToLogin(ActionEvent event) {
        try {
            SceneManager.switchScene("/com/uet/auction/view/Login.fxml", "Đăng nhập");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}