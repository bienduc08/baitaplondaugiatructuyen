package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.AlertHelper;
import com.uet.auction.client.util.SceneManager;
import com.uet.auction.common.Request.AuctionRequest;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class RegisterController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;

    public static RegisterController instance;

    public void initialize() {
        instance = this;
    }

    @FXML
    public void onRegisterButtonClick() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            AlertHelper.showError("Vui lòng nhập đủ thông tin!");
            return;
        }
        if (!password.equals(confirm)) {
            AlertHelper.showError("Mật khẩu xác nhận không khớp!");
            return;
        }

        // Gửi mảng String chứa user và pass
        String[] regData = {username, password};
        SocketClient.sendRequest(new AuctionRequest("REGISTER", regData));
    }

    public void handleRegisterResponse(boolean success, String message) {
        Platform.runLater(() -> {
            if (success) {
                AlertHelper.showInfo("Đăng ký thành công! Vui lòng đăng nhập.");
                goToLogin();
            } else {
                AlertHelper.showError(message);
            }
        });
    }

    @FXML
    public void goToLogin() {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        SceneManager.switchScene(stage, "/com/uet/auction/view/Login.fxml");
    }
}