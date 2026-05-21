package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.AlertHelper;
import com.uet.auction.client.util.SessionManager;
import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.common.Request.AuctionRequest;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class SellerMyProductsController extends TableController<ProductDTO> {

    public static SellerMyProductsController instance;

    @FXML private TextField               txtSearch;
    @FXML private ComboBox<String>        cbStatusFilter;

    @FXML private TableView<ProductDTO>            tblMyAuctions;
    @FXML private TableColumn<ProductDTO, Integer> colId;
    @FXML private TableColumn<ProductDTO, String>  colName;
    @FXML private TableColumn<ProductDTO, Double>  colCurrentPrice;
    @FXML private TableColumn<ProductDTO, Integer> colBids;
    @FXML private TableColumn<ProductDTO, String>  colStatus;
    @FXML private TableColumn<ProductDTO, Void>    colActions;

    private FilteredList<ProductDTO> filteredList;

    @FXML
    public void initialize() {
        instance = this;

        // Sử dụng tableData từ class cha (TableController) thay vì tạo masterList riêng
        filteredList = new FilteredList<>(tableData, p -> true);
        if (tblMyAuctions != null) tblMyAuctions.setItems(filteredList);

        setupTable();
        setupFilters();
        loadData();
    }

    @Override
    protected void setupTable() {
        if (colId   != null) colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        if (colName != null) colName.setCellValueFactory(new PropertyValueFactory<>("name"));

        if (colCurrentPrice != null) {
            colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
            colCurrentPrice.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(Double v, boolean empty) {
                    super.updateItem(v, empty);
                    setText(empty || v == null ? null : String.format("%,.0f VNĐ", v));
                }
            });
        }

        if (colBids != null) {
            colBids.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(Integer v, boolean empty) {
                    super.updateItem(v, empty);
                    setText(empty ? null : (v != null ? String.valueOf(v) : "—"));
                }
            });
        }

        if (colStatus != null) {
            colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
            colStatus.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String s, boolean empty) {
                    super.updateItem(s, empty);
                    if (empty || s == null) { setText(null); setStyle(""); return; }
                    switch (s) {
                        case "PENDING":  setText("⏳ Chờ duyệt"); setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;"); break;
                        case "OPEN":     setText("🔥 Đang đấu");  setStyle("-fx-text-fill: #2980b9; -fx-font-weight: bold;"); break;
                        case "CLOSED":   setText("🔒 Đã đóng");   setStyle("-fx-text-fill: #7f8c8d;"); break;
                        case "REJECTED": setText("✘ Từ chối");    setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;"); break;
                        default:         setText(s);               setStyle(""); break;
                    }
                }
            });
        }

        if (colActions != null) {
            colActions.setCellFactory(col -> new TableCell<>() {
                private final Button btn = new Button("Lịch sử bid");
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
    }

    private void setupFilters() {
        if (cbStatusFilter != null) {
            cbStatusFilter.getItems().addAll("Tất cả", "PENDING", "OPEN", "CLOSED", "REJECTED");
            cbStatusFilter.setValue("Tất cả");
            cbStatusFilter.valueProperty().addListener((obs, o, n) -> applyFilter());
        }
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((obs, o, n) -> applyFilter());
        }
    }

    private void applyFilter() {
        String kw     = txtSearch     != null ? txtSearch.getText().trim().toLowerCase() : "";
        String status = cbStatusFilter != null ? cbStatusFilter.getValue() : "Tất cả";

        filteredList.setPredicate(p -> {
            boolean matchStatus = "Tất cả".equals(status) || status == null || status.equals(p.getStatus());
            boolean matchKw     = kw.isEmpty()
                    || p.getName().toLowerCase().contains(kw)
                    || String.valueOf(p.getId()).contains(kw);
            return matchStatus && matchKw;
        });
    }

    @Override
    protected void loadData() {
        String username = SessionManager.getCurrentUsername();
        if (username != null)
            SocketClient.sendRequest(new AuctionRequest("GET_MY_PRODUCTS", username));
    }

    @Override
    public void updateTable(List<ProductDTO> items) {
        super.updateTable(items); // Gọi logic setAll của class cha
        javafx.application.Platform.runLater(this::applyFilter); // Đảm bảo bộ lọc áp dụng ngay sau khi dữ liệu mới nạp vào
    }

    // Giữ lại hàm này để đảm bảo ResponseListener không bị lỗi nếu nó đang gọi thẳng tới displayMyProducts
    public void displayMyProducts(List<ProductDTO> products) {
        updateTable(products);
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
}