package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.AlertHelper;
import com.uet.auction.client.util.SceneManager;
import com.uet.auction.common.Request.AuctionRequest;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class RegisterController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<String> roleComboBox;

    public static RegisterController instance;

    @FXML
    public void initialize() {
        instance = this;
        // Thêm các lựa chọn quyền (Role) vào ComboBox
        if (roleComboBox != null) {
            roleComboBox.getItems().addAll("USER", "SELLER");
            roleComboBox.getSelectionModel().select("USER"); // Mặc định là User
        }
    }

    @FXML
    public void onRegisterButtonClick() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();
        String role = roleComboBox != null ? roleComboBox.getValue() : "USER";

        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            AlertHelper.showError("Vui lòng điền đầy đủ thông tin!");
            return;
        }

        if (!password.equals(confirm)) {
            AlertHelper.showError("Mật khẩu xác nhận không khớp!");
            return;
        }

        Object[] registerData = new Object[]{username, password, role};
        SocketClient.sendRequest(new AuctionRequest("REGISTER", registerData));
    }

    @FXML
    public void onBackToLoginClick() {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        SceneManager.switchScene(stage, "/com/uet/auction/view/Login.fxml");
    }

    // ResponseListener sẽ gọi hàm này
    public void handleRegisterResponse(boolean success, String message) {
        Platform.runLater(() -> {
            if (success) {
                AlertHelper.showInfo("Đăng ký thành công! Hãy đăng nhập.");
                onBackToLoginClick();
            } else {
                AlertHelper.showError("Lỗi đăng ký: " + message);
            }
        });
    }
}