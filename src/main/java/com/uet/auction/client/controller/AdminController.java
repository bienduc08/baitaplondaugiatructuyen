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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.util.List;

public class AdminController {

    // Gắn ID với file Admin.fxml
    @FXML private TableView<ProductDTO> pendingTable;
    @FXML private TableColumn<ProductDTO, Integer> colId;
    @FXML private TableColumn<ProductDTO, String> colName;
    @FXML private TableColumn<ProductDTO, Double> colPrice;
    @FXML private TableColumn<ProductDTO, String> colSeller;

    public static AdminController instance;

    @FXML
    public void initialize() {
        instance = this;

        // Cài đặt dữ liệu cho các cột (Tên biến phải khớp y hệt trong ProductDTO)
        if (colId != null) colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        if (colName != null) colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        if (colPrice != null) colPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        if (colSeller != null) colSeller.setCellValueFactory(new PropertyValueFactory<>("sellerName"));

        // Vừa mở lên là tự động tải danh sách chờ duyệt
        loadPendingProducts();
    }

    // Gửi yêu cầu lấy sản phẩm PENDING
    public void loadPendingProducts() {
        SocketClient.sendRequest(new AuctionRequest("GET_PENDING_PRODUCTS", null));
    }

    // Hàm này được ResponseListener gọi khi Server trả về kết quả
    public void updatePendingList(List<ProductDTO> list) {
        Platform.runLater(() -> {
            ObservableList<ProductDTO> data = FXCollections.observableArrayList(list);
            if (pendingTable != null) {
                pendingTable.setItems(data);
            }
        });
    }

    // Xử lý nút Duyệt
    @FXML
    public void onApproveClick() {
        ProductDTO selected = pendingTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Object[] data = new Object[]{selected.getId(), "APPROVED"};
            SocketClient.sendRequest(new AuctionRequest("CHANGE_PRODUCT_STATUS", data));
        } else {
            AlertHelper.showError("Hãy chọn một sản phẩm trong bảng để duyệt!");
        }
    }

    // Xử lý nút Từ chối
    @FXML
    public void onRejectClick() {
        ProductDTO selected = pendingTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Object[] data = new Object[]{selected.getId(), "REJECTED"};
            SocketClient.sendRequest(new AuctionRequest("CHANGE_PRODUCT_STATUS", data));
        } else {
            AlertHelper.showError("Hãy chọn một sản phẩm trong bảng để từ chối!");
        }
    }

    // Xử lý nút Đăng xuất
    @FXML
    public void onLogoutClick() {
        SessionManager.clearSession(); // Xóa phiên làm việc
        Stage stage = (Stage) pendingTable.getScene().getWindow();
        SceneManager.switchScene(stage, "/com/uet/auction/view/Login.fxml");
    }
}