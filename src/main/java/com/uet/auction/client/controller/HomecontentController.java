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
import javafx.scene.layout.FlowPane;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class HomecontentController {

    public static HomecontentController instance; // Để ResponseListener có thể gọi hàm trả dữ liệu về

    @FXML private FlowPane productContainer;
    @FXML private TextField txtSearch;

    private List<ProductDTO> allProducts;
    private String currentCategory = "ALL"; // Biến lưu trạng thái lọc danh mục hiện tại

    @FXML
    public void initialize() {
        instance = this;

        // Bắt sự kiện mỗi khi người dùng gõ phím vào ô tìm kiếm thì tự động lọc luôn
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((observable, oldValue, newValue) -> filterAndDisplay());
        }

        // Tự động gọi API lấy danh sách sản phẩm đang đấu giá khi vừa load trang chủ
        loadProducts();
    }

    /**
     * Gửi yêu cầu lên Server để lấy sản phẩm
     */
    public void loadProducts() {
        SocketClient.sendRequest(new AuctionRequest("GET_OPEN_PRODUCTS", null));
    }

    /**
     * Server trả về danh sách, hàm này được gọi từ ResponseListener
     */
    public void displayProducts(List<ProductDTO> products) {
        Platform.runLater(() -> {
            this.allProducts = products;
            filterAndDisplay();
        });
    }

    /**
     * Hàm lọc kết hợp cả TỪ KHÓA TÌM KIẾM và DANH MỤC (Đồ điện tử, Thời trang...)
     */
    private void filterAndDisplay() {
        if (productContainer == null) return;

        String keyword = (txtSearch != null) ? txtSearch.getText().trim().toLowerCase() : "";

        List<ProductDTO> filteredList = (allProducts == null) ? List.of() : allProducts.stream()
                .filter(p -> {
                    // 1. Lọc theo tên sản phẩm hoặc tên người bán
                    boolean matchKeyword = keyword.isEmpty() ||
                            p.getName().toLowerCase().contains(keyword) ||
                            (p.getSellerName() != null && p.getSellerName().toLowerCase().contains(keyword));

                    // 2. Lọc theo danh mục (Giả sử ProductDTO có trường category)
                    // Nếu project của bạn chưa có getCategory(), bạn có thể bỏ qua phần matchCategory này.
                    boolean matchCategory = currentCategory.equals("ALL") ||
                            (p.getCategory() != null && p.getCategory().equals(currentCategory));

                    return matchKeyword && matchCategory;
                })
                .collect(Collectors.toList());

        renderProducts(filteredList);
    }

    /**
     * Hàm in danh sách sản phẩm đã lọc ra màn hình
     */
    private void renderProducts(List<ProductDTO> list) {
        productContainer.getChildren().clear(); // Xóa sạch các ô cũ

        if (list.isEmpty()) {
            Label emptyMsg = new Label("Không tìm thấy sản phẩm nào phù hợp.");
            emptyMsg.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 15; -fx-padding: 20;");
            productContainer.getChildren().add(emptyMsg);
            return;
        }

        for (ProductDTO product : list) {
            try {
                // 1. Tải giao diện của một ô sản phẩm
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/uet/auction/view/ProductItem.fxml"));
                Node productCard = loader.load();

                // 2. Lấy controller của ô đó để truyền dữ liệu
                ProductItemController controller = loader.getController();
                controller.setProductData(product);

                // 3. Ném ô sản phẩm vào FlowPane (Lưới sản phẩm)
                productContainer.getChildren().add(productCard);

            } catch (IOException e) {
                System.err.println("Lỗi load ProductItem: " + e.getMessage());
            }
        }
    }

    // ================== CÁC NÚT LỌC THEO DANH MỤC ==================

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