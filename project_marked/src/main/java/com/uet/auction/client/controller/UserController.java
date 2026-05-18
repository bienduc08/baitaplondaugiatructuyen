package com.uet.auction.client.controller;

import com.uet.auction.client.util.SceneManager;
import com.uet.auction.client.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import java.io.IOException;

public class UserController {
    public static UserController instance;

    @FXML private BorderPane mainBorderPane;
    @FXML private Label welcomeLabel;
    @FXML private Label lblBalance;

    @FXML
    public void initialize() {
        instance = this;
        if (SessionManager.getCurrentUser() != null) {
            welcomeLabel.setText("Xin chào, " + SessionManager.getCurrentUsername() + "!");
            lblBalance.setText(String.format("%,.0f VNĐ", SessionManager.getCurrentUser().getBalance()));
        }
        onShowHomeClick();
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node node = loader.load();
            mainBorderPane.setCenter(node);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML public void onShowHomeClick() {
        loadView("/com/uet/auction/view/HomeContent.fxml");
    }

    @FXML public void onShowUserAuctionsClick() {
        loadView("/com/uet/auction/view/UserAuctions.fxml");
    }

    @FXML public void onProfileButtonClick() {
        Node previousView = mainBorderPane.getCenter();
        ProfileController.onBackAction = () -> mainBorderPane.setCenter(previousView);
        loadView("/com/uet/auction/view/ProfileContent.fxml");
    }

    @FXML public void onRefreshButtonClick() { onShowHomeClick(); }

    @FXML public void onLogoutButtonClick() {
        try {
            SessionManager.clearSession();
            SceneManager.switchScene("/com/uet/auction/view/Login.fxml", "Đăng nhập");
        } catch (IOException e) { e.printStackTrace(); }
    }

    public BorderPane getMainBorderPane() {
        return mainBorderPane;
    }
}