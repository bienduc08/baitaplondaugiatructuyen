package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.common.DTO.BidDTO;
import com.uet.auction.common.Request.AuctionRequest;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.util.List;

public class BidHistoryController {

    @FXML private Label productNameLabel;
    @FXML private Label totalBidsLabel;

    @FXML private TableView<BidDTO> bidHistoryTable;
    @FXML private TableColumn<BidDTO, String> timeCol;
    @FXML private TableColumn<BidDTO, String> bidderCol;
    @FXML private TableColumn<BidDTO, Double> priceCol;
    @FXML private TableColumn<BidDTO, String> statusCol;

    private final ObservableList<BidDTO> bidListData = FXCollections.observableArrayList();
    private Integer currentProductId;

    // Instance tĩnh để ResponseListener có thể gọi displayBidHistory()
    public static BidHistoryController instance;

    @FXML
    public void initialize() {
        instance = this;

        timeCol.setCellValueFactory(new PropertyValueFactory<>("time"));
        bidderCol.setCellValueFactory(new PropertyValueFactory<>("bidderName"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        bidHistoryTable.setItems(bidListData);
    }

    /**
     * Được gọi từ màn hình Admin/Seller/User trước khi mở cửa sổ này.
     * Nhận ID và tên sản phẩm, sau đó tự gửi request lấy lịch sử.
     */
    public void setProductContext(Integer productId, String productName) {
        this.currentProductId = productId;
        productNameLabel.setText("Tên sản phẩm: " + productName);
        loadBidHistory();
    }

    /**
     * Gửi request GET_BID_HISTORY tới Server.
     * ResponseListener sẽ nhận kết quả và gọi displayBidHistory().
     */
    private void loadBidHistory() {
        if (currentProductId == null) return;
        bidListData.clear();
        totalBidsLabel.setText("Đang tải...");
        SocketClient.sendRequest(new AuctionRequest("GET_BID_HISTORY", currentProductId));
    }

    /**
     * Được gọi bởi ResponseListener khi Server trả về dữ liệu lịch sử.
     */
    public void displayBidHistory(List<BidDTO> list) {
        Platform.runLater(() -> {
            bidListData.clear();
            bidListData.addAll(list);
            totalBidsLabel.setText("Tổng cộng: " + list.size() + " lượt trả giá");
        });
    }

    @FXML
    private void onRefreshClick() {
        loadBidHistory();
    }

    @FXML
    private void onCloseClick() {
        instance = null; // Xóa instance khi đóng cửa sổ
        Stage currentStage = (Stage) bidHistoryTable.getScene().getWindow();
        currentStage.close();
    }
}