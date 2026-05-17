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
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdminController {

    public static AdminController instance;

    @FXML private Label lblAdminName;
    @FXML private Label lblCountPending;
    @FXML private Label lblCountOpen;
    @FXML private Label lblCountClosed;

    @FXML private TableView<ProductDTO>            pendingTable;
    @FXML private TableColumn<ProductDTO, Integer> idCol;
    @FXML private TableColumn<ProductDTO, String>  nameCol;
    @FXML private TableColumn<ProductDTO, Double>  priceCol;
    @FXML private TableColumn<ProductDTO, String>  sellerCol;
    @FXML private TableColumn<ProductDTO, String>  statusCol;
    @FXML private TableColumn<ProductDTO, String>  endTimeCol;

    private final ObservableList<ProductDTO> pendingListData = FXCollections.observableArrayList();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        instance = this;
        setupTable();
        pendingTable.setItems(pendingListData);

        if (lblAdminName != null && SessionManager.getCurrentUsername() != null)
            lblAdminName.setText("Admin: " + SessionManager.getCurrentUsername());

        loadPendingProducts();
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
            endTimeCol.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String s, boolean empty) {
                    super.updateItem(s, empty);
                    if (empty) { setText(null); return; }
                    ProductDTO p = getTableView().getItems().get(getIndex());
                    setText(p != null && p.getEndTime() != null ? p.getEndTime().format(FMT) : "—");
                }
            });
        }
    }

    /** Load TẤT CẢ sản phẩm trừ CLOSED để admin quản lý được toàn bộ */
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

        // FIX CHÍNH: đặt thẳng "OPEN" thay vì "APPROVED"
        // Không cần chờ AuctionTimer chuyển APPROVED→OPEN nữa
        // → user có thể bid ngay sau khi admin duyệt
        Object[] data = {selected.getId(), "OPEN"};
        SocketClient.sendRequest(new AuctionRequest("CHANGE_PRODUCT_STATUS", data));
    }

    @FXML public void onRejectButtonClick() {
        ProductDTO selected = pendingTable.getSelectionModel().getSelectedItem();
        if (selected == null) { AlertHelper.showError("Vui lòng chọn sản phẩm!"); return; }
        Object[] data = {selected.getId(), "REJECTED"};
        SocketClient.sendRequest(new AuctionRequest("CHANGE_PRODUCT_STATUS", data));
    }

    @FXML public void onRefreshButtonClick() { loadPendingProducts(); }

    @FXML public void onLogoutButtonClick() {
        try {
            SessionManager.clearSession();
            SceneManager.switchScene("/com/uet/auction/view/Login.fxml", "Đăng nhập");
        } catch (IOException e) { e.printStackTrace(); }
    }
}