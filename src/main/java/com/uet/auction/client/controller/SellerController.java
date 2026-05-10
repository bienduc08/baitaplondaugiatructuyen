package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.AlertHelper;
import com.uet.auction.client.util.SceneManager;
import com.uet.auction.client.util.SessionManager;
import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.common.Request.AuctionRequest;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class SellerController {

    public static SellerController instance;

    @FXML private Label lblActiveAuctions;
    @FXML private Label lblSoldItems;

    @FXML private TextField nameField;
    @FXML private TextField priceField;
    @FXML private TextArea  descriptionArea;
    @FXML private DatePicker endDatePicker;

    // FIX: nút Gửi để disable trong lúc đang gửi
    @FXML private Button btnSubmit;
    // FIX: label trạng thái form thay thế cho popup "chờ admin duyệt" giả
    @FXML private Label lblFormStatus;

    @FXML private TableView<ProductDTO>            tableSellerProducts;
    @FXML private TableColumn<ProductDTO, Integer> colId;
    @FXML private TableColumn<ProductDTO, String>  colName;
    @FXML private TableColumn<ProductDTO, Double>  colPrice;
    @FXML private TableColumn<ProductDTO, Double>  colHighestBid;
    @FXML private TableColumn<ProductDTO, String>  colStatus;
    @FXML private TableColumn<ProductDTO, Void>    colAction;

    private final ObservableList<ProductDTO> productList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        instance = this;
        setupTable();
        tableSellerProducts.setItems(productList);
        loadMyProducts();
    }

    private void setupTable() {
        if (colId       != null) colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        if (colName     != null) colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        if (colPrice    != null) colPrice.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
        if (colHighestBid != null) colHighestBid.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        if (colStatus   != null) {
            colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
            // Hiển thị màu theo trạng thái
            colStatus.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String status, boolean empty) {
                    super.updateItem(status, empty);
                    if (empty || status == null) { setText(null); setStyle(""); return; }
                    setText(statusLabel(status));
                    setStyle(statusStyle(status));
                }
            });
        }
        if (colAction != null) setupActionColumn();
    }

    private String statusLabel(String s) {
        switch (s) {
            case "PENDING":  return "⏳ Chờ duyệt";
            case "APPROVED": return "✔ Đã duyệt";
            case "OPEN":     return "🔥 Đang đấu";
            case "CLOSED":   return "🔒 Đã đóng";
            case "REJECTED": return "✘ Từ chối";
            default: return s;
        }
    }

    private String statusStyle(String s) {
        switch (s) {
            case "PENDING":  return "-fx-text-fill: #e67e22; -fx-font-weight: bold;";
            case "APPROVED": return "-fx-text-fill: #27ae60; -fx-font-weight: bold;";
            case "OPEN":     return "-fx-text-fill: #2980b9; -fx-font-weight: bold;";
            case "CLOSED":   return "-fx-text-fill: #7f8c8d;";
            case "REJECTED": return "-fx-text-fill: #e74c3c; -fx-font-weight: bold;";
            default: return "";
        }
    }

    private void setupActionColumn() {
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Xem lịch sử");
            {
                btn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;" +
                        "-fx-background-radius: 4; -fx-cursor: hand; -fx-font-size: 11;");
                btn.setOnAction(e -> {
                    ProductDTO p = getTableView().getItems().get(getIndex());
                    openBidHistory(p);
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });
    }

    private void openBidHistory(ProductDTO product) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/uet/auction/view/BidHistoryView.fxml"));
            Parent root = loader.load();
            BidHistoryController ctrl = loader.getController();
            ctrl.setProductContext(product.getId(), product.getName());
            Stage stage = new Stage();
            stage.setTitle("Lịch sử đấu giá — " + product.getName());
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            AlertHelper.showError("Không thể mở lịch sử đấu giá!");
        }
    }

    @FXML public void handleAddNewProduct() {
        if (nameField != null) nameField.requestFocus();
    }

    /**
     * FIX CHÍNH:
     * 1. Disable nút "Gửi đăng bán" ngay sau khi click (tránh double-submit)
     * 2. KHÔNG gọi clearFields() ngay — đợi ResponseListener xác nhận thành công
     * 3. Hiện trạng thái "Đang gửi..." thay vì popup giả
     */
    @FXML
    public void onPostProductButtonClick() {
        String name     = nameField.getText().trim();
        String priceStr = priceField.getText().trim();

        if (name.isEmpty() || priceStr.isEmpty() || endDatePicker.getValue() == null) {
            setFormStatus("Vui lòng điền đầy đủ: Tên, Giá và Ngày kết thúc!", false);
            return;
        }
        if (SessionManager.getCurrentUsername() == null) {
            setFormStatus("Phiên đăng nhập hết hạn, vui lòng đăng nhập lại!", false);
            return;
        }

        try {
            double startingPrice = Double.parseDouble(priceStr);
            if (startingPrice <= 0) {
                setFormStatus("Giá khởi điểm phải lớn hơn 0!", false);
                return;
            }

            LocalDateTime endTime = LocalDateTime.of(endDatePicker.getValue(), LocalTime.of(23, 59));
            if (endTime.isBefore(LocalDateTime.now())) {
                setFormStatus("Ngày kết thúc phải ở tương lai!", false);
                return;
            }

            ProductDTO product = new ProductDTO();
            product.setName(name);
            product.setStartingPrice(startingPrice);
            product.setCurrentPrice(startingPrice);
            product.setDescription(descriptionArea != null ? descriptionArea.getText().trim() : "");
            product.setSellerName(SessionManager.getCurrentUsername());
            product.setStartTime(LocalDateTime.now());
            product.setEndTime(endTime);
            product.setStatus("PENDING");

            // Disable nút để tránh gửi 2 lần
            if (btnSubmit != null) btnSubmit.setDisable(true);
            setFormStatus("⏳ Đang gửi lên server...", null);

            SocketClient.sendRequest(new AuctionRequest("ADD_PRODUCT", product));
            // clearFields() sẽ được gọi trong handleAddProductResult() khi server xác nhận

        } catch (NumberFormatException e) {
            setFormStatus("Giá khởi điểm phải là số hợp lệ (VD: 500000)!", false);
        }
    }

    /**
     * Gọi bởi ResponseListener khi nhận ADD_PRODUCT_RESULT.
     * FIX: chỉ clear form khi server xác nhận thành công.
     */
    public void handleAddProductResult(boolean success, String message) {
        Platform.runLater(() -> {
            if (btnSubmit != null) btnSubmit.setDisable(false);
            if (success) {
                setFormStatus("✔ " + message, true);
                clearFields();
                loadMyProducts(); // Reload bảng để hiện sản phẩm mới
            } else {
                setFormStatus("✘ " + message, false);
                // KHÔNG clear form — để user sửa và thử lại
            }
        });
    }

    private void setFormStatus(String text, Boolean success) {
        if (lblFormStatus == null) return;
        lblFormStatus.setText(text);
        if (success == null)       lblFormStatus.setStyle("-fx-text-fill: #7f8c8d;");
        else if (success)          lblFormStatus.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        else                       lblFormStatus.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
    }

    public void loadMyProducts() {
        String username = SessionManager.getCurrentUsername();
        if (username == null) return;
        SocketClient.sendRequest(new AuctionRequest("GET_MY_PRODUCTS", username));
    }

    public void displayMyProducts(List<ProductDTO> list) {
        Platform.runLater(() -> {
            productList.setAll(list);
            long active = list.stream().filter(p -> "OPEN".equals(p.getStatus())).count();
            long sold   = list.stream().filter(p -> "CLOSED".equals(p.getStatus())).count();
            if (lblActiveAuctions != null) lblActiveAuctions.setText(String.valueOf(active));
            if (lblSoldItems      != null) lblSoldItems.setText(String.valueOf(sold));
        });
    }

    private void clearFields() {
        nameField.clear();
        priceField.clear();
        if (descriptionArea != null) descriptionArea.clear();
        endDatePicker.setValue(null);
        setFormStatus("", null);
    }

    @FXML public void onBackButtonClick() throws IOException {
        SceneManager.switchScene("/com/uet/auction/view/User.fxml", "Trang chủ");
    }

    @FXML public void onLogoutButtonClick() throws IOException {
        SessionManager.clearSession();
        SceneManager.switchScene("/com/uet/auction/view/Login.fxml", "Đăng nhập");
    }
}