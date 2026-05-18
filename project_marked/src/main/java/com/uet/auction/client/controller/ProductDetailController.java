package com.uet.auction.client.controller;

import com.uet.auction.common.DTO.BidDTO;
import com.uet.auction.common.DTO.ProductDTO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.util.List;
import java.util.stream.Collectors;

public class ProductDetailController {

    public static ProductDetailController instance;

    // THÊM BIẾN NÀY: Để linh hoạt quay lại màn hình trước đó bất kể là User, Admin hay Seller
    public static Runnable onBackAction;

    @FXML private Label lblProductName, lblCurrentPrice, lblDescription, lblTimeRemaining;
    @FXML private Label lblSellerName, lblTopBidder;
    @FXML private TableView<BidDTO> tblRecentBids;
    @FXML private TableColumn<BidDTO, String> colUser, colBidTime;
    @FXML private TableColumn<BidDTO, Double> colBidPrice;

    private final ObservableList<BidDTO> recentBidsList = FXCollections.observableArrayList();
    private ProductDTO currentProduct;

    @FXML
    public void initialize() {
        instance = this;
        setupTable();
    }

    public void setProductData(ProductDTO product, List<BidDTO> allBids) {
        this.currentProduct = product;

        lblProductName.setText(product.getName());
        lblDescription.setText(product.getDescription());
        lblCurrentPrice.setText(String.format("%,.0f VNĐ", product.getCurrentPrice()));

        // Nạp dữ liệu lịch sử đấu giá (nếu có)
        if (allBids != null && !allBids.isEmpty()) {
            List<BidDTO> sortedBids = allBids.stream()
                    .sorted((b1, b2) -> b2.getTime().compareTo(b1.getTime()))
                    .collect(Collectors.toList());
            recentBidsList.setAll(sortedBids);
        } else {
            recentBidsList.clear();
        }
    }

    private void setupTable() {
        if (colUser != null) colUser.setCellValueFactory(new PropertyValueFactory<>("username"));
        if (colBidTime != null) colBidTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        if (colBidPrice != null) {
            colBidPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
            colBidPrice.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(Double amount, boolean empty) {
                    super.updateItem(amount, empty);
                    setText(empty || amount == null ? null : String.format("%,.0f VNĐ", amount));
                }
            });
        }
        if (tblRecentBids != null) tblRecentBids.setItems(recentBidsList);
    }

    public void updateStatus(String seller, String leader) {
        if (lblSellerName != null) lblSellerName.setText("Người bán: " + seller);
        if (lblTopBidder != null) lblTopBidder.setText("Đang dẫn đầu: " + (leader != null ? leader : "Chưa có"));
    }

    @FXML
    public void onBackButtonClick() {
        // Chạy hành động quay lại đã được controller cha "gài" vào
        if (onBackAction != null) {
            onBackAction.run();
        } else {
            System.err.println("Lỗi: Không có hành động quay lại (onBackAction) nào được định nghĩa!");
        }
    }
}