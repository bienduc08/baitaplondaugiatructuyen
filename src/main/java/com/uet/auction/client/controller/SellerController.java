package com.uet.auction.client.controller;

import com.uet.auction.client.util.SceneManager;
import com.uet.auction.client.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import java.io.IOException;

public class SellerController {
    public static SellerController instance;
    @FXML private BorderPane mainBorderPane;

    @FXML
    public void initialize() {
        instance = this;
        onShowMyProductsClick(); // Mặc định mở danh sách sản phẩm của Seller
    }

    private void loadView(String fxmlPath) {
        try {
            // ĐÃ SỬA: Thay vì load chết file Seller.fxml gây tràn bộ nhớ,
            // giờ đây loader sẽ gọi đúng file fxml được truyền vào tham số fxmlPath.
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            mainBorderPane.setCenter(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML public void onShowHomeClick()       { loadView("/com/uet/auction/view/HomeContent.fxml"); }
    @FXML public void onShowAddFormClick()    { loadView("/com/uet/auction/view/CreateProductContent.fxml"); }
    @FXML public void onShowMyProductsClick() { loadView("/com/uet/auction/view/MyAuctionsContent.fxml"); }

    @FXML public void loadMyProducts() { onShowMyProductsClick(); /* Nút refresh */ }

    @FXML public void onProfileButtonClick() {
        Node previousView = mainBorderPane.getCenter();
        ProfileController.onBackAction = () -> mainBorderPane.setCenter(previousView);
        loadView("/com/uet/auction/view/ProfileContent.fxml");
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
}