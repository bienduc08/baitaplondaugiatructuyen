package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.SceneManager;
import com.uet.auction.client.util.SessionManager;
import com.uet.auction.common.DTO.ProductDTO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.util.List;


public class AdminController {

    public static AdminController instance;
    @FXML private BorderPane mainBorderPane;
    @FXML private Label welcomeLabel;
    @FXML private Label lblBalance;
    @FXML private Label lblCountOpen;
    @FXML private Label lblCountClosed;
    @FXML private Label lblCountPending;
    private final ObservableList<ProductDTO> pendingListData = FXCollections.observableArrayList();


    @FXML
    public void initialize() {
        instance = this;
        if (welcomeLabel != null && SessionManager.getCurrentUsername() != null)
            welcomeLabel.setText("Xin chào, Admin: " + SessionManager.getCurrentUsername());
        if (lblBalance != null && SessionManager.getCurrentUser() != null) {
            lblBalance.setText(String.format("%,.0f VNĐ", SessionManager.getCurrentUser().getBalance()));
        }
        SocketClient.sendRequest(new com.uet.auction.common.Request.AuctionRequest("GET_ALL_PRODUCTS", null));
        onShowHomeClick();
    }
    // Hàm này chỉ đếm số lượng để hiển thị lên 3 cái Label thống kê
    public void updateStatisticsCounts(List<ProductDTO> products) {
        Platform.runLater(() -> {
            long pending = products.stream().filter(p -> "PENDING".equals(p.getStatus())).count();
            long open    = products.stream().filter(p -> "OPEN".equals(p.getStatus())).count();
            long closed  = products.stream().filter(p -> "CLOSED".equals(p.getStatus())).count();

            if (lblCountPending != null) lblCountPending.setText(String.valueOf(pending));
            if (lblCountOpen    != null) lblCountOpen.setText(String.valueOf(open));
            if (lblCountClosed  != null) lblCountClosed.setText(String.valueOf(closed));
        });
    }



    @FXML public void onRefreshButtonClick() {
        onShowHomeClick();
    }

    @FXML public void onLogoutButtonClick() {
        try {
            SessionManager.clearSession();
            SceneManager.switchScene("/com/uet/auction/view/Login.fxml", "Đăng nhập");
        } catch (IOException e) { e.printStackTrace(); }
    }

    public BorderPane getMainBorderPane() {
        return this.mainBorderPane;
    }

    // ------- XỬ LÝ MENU ĐIỀU HƯỚNG -------
    @FXML public void onBackButtonClick() {

    }

    @FXML public void onShowHomeClick() {
        loadView("/com/uet/auction/view/HomeContent.fxml");
    }

    @FXML public void onPendingClick() {
        loadView("/com/uet/auction/view/AdminPending.fxml");
    }

    @FXML public void onUserManageClick() {
        loadView("/com/uet/auction/view/AdminUserManagement.fxml");
    }

    @FXML public void onProfileButtonClick() {
        loadView("/com/uet/auction/view/ProfileContent.fxml");

    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node node = loader.load();
            mainBorderPane.setCenter(node);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}