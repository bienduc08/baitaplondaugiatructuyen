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

    @FXML private TextField fullnameField;
    @FXML private TextField usernameField;
    @FXML private TextField gmailField;
    @FXML private TextField phonenumberField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label statusLabel;
    @FXML private ComboBox<String> roleComboBox;

    @FXML
    public void initialize() {
        instance = this;
        roleComboBox.getItems().addAll("USER", "SELLER");
    }

    @FXML
    private void handleRegisterAction(ActionEvent event) {
        String fullname = fullnameField.getText().trim();
        String username = usernameField.getText().trim();
        String gmail    = gmailField.getText().trim();
        String phonenumber = phonenumberField.getText().trim();
        String password = passwordField.getText().trim();
        String confirm  = confirmPasswordField.getText().trim();

        // 1. Kiểm tra không được để trống bất kỳ trường nào (Bổ sung phonenumber)
        if (fullname.isEmpty() || gmail.isEmpty() || phonenumber.isEmpty() || username.isEmpty() || password.isEmpty()) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Vui lòng nhập đầy đủ tất cả thông tin!");
            return;
        }

        // 2. Kiểm tra định dạng Email cơ bản (Phải có @ và dấu chấm)
        if (!gmail.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Email không hợp lệ (Ví dụ đúng: abc@gmail.com)!");
            return;
        }

        // 3. Kiểm tra định dạng số điện thoại (Bắt đầu bằng 0, tổng cộng 10 chữ số)
        if (!phonenumber.matches("^0\\d{9}$")) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Số điện thoại không hợp lệ (Phải gồm 10 số và bắt đầu bằng 0)!");
            return;
        }

        // 4. Kiểm tra mật khẩu xác nhận
        if (!password.equals(confirm)) {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Mật khẩu xác nhận không khớp!");
            return;
        }

        String selectedRole = roleComboBox.getSelectionModel().getSelectedItem();
        if (selectedRole == null) {
            selectedRole = "USER"; // Fallback an toàn
        }

        statusLabel.setStyle("-fx-text-fill: black;");
        statusLabel.setText("Đang xử lý...");

        // 5. NÂNG CẤP MẢNG DỮ LIỆU: Bổ sung phonenumber vào gói tin
        Object[] regData = new Object[]{fullname,username, gmail, phonenumber, password, selectedRole};
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
