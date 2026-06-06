package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.common.Request.AuctionRequest;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class HomecontentController {

    public static HomecontentController instance;

    @FXML private GridPane productContainer;
    @FXML private TextField txtSearch;

    private List<ProductDTO> allProducts;
    private String currentCategory = "ALL";
    private Timeline autoRefreshTimeline;

    @FXML
    public void initialize() {
        instance = this;
        if (productContainer != null) {
            productContainer.setAlignment(javafx.geometry.Pos.TOP_CENTER);
            productContainer.setHgap(20);
            productContainer.setVgap(25);
        }

        if (txtSearch != null) {
            txtSearch.textProperty().addListener((observable, oldValue, newValue) -> filterAndDisplay());
        }

        loadProducts();

        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(5), e -> loadProducts()));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);

        if (productContainer.getScene() != null) {
            autoRefreshTimeline.play();
        }
        productContainer.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                autoRefreshTimeline.play();
            } else {
                autoRefreshTimeline.stop();
            }
        });
    }

    public void loadProducts() {
        SocketClient.sendRequest(new AuctionRequest("GET_OPEN_PRODUCTS", null));
    }

    public void displayProducts(List<ProductDTO> products) {
        Platform.runLater(() -> {
            this.allProducts = products;
            filterAndDisplay();
        });
    }

    /**
     * FIX: Cập nhật end_time của từng ProductItem đang hiển thị
     * mà KHÔNG render lại toàn bộ danh sách (tránh nhấp nháy).
     * Được gọi khi nhận UPDATE_PRICE kèm inlineProduct (anti-sniping).
     */
    public void updateProductEndTime(ProductDTO updatedProduct) {
        if (updatedProduct == null || productContainer == null) return;
        Platform.runLater(() -> {
            for (Node child : productContainer.getChildren()) {
                if (child.getUserData() instanceof ProductItemController) {
                    ProductItemController ctrl = (ProductItemController) child.getUserData();
                    ctrl.updateEndTime(updatedProduct);
                }
            }
            // Cập nhật allProducts cache để lần render tiếp theo dùng end_time mới
            if (allProducts != null) {
                for (int i = 0; i < allProducts.size(); i++) {
                    if (allProducts.get(i).getId() == updatedProduct.getId()) {
                        allProducts.get(i).setEndTime(updatedProduct.getEndTime());
                        allProducts.get(i).setCurrentPrice(updatedProduct.getCurrentPrice());
                        allProducts.get(i).setOwnerName(updatedProduct.getOwnerName());
                        break;
                    }
                }
            }
        });
    }

    private void filterAndDisplay() {
        if (productContainer == null) return;

        String keyword = (txtSearch != null) ? txtSearch.getText().trim().toLowerCase() : "";

        List<ProductDTO> filteredList = (allProducts == null) ? List.of() : allProducts.stream()
                .filter(p -> {
                    boolean matchKeyword = keyword.isEmpty() ||
                            p.getName().toLowerCase().contains(keyword) ||
                            (p.getSellerName() != null && p.getSellerName().toLowerCase().contains(keyword));

                    boolean matchCategory = currentCategory.equals("ALL") ||
                            (p.getCategory() != null && p.getCategory().equals(currentCategory));

                    return matchKeyword && matchCategory;
                })
                .collect(Collectors.toList());

        renderProducts(filteredList);
    }

    private void renderProducts(List<ProductDTO> list) {
        if (productContainer != null) {
            for (Node child : productContainer.getChildren()) {
                if (child.getUserData() instanceof ProductItemController) {
                    ((ProductItemController) child.getUserData()).stopCountdown();
                }
            }
            productContainer.getChildren().clear();
        }

        if (list.isEmpty()) {
            Label emptyMsg = new Label("Không tìm thấy sản phẩm nào phù hợp.");
            emptyMsg.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 15; -fx-padding: 20;");
            productContainer.add(emptyMsg, 0, 0);
            return;
        }

        int col = 0;
        int row = 0;
        final int MAX_COLS = 4;

        for (ProductDTO product : list) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/uet/auction/view/ProductItem.fxml"));
                Node productCard = loader.load();

                ProductItemController controller = loader.getController();
                controller.setProductData(product);
                productCard.setUserData(controller);

                productContainer.add(productCard, col, row);

                col++;
                if (col >= MAX_COLS) {
                    col = 0;
                    row++;
                }
            } catch (IOException e) {
                System.err.println("Lỗi load ProductItem: " + e.getMessage());
            }
        }
    }

    @FXML
    public void onFilterAll() {
        currentCategory = "ALL";
        filterAndDisplay();
    }

    @FXML
    public void onFilterElectronics() {
        currentCategory = "Đồ điện tử";
        filterAndDisplay();
    }

    @FXML
    public void onFilterFashion() {
        currentCategory = "Thời trang";
        filterAndDisplay();
    }
}