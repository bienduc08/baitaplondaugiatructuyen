package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.SceneManager;
import com.uet.auction.client.util.SessionManager;
import com.uet.auction.common.Request.AuctionRequest;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

import javafx.scene.control.Button;
import java.io.IOException;

public class UserController {
    public static UserController instance;

    private enum ActiveView { HOME, JOINED, PROFILE }

    @FXML private BorderPane mainBorderPane;
    @FXML private Label welcomeLabel;
    @FXML private Label lblBalance;
    @FXML public static Runnable onBackAction;
    @FXML private Button btnNotifications;

    private ActiveView activeView = ActiveView.HOME;

    @FXML
    public void initialize() {
        instance = this;
        if (SessionManager.getCurrentUser() != null) {
            welcomeLabel.setText("Xin chào, " + SessionManager.getCurrentUsername() + "!");
            lblBalance.setText(String.format("%,.0f VNĐ", SessionManager.getCurrentUser().getBalance()));

            // Tải số dư mới nhất từ server
            String username = SessionManager.getCurrentUsername();
            if (username != null) {
                SocketClient.sendRequest(new AuctionRequest("GET_USER_BALANCE", username));
            }
        }
        onShowHomeClick();
        // Tải số dư mới nhất từ server
        String username = SessionManager.getCurrentUsername();
        if (username != null) {
            SocketClient.sendRequest(new AuctionRequest("GET_USER_BALANCE", username));

            // ---> THÊM DÒNG NÀY: Tải danh sách thông báo <---
            SocketClient.sendRequest(new AuctionRequest("GET_NOTIFICATIONS", username));
        }
    }

    private void loadView(String fxmlPath, ActiveView view) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node node = loader.load();
            mainBorderPane.setCenter(node);
            activeView = view;
        } catch (IOException e) {
            e.printStackTrace();
            javafx.application.Platform.runLater(() ->
                    com.uet.auction.client.util.AlertHelper.showError("Không thể tải giao diện! Vui lòng thử lại.")
            );
        }
    }

    @FXML public void onShowHomeClick() {
        loadView("/com/uet/auction/view/HomeContent.fxml", ActiveView.HOME);
    }

    @FXML public void onShowUserAuctionsClick() {
        loadView("/com/uet/auction/view/UserAuctions.fxml", ActiveView.JOINED);
    }

    @FXML public void onProfileButtonClick() {
        Node previousView = mainBorderPane.getCenter();
        ProfileUserController.onBackAction = () -> mainBorderPane.setCenter(previousView);
        loadView("/com/uet/auction/view/ProfileUser.fxml", ActiveView.PROFILE);
    }

    @FXML
    public void onRefreshButtonClick() {
        // Tải số dư mới nhất từ server khi nhấn làm mới
        String username = SessionManager.getCurrentUsername();
        if (username != null) {
            SocketClient.sendRequest(new AuctionRequest("GET_USER_BALANCE", username));
        }

        switch (activeView) {
            case JOINED:
                if (UserAuctionsController.instance != null)
                    UserAuctionsController.instance.reloadJoinedAuctions();
                else
                    onShowUserAuctionsClick();
                break;
            case PROFILE:
                if (ProfileUserController.instance != null) {
                    if (username != null)
                        SocketClient.sendRequest(new AuctionRequest("GET_MY_BIDS", username));
                } else {
                    onProfileButtonClick();
                }
                break;
            default:
                onShowHomeClick();
                break;
        }
    }

    public void updateBalance() {
        if (SessionManager.getCurrentUser() != null && lblBalance != null) {
            lblBalance.setText(String.format("%,.0f VNĐ", SessionManager.getCurrentUser().getBalance()));
        }
    }

    @FXML public void onLogoutButtonClick() {
        try {
            SessionManager.clearSession();
            SceneManager.switchScene("/com/uet/auction/view/Login.fxml", "Đăng nhập");
        } catch (IOException e) { e.printStackTrace(); }
    }

    public BorderPane getMainBorderPane() {
        return mainBorderPane;
    }

    public void refreshBalance() {
        if (SessionManager.getCurrentUser() != null && lblBalance != null) {
            lblBalance.setText(String.format("%,.0f VNĐ", SessionManager.getCurrentUser().getBalance()));
        }
    }
    // Sự kiện khi click vào nút chuông
    @FXML
    private void onShowNotificationsClick() {
        // 1. Lưu lại giao diện đang hiển thị hiện tại ở giữa màn hình (Center)
        javafx.scene.Node previousView = mainBorderPane.getCenter();

        // 2. Truyền hành động "Quay lại" cho NotificationListController
        // Khi bấm nút Back, nó sẽ lấy giao diện cũ nhét lại vào Center
        com.uet.auction.client.controller.NotificationListController.onBackAction = () -> {
            mainBorderPane.setCenter(previousView);
        };

        // 3. Tải và hiển thị màn hình Thông báo đè lên
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/uet/auction/view/NotificationList.fxml"));
            javafx.scene.Node node = loader.load();
            mainBorderPane.setCenter(node);
        } catch (java.io.IOException e) {
            e.printStackTrace();
            com.uet.auction.client.util.AlertHelper.showError("Không thể tải giao diện thông báo!");
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