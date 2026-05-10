package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.SceneManager;
import com.uet.auction.client.util.SessionManager;
import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.common.Request.AuctionRequest;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class UserController {

    public static UserController instance;

    @FXML private FlowPane productContainer;
    @FXML private Label welcomeLabel;
    @FXML private Label lblBalance;
    @FXML private TextField txtSearch;

    // Lưu toàn bộ danh sách gốc để filter không cần gọi server
    private List<ProductDTO> allProducts;

    // Danh mục đang chọn — null = tất cả
    private String selectedCategory = null;
    @FXML
    public void initialize() {
        instance = this;

        if (SessionManager.getCurrentUser() != null) {
            if (welcomeLabel != null) {
                welcomeLabel.setText("Xin chào, " + SessionManager.getCurrentUsername() + "!");
            }
            if (lblBalance != null) {
                // THAY "—" bằng số dư thực từ session
                double balance = SessionManager.getCurrentUser().getBalance();
                lblBalance.setText(String.format("%,.0f VNĐ", balance));
            }
        }

        if (txtSearch != null) {
            txtSearch.textProperty().addListener((obs, oldVal, newVal) -> filterAndDisplay());
        }

        loadProducts();
    }

    /** Gửi request lấy danh sách sản phẩm OPEN từ server */
    public void loadProducts() {
        SocketClient.sendRequest(new AuctionRequest("GET_OPEN_PRODUCTS", null));
    }

    /** Được gọi bởi ResponseListener khi nhận GET_PRODUCTS_RESULT */
    public void displayProducts(List<ProductDTO> products) {
        Platform.runLater(() -> {
            this.allProducts = products;
            filterAndDisplay();
        });
    }

    /**
     * Filter theo ô tìm kiếm + danh mục đang chọn rồi render.
     * Gọi lại bất cứ khi nào search text hoặc category thay đổi.
     */
    private void filterAndDisplay() {
        if (productContainer == null) return;

        String keyword = (txtSearch != null) ? txtSearch.getText().trim().toLowerCase() : "";

        List<ProductDTO> filtered = (allProducts == null)
                ? List.of()
                : allProducts.stream()
                .filter(p -> keyword.isEmpty()
                        || p.getName().toLowerCase().contains(keyword)
                        || (p.getSellerName() != null && p.getSellerName().toLowerCase().contains(keyword)))
                .collect(Collectors.toList());

        renderProducts(filtered);
    }

    /** Vẽ thẻ sản phẩm vào FlowPane */
    private void renderProducts(List<ProductDTO> list) {
        productContainer.getChildren().clear();
        if (list.isEmpty()) {
            Label empty = new Label("Không có sản phẩm nào đang đấu giá.");
            empty.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14;");
            productContainer.getChildren().add(empty);
            return;
        }
        for (ProductDTO product : list) {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/uet/auction/view/ProductItem.fxml"));
                Node card = loader.load();
                ProductItemController ctrl = loader.getController();
                ctrl.setData(product);
                productContainer.getChildren().add(card);
            } catch (IOException e) {
                System.err.println("Lỗi khi load ProductItem.fxml: " + e.getMessage());
            }
        }
    }

    // --- Nút bấm trên header ---

    @FXML
    public void onRefreshButtonClick() {
        if (txtSearch != null) txtSearch.clear();
        selectedCategory = null;
        loadProducts();
    }

    /** Nút lọc danh mục — gọi từ FXML onAction */
    @FXML
    public void onFilterAll()         { selectedCategory = null;          filterAndDisplay(); }
    @FXML
    public void onFilterElectronics() { selectedCategory = "Đồ điện tử"; filterAndDisplay(); }
    @FXML
    public void onFilterFashion()     { selectedCategory = "Thời trang";  filterAndDisplay(); }
    @FXML
    public void onFilterAntiques()    { selectedCategory = "Đồ cổ";       filterAndDisplay(); }

    @FXML
    public void onLogoutButtonClick() {
        try {
            SessionManager.clearSession();
            SceneManager.switchScene("/com/uet/auction/view/Login.fxml", "Đăng nhập");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}