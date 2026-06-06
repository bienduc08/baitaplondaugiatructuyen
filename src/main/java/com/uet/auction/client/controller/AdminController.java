package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.AlertHelper;
import com.uet.auction.client.util.SceneManager;
import com.uet.auction.client.util.SessionManager;
import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.common.Request.AuctionRequest;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.util.List;

public class AdminController {

    public static AdminController instance;
    @FXML
    private BorderPane mainBorderPane;
    @FXML
    private Label welcomeLabel; // Đã sửa tên biến khớp với FXML
    @FXML
    private Label lblCountPending;
    @FXML
    private Label lblCountOpen;
    @FXML
    private Label lblCountClosed;
    @FXML
    private Label lblBalance;

    @FXML
    private TableView<ProductDTO> pendingTable;
    @FXML
    private Button btnNotifications;

    // Đã đổi String thành LocalDateTime để chuẩn hóa hiển thị thời gian

    private enum ActiveView { HOME, PENDING,MANAGEMENT, PROFILE }
    private AdminController.ActiveView activeView = AdminController.ActiveView.HOME;

    @FXML
    public void initialize() {
        instance = this;
        if (welcomeLabel != null && SessionManager.getCurrentUsername() != null) {
            welcomeLabel.setText("Xin chào, Admin: " + SessionManager.getCurrentUsername());
        }
        if (lblBalance != null && SessionManager.getCurrentUser() != null) {
            lblBalance.setText(String.format("%,.0f VNĐ", SessionManager.getCurrentUser().getBalance()));

            // Tải số dư mới nhất từ server
            String username = SessionManager.getCurrentUsername();
            if (username != null) {
                SocketClient.sendRequest(new AuctionRequest("GET_USER_BALANCE", username));
            }
        }
        loadPendingProducts();
        onShowHomeClick();
        // Tải số dư mới nhất từ server
        String username = SessionManager.getCurrentUsername();
        if (username != null) {
            SocketClient.sendRequest(new AuctionRequest("GET_USER_BALANCE", username));

            // ---> THÊM DÒNG NÀY: Tải danh sách thông báo <---
            SocketClient.sendRequest(new AuctionRequest("GET_NOTIFICATIONS", username));
        }
    }

    public void updateBalance() {
        if (SessionManager.getCurrentUser() != null && lblBalance != null) {
            lblBalance.setText(String.format("%,.0f VNĐ", SessionManager.getCurrentUser().getBalance()));
        }
    }

    public void loadPendingProducts() {
        SocketClient.sendRequest(new AuctionRequest("GET_ALL_PRODUCTS", null));
    }


    public void updatePendingList(List<ProductDTO> products) {
        Platform.runLater(() -> {
            long pending = products.stream().filter(p -> "PENDING".equals(p.getStatus())).count();
            long open = products.stream().filter(p -> "OPEN".equals(p.getStatus())).count();
            long closed = products.stream().filter(p -> "CLOSED".equals(p.getStatus())).count();
            if (lblCountPending != null) lblCountPending.setText(String.valueOf(pending));
            if (lblCountOpen != null) lblCountOpen.setText(String.valueOf(open));
            if (lblCountClosed != null) lblCountClosed.setText(String.valueOf(closed));
        });
    }

    @FXML
    public void onApproveButtonClick() {
        // Điều hướng sang tab AdminPending để duyệt sản phẩm
        onPendingClick();
    }

    @FXML
    public void onRejectButtonClick() {
        // Điều hướng sang tab AdminPending để từ chối sản phẩm
        onPendingClick();
    }

    @FXML
    public void onRefreshButtonClick() {
        // Tải số dư mới nhất từ server khi nhấn làm mới
        String username = SessionManager.getCurrentUsername();
        if (username != null) {
            SocketClient.sendRequest(new AuctionRequest("GET_USER_BALANCE", username));
        }
        onShowHomeClick();
    }

    @FXML
    public void onLogoutButtonClick() {
        try {
            SessionManager.clearSession();
            SceneManager.switchScene("/com/uet/auction/view/Login.fxml", "Đăng nhập");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public BorderPane getMainBorderPane() {
        return this.mainBorderPane;
    }

    // ------- XỬ LÝ MENU ĐIỀU HƯỚNG -------

    @FXML
    public void onShowHomeClick() {
        loadView("/com/uet/auction/view/HomeContent.fxml", ActiveView.HOME);
    }

    @FXML
    public void onPendingClick() {
        loadView("/com/uet/auction/view/AdminPending.fxml",ActiveView.PENDING);
    }

    @FXML
    public void onUserManageClick() {
        loadView("/com/uet/auction/view/AdminUserManagement.fxml",ActiveView.MANAGEMENT);
    }


    @FXML
    public void onProfileButtonClick() {
        Node previousView = mainBorderPane.getCenter();
        ProfileAdminController.onBackAction = () -> mainBorderPane.setCenter(previousView);
        loadView("/com/uet/auction/view/ProfileAdmin.fxml",ActiveView.PROFILE);
    }

    private void loadView(String fxmlPath, ActiveView view) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node node = loader.load();
            mainBorderPane.setCenter(node);
            activeView = view;
        } catch (IOException e) {
            e.printStackTrace();
            AlertHelper.showError("Không thể tải giao diện! Vui lòng thử lại.");
        }
    }
    @FXML
    private void onShowNotificationsClick() {
        // Lưu lại giao diện hiện tại ở Center trước khi chuyển sang xem thông báo
        javafx.scene.Node previousView = mainBorderPane.getCenter();
        com.uet.auction.client.controller.NotificationListController.onBackAction = () -> mainBorderPane.setCenter(previousView);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/uet/auction/view/NotificationList.fxml"));
            javafx.scene.Node node = loader.load();
            mainBorderPane.setCenter(node);
        } catch (IOException e) {
            e.printStackTrace();
            javafx.application.Platform.runLater(() ->
                    com.uet.auction.client.util.AlertHelper.showError("Không thể tải giao diện thông báo!")
            );
        }
    }

    // Hàm tiện ích để cập nhật số lượng thông báo
    public void updateNotificationCount(int unreadCount) {
        if (unreadCount > 0) {
            btnNotifications.setText("🔔 (" + unreadCount + ")");
            // Có thể thêm đổi màu chữ đậm hơn ở đây
        } else {
            btnNotifications.setText("🔔"); // Ẩn số 0 đi cho đẹp
        }
    }
}