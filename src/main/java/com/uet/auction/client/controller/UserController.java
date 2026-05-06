package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.AlertHelper;
import com.uet.auction.client.util.SceneManager;
import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.common.Request.AuctionRequest;
import com.uet.auction.common.Response.AuctionResponse;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import java.util.List;

public class UserController {
    @FXML private VBox productListContainer;
    public static UserController instance;

    @FXML
    public void initialize() {
        instance = this;
        loadProducts(); // Load ngay khi mở màn hình
    }

    // Hàm này được gọi từ ResponseListener
    public void displayProducts(List<ProductDTO> products) {
        Platform.runLater(() -> {
            productListContainer.getChildren().clear();
            for (ProductDTO p : products) {
                Label label = new Label(p.getName() + " - Giá: " + p.getCurrentPrice());
                productListContainer.getChildren().add(label);
                // Lưu ý: Bạn có thể thay Label bằng việc load file product_item.fxml như bài trước mình hướng dẫn
            }
        });
    }

    @FXML
    public void onLogoutClick() {
        Stage stage = (Stage) productListContainer.getScene().getWindow();
        SceneManager.switchScene(stage, "/com/uet/auction/view/Login.fxml");
    }
    public void loadProducts() {
        // User chỉ được lấy các sản phẩm đang OPEN
        AuctionRequest req = new AuctionRequest("GET_OPEN_PRODUCTS", null);
        SocketClient.sendRequest(req);
    }
}