package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.AlertHelper;
import com.uet.auction.client.util.SessionManager;
import com.uet.auction.common.DTO.BidDTO;
import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.common.Request.AuctionRequest;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

public class ProductDetailController {

    public static ProductDetailController instance;
    public static Runnable onBackAction;

    private static final double MIN_BID_STEP = 50_000;

    @FXML private Label lblProductName, lblCurrentPrice, lblDescription, lblTimeRemaining;
    @FXML private Label lblSellerName, lblTopBidder, lblMinStep;
    @FXML private TextField txtBidAmount;
    @FXML private Button btnPlaceBid;
    @FXML private TableView<BidDTO> tblRecentBids;
    @FXML private TableColumn<BidDTO, String> colUser, colBidTime;
    @FXML private TableColumn<BidDTO, Double> colBidPrice;

    private final ObservableList<BidDTO> recentBidsList = FXCollections.observableArrayList();
    private ProductDTO currentProduct;
    private Timeline countdown;

    @FXML
    public void initialize() {
        instance = this;
        setupTable();
        if (lblMinStep != null) {
            lblMinStep.setText(String.format("%,.0f VNĐ", MIN_BID_STEP));
        }
    }

    public void setProductData(ProductDTO product, List<BidDTO> allBids) {
        this.currentProduct = product;
        refreshProductUI();

        if (allBids != null && !allBids.isEmpty()) {
            displayBidHistory(allBids);
        } else {
            recentBidsList.clear();
            loadBidHistory();
        }
    }

    private void refreshProductUI() {
        if (currentProduct == null) return;

        lblProductName.setText(currentProduct.getName());
        lblDescription.setText(currentProduct.getDescription() != null ? currentProduct.getDescription() : "");
        lblCurrentPrice.setText(String.format("%,.0f VNĐ", currentProduct.getCurrentPrice()));

        String seller = currentProduct.getSellerName() != null ? currentProduct.getSellerName() : "—";
        String leader = (currentProduct.getOwnerName() != null && !currentProduct.getOwnerName().isBlank())
                ? currentProduct.getOwnerName() : "Chưa có";
        updateStatus(seller, leader);

        if (currentProduct.getEndTime() != null) {
            startCountdown(currentProduct.getEndTime());
        } else if (lblTimeRemaining != null) {
            lblTimeRemaining.setText("Còn lại: —");
        }

        updateBidControls();
    }

    private void updateBidControls() {
        if (btnPlaceBid == null || txtBidAmount == null || currentProduct == null) return;

        boolean isClosed = "CLOSED".equals(currentProduct.getStatus());
        String currentUser = SessionManager.getCurrentUsername();
        String role = SessionManager.getCurrentUser() != null ? SessionManager.getCurrentUser().getRole() : null;
        boolean isSeller = "SELLER".equals(role);
        boolean isLeading = currentUser != null && currentUser.equals(currentProduct.getOwnerName());

        boolean canBid = !isClosed && !isSeller && !isLeading && "OPEN".equals(currentProduct.getStatus());
        btnPlaceBid.setDisable(!canBid);
        txtBidAmount.setDisable(!canBid);

        if (isClosed) {
            btnPlaceBid.setText("PHIÊN ĐÃ KẾT THÚC");
        } else if (isLeading) {
            btnPlaceBid.setText("BẠN ĐANG GIỮ ĐỈNH");
        } else if (isSeller) {
            btnPlaceBid.setText("NGƯỜI BÁN KHÔNG ĐƯỢC ĐẤU GIÁ");
        } else {
            btnPlaceBid.setText("ĐẶT GIÁ NGAY");
        }
    }

    private void startCountdown(LocalDateTime endTime) {
        if (countdown != null) countdown.stop();

        countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(endTime)) {
                lblTimeRemaining.setText("⏰ Đã kết thúc");
                lblTimeRemaining.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                if (countdown != null) countdown.stop();
                if (currentProduct != null) currentProduct.setStatus("CLOSED");
                updateBidControls();
                return;
            }
            long totalSeconds = ChronoUnit.SECONDS.between(now, endTime);
            long days    = totalSeconds / 86400;
            long hours   = (totalSeconds % 86400) / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            long secs    = totalSeconds % 60;

