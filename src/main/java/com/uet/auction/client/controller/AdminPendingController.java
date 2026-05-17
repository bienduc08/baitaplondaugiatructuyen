package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.AlertHelper;
import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.common.Request.AuctionRequest;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import java.util.List;

public class AdminPendingController {
    public static AdminPendingController instance;
    @FXML private TableView<ProductDTO> tblPendingProducts;
    @FXML private TableColumn<ProductDTO, Integer> colId;
    @FXML private TableColumn<ProductDTO, String> colName;
    @FXML private TableColumn<ProductDTO, String> colSeller;
    @FXML private TableColumn<ProductDTO, Double> colStartPrice;
    @FXML private TableColumn<ProductDTO, Void> colAction;

    private final ObservableList<ProductDTO> pendingList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        instance = this;
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colSeller.setCellValueFactory(new PropertyValueFactory<>("sellerName"));
        colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));

        setupActionButtons();
        tblPendingProducts.setItems(pendingList);

        // Load danh sách cần duyệt
        SocketClient.sendRequest(new AuctionRequest("GET_ALL_PRODUCTS", null));
    }

    private void setupActionButtons() {
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnApprove = new Button("Duyệt");
            private final Button btnReject = new Button("Từ chối");
            private final HBox pane = new HBox(5, btnApprove, btnReject);

            {
                btnApprove.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white;");
                btnReject.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");

                btnApprove.setOnAction(e -> {
                    ProductDTO p = getTableView().getItems().get(getIndex());
                    SocketClient.sendRequest(new AuctionRequest("CHANGE_PRODUCT_STATUS", new Object[]{p.getId(), "OPEN"}));
                });

                btnReject.setOnAction(e -> {
                    ProductDTO p = getTableView().getItems().get(getIndex());
                    SocketClient.sendRequest(new AuctionRequest("CHANGE_PRODUCT_STATUS", new Object[]{p.getId(), "REJECTED"}));
                });
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    public void updatePendingList(List<ProductDTO> products) {
        // Lọc ra những sản phẩm đang PENDING để hiển thị
        Platform.runLater(() -> {
            pendingList.clear();
            for (ProductDTO p : products) {
                if ("PENDING".equals(p.getStatus())) {
                    pendingList.add(p);
                }
            }
        });
    }
    public void loadPendingProducts() {
        SocketClient.sendRequest(new com.uet.auction.common.Request.AuctionRequest("GET_ALL_PRODUCTS", null));
    }
}