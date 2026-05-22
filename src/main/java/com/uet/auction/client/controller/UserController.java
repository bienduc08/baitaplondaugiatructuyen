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
import java.io.IOException;

public class UserController {
    public static UserController instance;

    private enum ActiveView { HOME, JOINED, PROFILE }

    @FXML private BorderPane mainBorderPane;
    @FXML private Label welcomeLabel;
    @FXML private Label lblBalance;

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
    }

    private void loadView(String fxmlPath, ActiveView view) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node node = loader.load();
            mainBorderPane.setCenter(node);
            activeView = view;
        } catch (IOException e) {
            e.printStackTrace();
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
        ProfileController.onBackAction = () -> mainBorderPane.setCenter(previousView);
        loadView("/com/uet/auction/view/ProfileContent.fxml", ActiveView.PROFILE);
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
                if (JoinedAuctionsController.instance != null)
                    JoinedAuctionsController.instance.reloadJoinedAuctions();
                else
                    onShowUserAuctionsClick();
                break;
            case PROFILE:
                if (ProfileController.instance != null) {
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
}