            String text = days > 0
                    ? String.format("Còn lại: %d ngày %02d:%02d:%02d", days, hours, minutes, secs)
                    : String.format("Còn lại: %02d:%02d:%02d", hours, minutes, secs);
            lblTimeRemaining.setText(text);
            lblTimeRemaining.setStyle(totalSeconds < 3600
                    ? "-fx-text-fill: #e74c3c; -fx-font-weight: bold;"
                    : "-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        }));
        countdown.setCycleCount(Timeline.INDEFINITE);
        countdown.play();
    }

    public void displayBidHistory(List<BidDTO> list) {
        if (list == null) {
            recentBidsList.clear();
            return;
        }
        List<BidDTO> sortedBids = list.stream()
                .sorted((b1, b2) -> b2.getTime().compareTo(b1.getTime()))
                .collect(Collectors.toList());
        recentBidsList.setAll(sortedBids);
    }

    public void loadBidHistory() {
        if (currentProduct == null) return;
        SocketClient.sendRequest(new AuctionRequest("GET_BID_HISTORY", currentProduct.getId()));
    }

    public void refreshAfterBid() {
        loadBidHistory();
        SocketClient.sendRequest(new AuctionRequest("GET_OPEN_PRODUCTS", null));
        if (txtBidAmount != null) txtBidAmount.clear();
    }

    public void onPriceUpdateBroadcast() {
        loadBidHistory();
        SocketClient.sendRequest(new AuctionRequest("GET_OPEN_PRODUCTS", null));
    }

    public void updateProductFromList(List<ProductDTO> products) {
        if (currentProduct == null || products == null) return;
        products.stream()
                .filter(p -> p.getId() == currentProduct.getId())
                .findFirst()
                .ifPresent(p -> {
                    currentProduct = p;
                    refreshProductUI();
                });
    }

    private void setupTable() {
        if (colUser != null) colUser.setCellValueFactory(new PropertyValueFactory<>("bidderName"));
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
        if (lblSellerName != null) lblSellerName.setText(seller);
        if (lblTopBidder != null) lblTopBidder.setText(leader != null && !leader.isBlank() ? leader : "Chưa có");
    }

    @FXML
    public void onBidButtonClick() {
        if (currentProduct == null || txtBidAmount == null) return;

        String text = txtBidAmount.getText().trim();
        if (text.isEmpty()) {
            AlertHelper.showError("Vui lòng nhập số tiền muốn đặt!");
            return;
        }

        String cleanText = text.replace(",", "");
        try {
            double bidAmount = Double.parseDouble(cleanText);

            if (bidAmount <= 0) {
                AlertHelper.showError("Số tiền phải lớn hơn 0!");
                return;
            }

            String role = SessionManager.getCurrentUser().getRole();
            if ("SELLER".equals(role)) {
                AlertHelper.showError("Người bán không được phép tham gia đấu giá!");
                return;
            }

            String currentUser = SessionManager.getCurrentUsername();
            String currentOwner = currentProduct.getOwnerName();
            if (currentUser != null && currentUser.equals(currentOwner)) {
                AlertHelper.showError("Bạn đang giữ mức giá cao nhất!\nKhông thể đặt thêm cho đến khi bị vượt qua.");
                return;
            }

            double currentPrice = currentProduct.getCurrentPrice();
            if (bidAmount <= currentPrice) {
                AlertHelper.showError(String.format(
                        "Giá đặt phải LỚN HƠN giá hiện tại!\nGiá hiện tại: %,.0f VNĐ\nBạn nhập: %,.0f VNĐ",
                        currentPrice, bidAmount));
                return;
            }

            double balance = SessionManager.getCurrentUser().getBalance();
            if (balance <= bidAmount) {
                AlertHelper.showError(String.format(
                        "Số dư không đủ!\nSố dư hiện tại: %,.0f VNĐ\nGiá bạn muốn đặt: %,.0f VNĐ",
                        balance, bidAmount));
                return;
            }

            Object[] bidData = new Object[]{currentProduct.getId(), currentUser, bidAmount};
            SocketClient.sendRequest(new AuctionRequest("PLACE_BID", bidData));

        } catch (NumberFormatException e) {
            AlertHelper.showError("Số tiền không hợp lệ!\nVui lòng chỉ nhập số (VD: 25000000)");
        }
    }

    @FXML
    public void onBackButtonClick() {
        if (countdown != null) countdown.stop();
        instance = null;
        if (onBackAction != null) {
            onBackAction.run();
        } else {
            System.err.println("Lỗi: Không có hành động quay lại (onBackAction) nào được định nghĩa!");
        }
    }
}
