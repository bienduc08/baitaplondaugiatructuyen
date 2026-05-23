package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.AlertHelper;
import com.uet.auction.client.util.SceneManager;
import com.uet.auction.client.util.SessionManager;
import com.uet.auction.common.DTO.UserDTO;
import com.uet.auction.common.Request.AuctionRequest;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ProfileAdminController {

    public static ProfileAdminController instance;

    @FXML private Label lblUsername;
    @FXML private Label lblRole;
    @FXML private Label lblBalance;
    @FXML private Label lblHeaderName;
    @FXML private Label lblHeaderEmail;
    @FXML private Label lblCurrentTime;
    @FXML private Label lblSystemStatus;

    @FXML
    public void initialize() {
        instance = this;

        UserDTO user = SessionManager.getCurrentUser();
        if (user != null) {
            // Tên hiển thị chính
            String displayName = (user.getFullName() != null && !user.getFullName().isEmpty())
                    ? user.getFullName() : user.getUsername();
            if (lblUsername != null) lblUsername.setText(displayName);
            if (lblRole != null) lblRole.setText("🛡 Quản trị viên");
            if (lblBalance != null)
                lblBalance.setText(String.format("%,.0f VNĐ", user.getBalance()));

            // Góc trên phải: Admin luôn hiển thị cố định
            if (lblHeaderName != null) lblHeaderName.setText("👤 Admin");
            if (lblHeaderEmail != null) lblHeaderEmail.setText("✉ admin@gmail.com");

            // Tải số dư mới nhất từ server
            String username = SessionManager.getCurrentUsername();
            if (username != null) {
                SocketClient.sendRequest(new AuctionRequest("GET_USER_BALANCE", username));
            }
        }

        // Cập nhật thời gian hiện tại
        if (lblCurrentTime != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            lblCurrentTime.setText("🕐 " + LocalDateTime.now().format(fmt));
        }

        if (lblSystemStatus != null) {
            lblSystemStatus.setText("🟢 Hệ thống hoạt động bình thường");
        }
    }

    public void updateBalance() {
        UserDTO user = SessionManager.getCurrentUser();
        if (user != null && lblBalance != null) {
            lblBalance.setText(String.format("%,.0f VNĐ", user.getBalance()));
        }
    }

    @FXML
    public void onDepositClick() {
        String username = SessionManager.getCurrentUsername();
        if (username == null) {
            AlertHelper.showError("Phiên đăng nhập không hợp lệ!");
            return;
        }

        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
        dialog.setTitle("Nạp tiền");
        dialog.setHeaderText("Nạp tiền vào ví Admin");
        dialog.setContentText("Nhập số tiền (VNĐ):");

        dialog.showAndWait().ifPresent(input -> {
            String raw = input.trim().replace(",", "").replace(".", "");
            if (raw.isEmpty()) { AlertHelper.showError("Vui lòng nhập số tiền!"); return; }
            double amount;
            try { amount = Double.parseDouble(raw); }
            catch (NumberFormatException e) { AlertHelper.showError("Số tiền không hợp lệ!"); return; }
            if (amount <= 0) { AlertHelper.showError("Số tiền nạp phải lớn hơn 0!"); return; }
            if (amount > 500_000_000) { AlertHelper.showError("Mỗi lần nạp tối đa 500.000.000 VNĐ!"); return; }
            SocketClient.sendRequest(new AuctionRequest("DEPOSIT", new Object[]{username, amount}));
        });
    }

    public void handleDepositSuccess(double newBalance) {
        Platform.runLater(() -> {
            UserDTO user = SessionManager.getCurrentUser();
            if (user != null) user.setBalance(newBalance);
            updateBalance();
        });
    }

    @FXML
    public void onLogoutButtonClick() {
        try {
            SessionManager.clearSession();
            SceneManager.switchScene("/com/uet/auction/view/Login.fxml", "Đăng nhập");
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    public void onBackButtonClick() {
        try {
            SceneManager.switchScene("/com/uet/auction/view/Admin.fxml", "Quản trị viên");
        } catch (IOException e) { e.printStackTrace(); }
    }
}
