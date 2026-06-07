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

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class HomeContentController {

    public static HomeContentController instance;

    @FXML private GridPane productContainer;
    @FXML private TextField txtSearch;

    private List<ProductDTO> allProducts;
    private String currentCategory = "ALL";

    @FXML
    public void initialize() {
        instance = this;
        if (productContainer != null) {
            // Căn giữa toàn bộ lưới sản phẩm
            productContainer.setAlignment(javafx.geometry.Pos.TOP_CENTER);
            productContainer.setHgap(20);
            productContainer.setVgap(25);
        }

        // Bắt sự kiện mỗi khi người dùng gõ phím vào ô tìm kiếm (Đã xóa đoạn bị lặp)
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((observable, oldValue, newValue) -> filterAndDisplay());
        }

        // Tự động gọi API lấy danh sách sản phẩm lần đầu
        loadProducts();

        // Không cần auto-refresh phía client vì server đã broadcast UPDATE_PRICE mỗi 5 giây.
        // Tránh trùng lặp: server push + client pull cùng lúc gây load 2 lần.
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
            // ĐÃ SỬA: Với GridPane, khi add 1 phần tử chiếm chỗ, nên dùng add(node, col, row)
            productContainer.add(emptyMsg, 0, 0);
            return;
        }

        int col = 0;
        int row = 0;
        final int MAX_COLS = 4; // Cố định 4 sản phẩm trên 1 hàng

        for (ProductDTO product : list) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/uet/auction/view/ProductItem.fxml"));
                Node productCard = loader.load();

                ProductItemController controller = loader.getController();
                controller.setProductData(product);
                productCard.setUserData(controller);

                // Thêm vào GridPane theo tọa độ (cột, hàng)
                productContainer.add(productCard, col, row);

                col++;
                if (col >= MAX_COLS) {
                    col = 0; // Reset về cột đầu tiên
                    row++;   // Xuống hàng tiếp theo
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