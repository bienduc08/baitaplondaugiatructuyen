package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.AlertHelper;
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

import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class UserAuctionsController {

    public static UserAuctionsController instance;

    @FXML private TableView<ProductDTO> tblJoinedAuctions;
    @FXML private TableColumn<ProductDTO, Integer> colId;
    @FXML private TableColumn<ProductDTO, String> colName;
    @FXML private TableColumn<ProductDTO, BigDecimal> colCurrentPrice;
    @FXML private TableColumn<ProductDTO, String> colOwner;
    @FXML private TableColumn<ProductDTO, String> colEndTime;
    @FXML private TableColumn<ProductDTO, String> colStatus;
    @FXML private TableColumn<ProductDTO, Void> colAction;

    private final ObservableList<ProductDTO> joinedList = FXCollections.observableArrayList();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        instance = this;
        setupTable();
        tblJoinedAuctions.setItems(joinedList);

        reloadJoinedAuctions();
    }

    private void setupTable() {
        if (colId != null) colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        if (colName != null) colName.setCellValueFactory(new PropertyValueFactory<>("name"));

        if (colCurrentPrice != null) {
            colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
            colCurrentPrice.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(BigDecimal price, boolean empty) {
                    super.updateItem(price, empty);
                    setText(empty || price == null ? null : String.format("%,.0f VNĐ", price));
                    setStyle("-fx-font-weight: bold; -fx-text-fill: #e67e22;");
                }
            });
        }

        if (colOwner != null) {
            colOwner.setCellValueFactory(new PropertyValueFactory<>("ownerName"));
            colOwner.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String owner, boolean empty) {
                    super.updateItem(owner, empty);
                    if (empty) { setText(null); return; }
                    String currentUser = SessionManager.getCurrentUsername();
                    if (owner != null && owner.equals(currentUser)) {
                        setText("Bạn (Đang giữ đỉnh)");
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else {
                        setText(owner != null ? owner : "Chưa có");
                        setStyle("-fx-text-fill: #e74c3c;");
                    }
                }
            });
        }

        if (colEndTime != null) {
            colEndTime.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(String s, boolean empty) {
                    super.updateItem(s, empty);
                    if (empty) { setText(null); return; }
                    ProductDTO p = getTableView().getItems().get(getIndex());
                    setText(p != null && p.getEndTime() != null ? p.getEndTime().format(FMT) : "—");
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
                        case "OPEN":   setText("🔥 Đang đấu"); setStyle("-fx-text-fill: #2980b9; -fx-font-weight: bold;"); break;
                        case "CLOSED": setText("🔒 Đã đóng");  setStyle("-fx-text-fill: #7f8c8d;"); break;
                        default:       setText(s); setStyle(""); break;
                    }
                }
            });
        }

        if (colAction != null) {
            colAction.setCellFactory(col -> new TableCell<>() {
                private final Button btnDetail = new Button("Chi tiết");
                private final Button btnChart  = new Button("📈 Biểu đồ");
                private final javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(5, btnDetail, btnChart);
                {
                    btnDetail.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
                    btnChart.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5; -fx-font-size: 11;");
                    btnDetail.setOnAction(e -> {
                        ProductDTO p = getTableView().getItems().get(getIndex());
                        openProductDetail(p);
                    });
                    btnChart.setOnAction(e -> {
                        ProductDTO p = getTableView().getItems().get(getIndex());
                        openBidHistory(p);
                    });
                }
                @Override protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : box);
                }
            });
        }
    }

    public void displayJoinedAuctions(List<ProductDTO> products) {
        Platform.runLater(() -> joinedList.setAll(products));
    }

    private void openBidHistory(ProductDTO product) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/uet/auction/view/BidHistoryView.fxml"));
            Node bidHistoryNode = loader.load();

            BidHistoryController ctrl = loader.getController();
            ctrl.setProductContext(product.getId(), product.getName());

            if (UserController.instance != null && UserController.instance.getMainBorderPane() != null) {
                BorderPane mainPane = UserController.instance.getMainBorderPane();

                Node previousCenterView = mainPane.getCenter();

                BidHistoryController.onBackAction = () -> {
                    mainPane.setCenter(previousCenterView);
                };

                mainPane.setCenter(bidHistoryNode);
            }
        } catch (IOException e) {
            e.printStackTrace();
            AlertHelper.showError("Không thể mở biểu đồ lịch sử đấu giá!");
        }
    }

    private void openProductDetail(ProductDTO product) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/uet/auction/view/ProductDetailContent.fxml"));
            Node detailNode = loader.load();

            ProductDetailController ctrl = loader.getController();
            ctrl.setProductData(product, null);
            ctrl.reloadProductDetails();

            if (UserController.instance != null && UserController.instance.getMainBorderPane() != null) {
                BorderPane mainPane = UserController.instance.getMainBorderPane();
                Node previousCenterView = mainPane.getCenter();

                ProductDetailController.onBackAction = () -> mainPane.setCenter(previousCenterView);

                mainPane.setCenter(detailNode);
            }
        } catch (IOException e) {
            e.printStackTrace();
            AlertHelper.showError("Không thể mở chi tiết sản phẩm!");
        }
    }
    public void reloadJoinedAuctions() {
        String username = SessionManager.getCurrentUsername();
        if (username != null) {
            SocketClient.sendRequest(new AuctionRequest("GET_JOINED_PRODUCTS", username));
        }
    }
}