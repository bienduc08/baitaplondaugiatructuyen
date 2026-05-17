package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.SessionManager;
import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.common.Request.AuctionRequest;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.util.List;

public class SellerMyProductsController {
    public static SellerMyProductsController instance; // THÊM DÒNG NÀY
    @FXML private TableView<ProductDTO> tblMyProducts;
    @FXML private TableColumn<ProductDTO, Integer> colId;
    @FXML private TableColumn<ProductDTO, String> colName;
    @FXML private TableColumn<ProductDTO, Double> colStartPrice;
    @FXML private TableColumn<ProductDTO, Double> colCurrentPrice;
    @FXML private TableColumn<ProductDTO, String> colStatus;

    private final ObservableList<ProductDTO> productList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        instance = this;
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        tblMyProducts.setItems(productList);

        // Gọi API lấy dữ liệu ngay khi vừa load form
        String username = SessionManager.getCurrentUsername();
        if (username != null) {
            SocketClient.sendRequest(new AuctionRequest("GET_MY_PRODUCTS", username));
        }
    }

    // Server trả về gọi hàm này (thông qua ResponseListener)
    public void displayMyProducts(List<ProductDTO> list) {
        Platform.runLater(() -> productList.setAll(list));
    }
}