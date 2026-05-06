package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.common.Request.AuctionRequest;
import com.uet.auction.common.Request.LoginRequest;
import com.uet.auction.client.util.AlertHelper;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    // Tạo 1 biến tĩnh để ResponseListener có thể gọi lại
    public static LoginController instance;

    public void initialize() {
        instance = this;
    }

    @FXML
    public void onLoginButtonClick() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            AlertHelper.showError("Vui lòng nhập đủ thông tin!");
            return;
        }

        LoginRequest loginData = new LoginRequest(username, password);
        SocketClient.sendRequest(new AuctionRequest("LOGIN", loginData));
        // Gửi xong là thôi, đợi ResponseListener xử lý kết quả
    }

    // Hàm này sẽ được ResponseListener gọi khi Server trả về kết quả Đăng nhập
    public void handleLoginResponse(boolean success, String message) {
        Platform.runLater(() -> {
            if (success) {
                AlertHelper.showInfo("Đăng nhập thành công!");
                goToUserScreen();
            } else {
                AlertHelper.showError(message);
            }
        });
    }

    private void goToUserScreen() {
        try {
            Stage stage = (Stage) usernameField.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/uet/auction/view/User.fxml"));
            stage.setScene(new Scene(loader.load()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}