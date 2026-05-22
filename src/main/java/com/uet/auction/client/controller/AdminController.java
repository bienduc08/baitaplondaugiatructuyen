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
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminController {

    public static AdminController instance;
    @FXML private BorderPane mainBorderPane;
    @FXML private VBox adminContent; // Màn hình quản lý duyệt sản phẩm
    @FXML private Label welcomeLabel; // Đã sửa tên biến khớp với FXML
    @FXML private Label lblCountPending;
    @FXML private Label lblCountOpen;
    @FXML private Label lblCountClosed;
    @FXML private Label lblBalance;

    @FXML private TableView<ProductDTO> pendingTable;
    @FXML private TableColumn<ProductDTO, Integer> idCol;
    @FXML private TableColumn<ProductDTO, String>  nameCol;
    @FXML private TableColumn<ProductDTO, Double>  priceCol;
    @FXML private TableColumn<ProductDTO, String>  sellerCol;
    @FXML private TableColumn<ProductDTO, String>  statusCol;

    // Đã đổi String thành LocalDateTime để chuẩn hóa hiển thị thời gian
    @FXML private TableColumn<ProductDTO, LocalDateTime> endTimeCol;

    private final ObservableList<ProductDTO> pendingListData = FXCollections.observableArrayList();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        instance = this;
        setupTable();
        pendingTable.setItems(pendingListData);
        if (welcomeLabel != null && SessionManager.getCurrentUsername() != null) {
            welcomeLabel.setText("Xin chào, Admin: " + SessionManager.getCurrentUsername());
        }
        if (lblBalance != null && SessionManager.getCurrentUser() != null) {
            lblBalance.setText(String.format("%,.0f VNĐ", SessionManager.getCurrentUser().getBalance()));
        }
        onShowHomeClick();
    }

    public void updateBalance() {
        if (SessionManager.getCurrentUser() != null && lblBalance != null) {
            lblBalance.setText(String.format("%,.0f VNĐ", SessionManager.getCurrentUser().getBalance()));
        }
    }

    private void setupTable() {
        if (idCol   != null) idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        if (nameCol != null) nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        if (priceCol != null) {
            priceCol.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
            priceCol.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(Double v, boolean empty) {
                    super.updateItem(v, empty);
                    setText(empty || v == null ? null : String.format("%,.0f VNĐ", v));
                }
            });
        }
        if (sellerCol != null) sellerCol.setCellValueFactory(new PropertyValueFactory<>("sellerName"));
        if (statusCol != null) {
            statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
            statusCol.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String s, boolean empty) {
                    super.updateItem(s, empty);
                    if (empty || s == null) { setText(null); setStyle(""); return; }
                    switch (s) {
                        case "PENDING":  setText("⏳ Chờ duyệt"); setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;"); break;
                        case "OPEN":     setText("🔥 Đang đấu");  setStyle("-fx-text-fill: #2980b9; -fx-font-weight: bold;"); break;
                        case "CLOSED":   setText("🔒 Đã đóng");   setStyle("-fx-text-fill: #7f8c8d;"); break;
                        case "REJECTED": setText("✘ Từ chối");    setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;"); break;
                        default:         setText(s); setStyle(""); break;
                    }
                }
            });
        }
        if (endTimeCol != null) {
            // FIX LỖI: Map cột với thuộc tính endTime và định dạng an toàn
            endTimeCol.setCellValueFactory(new PropertyValueFactory<>("endTime"));
            endTimeCol.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(LocalDateTime item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText("—");
                    } else {
                        setText(item.format(FMT));
                    }
                }
            });
        }
    }

    public void loadPendingProducts() {
        SocketClient.sendRequest(new AuctionRequest("GET_ALL_PRODUCTS", null));
    }

    public void updatePendingList(List<ProductDTO> products) {
        Platform.runLater(() -> {
            pendingListData.setAll(products);
            long pending = products.stream().filter(p -> "PENDING".equals(p.getStatus())).count();
            long open    = products.stream().filter(p -> "OPEN".equals(p.getStatus())).count();
            long closed  = products.stream().filter(p -> "CLOSED".equals(p.getStatus())).count();
            if (lblCountPending != null) lblCountPending.setText(String.valueOf(pending));
            if (lblCountOpen    != null) lblCountOpen.setText(String.valueOf(open));
            if (lblCountClosed  != null) lblCountClosed.setText(String.valueOf(closed));
        });
    }

    @FXML public void onApproveButtonClick() {
        ProductDTO selected = pendingTable.getSelectionModel().getSelectedItem();
        if (selected == null) { AlertHelper.showError("Vui lòng chọn sản phẩm!"); return; }
        if ("OPEN".equals(selected.getStatus())) { AlertHelper.showError("Sản phẩm này đã đang đấu giá!"); return; }
        if ("CLOSED".equals(selected.getStatus())) { AlertHelper.showError("Sản phẩm này đã đóng!"); return; }

        Object[] data = {selected.getId(), "OPEN"};
        SocketClient.sendRequest(new AuctionRequest("CHANGE_PRODUCT_STATUS", data));
    }

    @FXML public void onRejectButtonClick() {
        ProductDTO selected = pendingTable.getSelectionModel().getSelectedItem();
        if (selected == null) { AlertHelper.showError("Vui lòng chọn sản phẩm!"); return; }
        Object[] data = {selected.getId(), "REJECTED"};
        SocketClient.sendRequest(new AuctionRequest("CHANGE_PRODUCT_STATUS", data));
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

    @FXML public void onShowHomeClick() {
        loadView("/com/uet/auction/view/HomeContent.fxml");
    }

    @FXML public void onPendingClick() {
        // Khi click "Duyệt sản phẩm", trả lại adminContent vào giữa màn hình
        if (adminContent != null) {
            mainBorderPane.setCenter(adminContent);
            loadPendingProducts();
        }
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
            // Không tự động gọi loadPendingProducts() khi đổi view;
            // chỉ tải khi người dùng bấm vào tab "Duyệt sản phẩm" (onPendingClick)
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}