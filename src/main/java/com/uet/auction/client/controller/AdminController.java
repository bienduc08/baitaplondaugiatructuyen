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
    @FXML
    private BorderPane mainBorderPane;
    @FXML
    private VBox adminContent; // Màn hình quản lý duyệt sản phẩm
    @FXML
    private Label welcomeLabel; // Đã sửa tên biến khớp với FXML
    @FXML
    private Label lblCountPending;
    @FXML
    private Label lblCountOpen;
    @FXML
    private Label lblCountClosed;
    @FXML
    private Label lblBalance;

    @FXML
    private TableView<ProductDTO> pendingTable;
    @FXML
    private TableColumn<ProductDTO, Integer> idCol;
    @FXML
    private TableColumn<ProductDTO, String> nameCol;
    @FXML
    private TableColumn<ProductDTO, Double> priceCol;
    @FXML
    private TableColumn<ProductDTO, String> sellerCol;
    @FXML
    private TableColumn<ProductDTO, String> statusCol;

    // Đã đổi String thành LocalDateTime để chuẩn hóa hiển thị thời gian
    @FXML
    private TableColumn<ProductDTO, LocalDateTime> endTimeCol;

    private final ObservableList<ProductDTO> pendingListData = FXCollections.observableArrayList();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private enum ActiveView { HOME, PENDING,MANAGEMENT, PROFILE }
    private AdminController.ActiveView activeView = AdminController.ActiveView.HOME;

    @FXML
    public void initialize() {
        instance = this;
        pendingTable.setItems(pendingListData);
        if (welcomeLabel != null && SessionManager.getCurrentUsername() != null) {
            welcomeLabel.setText("Xin chào, Admin: " + SessionManager.getCurrentUsername());
        }
        if (lblBalance != null && SessionManager.getCurrentUser() != null) {
            lblBalance.setText(String.format("%,.0f VNĐ", SessionManager.getCurrentUser().getBalance()));

            // Tải số dư mới nhất từ server
            String username = SessionManager.getCurrentUsername();
            if (username != null) {
                SocketClient.sendRequest(new AuctionRequest("GET_USER_BALANCE", username));
            }
        }
        loadPendingProducts();
        onShowHomeClick();
    }

    public void updateBalance() {
        if (SessionManager.getCurrentUser() != null && lblBalance != null) {
            lblBalance.setText(String.format("%,.0f VNĐ", SessionManager.getCurrentUser().getBalance()));
        }
    }

    public void loadPendingProducts() {
        SocketClient.sendRequest(new AuctionRequest("GET_ALL_PRODUCTS", null));
    }


    public void updatePendingList(List<ProductDTO> products) {
        Platform.runLater(() -> {
            pendingListData.setAll(products);
            long pending = products.stream().filter(p -> "PENDING".equals(p.getStatus())).count();
            long open = products.stream().filter(p -> "OPEN".equals(p.getStatus())).count();
            long closed = products.stream().filter(p -> "CLOSED".equals(p.getStatus())).count();
            if (lblCountPending != null) lblCountPending.setText(String.valueOf(pending));
            if (lblCountOpen != null) lblCountOpen.setText(String.valueOf(open));
            if (lblCountClosed != null) lblCountClosed.setText(String.valueOf(closed));
        });
    }

    @FXML
    public void onApproveButtonClick() {
        ProductDTO selected = pendingTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showError("Vui lòng chọn sản phẩm!");
            return;
        }
        if ("OPEN".equals(selected.getStatus())) {
            AlertHelper.showError("Sản phẩm này đã đang đấu giá!");
            return;
        }
        if ("CLOSED".equals(selected.getStatus())) {
            AlertHelper.showError("Sản phẩm này đã đóng!");
            return;
        }

        Object[] data = {selected.getId(), "OPEN"};
        SocketClient.sendRequest(new AuctionRequest("CHANGE_PRODUCT_STATUS", data));
    }

    @FXML
    public void onRejectButtonClick() {
        ProductDTO selected = pendingTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertHelper.showError("Vui lòng chọn sản phẩm!");
            return;
        }
        Object[] data = {selected.getId(), "REJECTED"};
        SocketClient.sendRequest(new AuctionRequest("CHANGE_PRODUCT_STATUS", data));
    }

    @FXML
    public void onRefreshButtonClick() {
        // Tải số dư mới nhất từ server khi nhấn làm mới
        String username = SessionManager.getCurrentUsername();
        if (username != null) {
            SocketClient.sendRequest(new AuctionRequest("GET_USER_BALANCE", username));
        }
        onShowHomeClick();
    }

    @FXML
    public void onLogoutButtonClick() {
        try {
            SessionManager.clearSession();
            SceneManager.switchScene("/com/uet/auction/view/Login.fxml", "Đăng nhập");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public BorderPane getMainBorderPane() {
        return this.mainBorderPane;
    }

    // ------- XỬ LÝ MENU ĐIỀU HƯỚNG -------

    @FXML
    public void onShowHomeClick() {
        loadView("/com/uet/auction/view/HomeContent.fxml",ActiveView.HOME);
    }

    @FXML
    public void onPendingClick() {
        loadView("/com/uet/auction/view/AdminPending.fxml",ActiveView.PENDING);
    }

    @FXML
    public void onUserManageClick() {
        loadView("/com/uet/auction/view/AdminUserManagement.fxml",ActiveView.MANAGEMENT);
    }


    @FXML
    public void onProfileButtonClick() {
        Node previousView = mainBorderPane.getCenter();
        ProfileController.onBackAction = () -> mainBorderPane.setCenter(previousView);
        loadView("/com/uet/auction/view/ProfileAdmin.fxml", AdminController.ActiveView.PROFILE);
    }

    private void loadView(String fxmlPath, AdminController.ActiveView view) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node node = loader.load();
            mainBorderPane.setCenter(node);
            activeView = view;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}