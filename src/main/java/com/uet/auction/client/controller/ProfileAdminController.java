package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.AlertHelper;
import com.uet.auction.client.util.SessionManager;
import com.uet.auction.common.DTO.UserDTO;
import com.uet.auction.common.Request.AuctionRequest;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ProfileAdminController {

    public static ProfileAdminController instance;

    @FXML private Label lblUsername;
    @FXML private Label lblRole;
    @FXML private Label lblBalance;
    @FXML private Label lblHeaderName;
    @FXML private Label lblHeaderEmail;
    @FXML private Label lblHeaderPhoneNumber;
    @FXML private Label lblCurrentTime;
    @FXML private Label lblSystemStatus;
    @FXML public static Runnable onBackAction;
    @FXML private Button btnDeposit;

    @FXML
    public void initialize() {
        instance = this;

        UserDTO user = SessionManager.getCurrentUser();
        if (user != null) {
            // Tên hiển thị chính
            if (lblUsername != null) lblUsername.setText(user.getUsername());
            if (lblRole != null) lblRole.setText("🛡 Quản trị viên");
            if (lblBalance != null)
                lblBalance.setText(String.format("%,.0f VNĐ", user.getBalance()));

            // Góc trên phải: Admin luôn hiển thị cố định
            if (lblHeaderName != null) {
                String fullName = (user.getFullName() != null && !user.getFullName().isEmpty())
                        ? user.getFullName() : user.getUsername();
                lblHeaderName.setText("👤 " + fullName);
            }
            if (lblHeaderEmail != null) {
                String gmail = (user.getGmail() != null && !user.getGmail().isEmpty())
                        ? user.getGmail() : user.getUsername() + "@gmail.com";
                lblHeaderEmail.setText("✉ " + gmail);
            }
            if (lblHeaderPhoneNumber != null) {
                String phone = (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty())
                        ? user.getPhoneNumber() : "Chưa có SĐT";
                lblHeaderPhoneNumber.setText("📞 " + phone);
            }

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
            BigDecimal amount;
            try { amount = new BigDecimal(raw); }
            catch (NumberFormatException e) { AlertHelper.showError("Số tiền không hợp lệ!"); return; }
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                AlertHelper.showError("Số tiền nạp phải lớn hơn 0!");
                return;
            }
            if (amount.compareTo(new BigDecimal("500000000")) > 0) {
                AlertHelper.showError("Mỗi lần nạp tối đa 500.000.000 VNĐ!");
                return;
            }
            if (btnDeposit != null) btnDeposit.setDisable(true);
            SocketClient.sendRequest(new AuctionRequest("DEPOSIT", new Object[]{username, amount}));
        });
    }

    public void handleDepositSuccess(BigDecimal newBalance) {
        Platform.runLater(() -> {
            UserDTO user = SessionManager.getCurrentUser();
            if (user != null) user.setBalance(newBalance);
            updateBalance();
            if (btnDeposit != null) btnDeposit.setDisable(false);
        });
    }

    public void handleDepositFailure() {
        if (btnDeposit != null) btnDeposit.setDisable(false);
    }
    @FXML
    public void onEditProfileClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/uet/auction/view/ProfileEdit.fxml"));
            Node editNode = loader.load();

            // ĐÃ SỬA: Dùng AdminController thay vì UserController
            if (AdminController.instance != null && AdminController.instance.getMainBorderPane() != null) {
                javafx.scene.layout.BorderPane mainPane = AdminController.instance.getMainBorderPane();
                Node previousCenterView = mainPane.getCenter();

                // Cài đặt nút Quay lại trả về view cũ của Admin
                ProfileEditController.onBackAction = () -> {
                    mainPane.setCenter(previousCenterView);
                };

                mainPane.setCenter(editNode);
            }
        } catch (Exception e) {
            e.printStackTrace();
            AlertHelper.showError("Không thể mở trang chỉnh sửa hồ sơ!");
        }
    }
}