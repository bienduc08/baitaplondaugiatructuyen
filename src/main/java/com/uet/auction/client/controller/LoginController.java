package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.SceneManager;
import com.uet.auction.client.util.SessionManager;
import com.uet.auction.common.DTO.UserDTO;
import com.uet.auction.common.Request.AuctionRequest;
import com.uet.auction.common.Request.LoginRequest;
import com.uet.auction.common.Response.AuctionResponse;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    // Instance tĩnh để ResponseListener gọi lại
    public static LoginController instance;

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        instance = this;
    }

    /**
     * Xử lý khi nhấn nút Đăng nhập — GỬI request lên server
     */
    @FXML
    private void handleLoginButton(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        statusLabel.setText("Đang gửi yêu cầu đăng nhập...");

        // SỬA: thực sự gửi request lên server thay vì chỉ System.out.println
        LoginRequest loginReq = new LoginRequest(username, password);
        SocketClient.sendRequest(new AuctionRequest("LOGIN", loginReq));
    }

    /**
     * Nhận phản hồi từ ResponseListener (gọi từ luồng mạng)
     */
    public void handleLoginResponse(AuctionResponse res) {
        Platform.runLater(() -> {
            if (res.isSuccess()) {
                try {
                    // SỬA: lưu thông tin user vào session
                    UserDTO user = (UserDTO) res.getData();
                    SessionManager.setCurrentUser(user);

                    // SỬA: phân quyền theo role thay vì load cứng MainView.fxml
                    String role = user.getRole();
                    if ("ADMIN".equals(role)) {
                        SceneManager.switchScene("/com/uet/auction/view/Admin.fxml", "Quản trị viên");
                    } else if ("SELLER".equals(role)) {
                        SceneManager.switchScene("/com/uet/auction/view/Seller.fxml", "Người bán");
                    } else {
                        SceneManager.switchScene("/com/uet/auction/view/User.fxml", "Trang chủ");
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

    /**
     * Chuyển sang màn hình Đăng ký
     */
    @FXML
    private void handleRegisterButton(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/uet/auction/view/RegisterView.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Đăng ký tài khoản");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Không thể mở màn hình đăng ký!");
        }
    }
}