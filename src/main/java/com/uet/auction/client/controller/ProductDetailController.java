package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.SessionManager;
import com.uet.auction.common.DTO.BidDTO;
import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.common.DTO.UserDTO;
import com.uet.auction.common.Request.AuctionRequest;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.io.File;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import java.util.List;
import java.util.stream.Collectors;


public class ProductDetailController {

    public static ProductDetailController instance;

    // THÊM BIẾN NÀY: Để linh hoạt quay lại màn hình trước đó bất kể là User, Admin hay Seller
    public static Runnable onBackAction;

    @FXML
    private Label lblProductName, lblCurrentPrice, lblDescription, lblTimeRemaining, lblStepPrice;
    @FXML
    private Label lblSellerName, lblTopBidder;
    @FXML
    private TableView<BidDTO> tblRecentBids;
    @FXML
    private TableColumn<BidDTO, String> colUser, colBidTime;
    @FXML
    private TableColumn<BidDTO, Double> colBidPrice;
    @FXML
    private TextField txtBidAmount;
    @FXML
    private ImageView imgProduct;

    private final ObservableList<BidDTO> recentBidsList = FXCollections.observableArrayList();
    private ProductDTO currentProduct;
    private Timeline countdown;


    @FXML
    public void initialize() {
        instance = this;
        setupTable();
    }

    public void setProductData(ProductDTO product, List<BidDTO> allBids) {
        dispose();
        this.currentProduct = product;

        lblProductName.setText(product.getName());
        lblDescription.setText(product.getDescription());
        lblCurrentPrice.setText(String.format("%,.0f VNĐ", product.getCurrentPrice()));
        lblStepPrice.setText(String.format("%,.0f VNĐ", product.getStepPrice()));
        lblSellerName.setText(product.getSellerName() != null ? product.getSellerName() : "—");
        lblTopBidder.setText(product.getOwnerName() != null && !product.getOwnerName().isBlank() ? product.getOwnerName() : "Chưa có");

        loadImage(product.getImageUrl());
        updateBidHistory(allBids);

        if (product.getEndTime() != null) startCountdown(product.getEndTime());
        else if (lblTimeRemaining != null) lblTimeRemaining.setText("Hết hạn: —");
    }

    private void loadImage(String imageUrl) {
        if (imgProduct == null) return;
        imgProduct.setImage(null); // Giải phóng ảnh cũ

        if (imageUrl == null || imageUrl.trim().isEmpty() || imageUrl.toLowerCase().contains("macdinh")) {
            loadDefaultImage();
            return;
        }

        File file = new File("images/" + new File(imageUrl.trim()).getName());
        if (file.exists()) {
            // Resize 360x280 đúng khung hình, tắt background loading (false) để tránh tràn heap
            imgProduct.setImage(new Image(file.toURI().toString(), 360, 280, true, true, false));
        } else {
            loadDefaultImage();
        }
    }

    private void updateBidHistory(List<BidDTO> bids) {
        if (bids != null) {
            recentBidsList.setAll(bids.stream()
                    .sorted((b1, b2) -> b2.getTime().compareTo(b1.getTime()))
                    .collect(Collectors.toList()));
        }
    }

    public void dispose() {
        if (countdown != null) {
            countdown.stop();
            countdown = null;
        }
        if (imgProduct != null) {
            imgProduct.setImage(null);
        }
        recentBidsList.clear();
    }


    private void startCountdown(LocalDateTime endTime) {
        countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            long totalSeconds = ChronoUnit.SECONDS.between(LocalDateTime.now(), endTime);
            if (totalSeconds <= 0) {
                if (lblTimeRemaining != null) lblTimeRemaining.setText("⏰ Phiên đấu giá đã kết thúc");
                if (txtBidAmount != null) txtBidAmount.setDisable(true);
                countdown.stop();
                return;
            }
            if (lblTimeRemaining != null) {
                lblTimeRemaining.setText(String.format("Còn lại: %d ngày %02d:%02d:%02d",
                        totalSeconds / 86400, (totalSeconds % 86400) / 3600, (totalSeconds % 3600) / 60, totalSeconds % 60));
                lblTimeRemaining.setStyle(totalSeconds < 3600 ? "-fx-text-fill: #e74c3c; -fx-font-weight: bold;" : "-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
            }
        }));
        countdown.setCycleCount(Timeline.INDEFINITE);
        countdown.play();
    }

    private void setupTable() {
        if (colUser != null) colUser.setCellValueFactory(new PropertyValueFactory<>("userName"));
        if (colBidTime != null) colBidTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        if (colBidPrice != null) {
            colBidPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
            colBidPrice.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(Double amount, boolean empty) {
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
        dispose();
        if (onBackAction != null) onBackAction.run();
    }

    public void reloadProductDetails() {
        if (currentProduct != null) {
            SocketClient.sendRequest(new AuctionRequest("GET_BID_HISTORY", currentProduct.getId()));
        }
    }

    public void displayBidHistory(List<BidDTO> bids) {
        if (bids != null) {
            recentBidsList.setAll(bids.stream()
                    .sorted((b1, b2) -> (b1.getTime() == null ? 1 : b2.getTime().compareTo(b1.getTime())))
                    .collect(Collectors.toList()));

            if (!recentBidsList.isEmpty()) {
                BidDTO top = recentBidsList.get(0);
                if (currentProduct != null) {
                    currentProduct.setCurrentPrice(top.getPrice());
                    currentProduct.setOwnerName(top.getUserName());
                }
                if (lblCurrentPrice != null) lblCurrentPrice.setText(String.format("%,.0f VNĐ", top.getPrice()));
                if (lblTopBidder != null) lblTopBidder.setText(top.getUserName());
            }
        }
    }

    @FXML
    public void onPlaceBidClick() {
        try {
            double bidAmount = Double.parseDouble(txtBidAmount.getText().replace(",", "").trim());
            UserDTO sessionUser = SessionManager.getCurrentUser();

            if (sessionUser == null) return;
            if ("SELLER".equals(sessionUser.getRole()) || "ADMIN".equals(sessionUser.getRole())) {
                showAlert(Alert.AlertType.ERROR, "Từ chối", "Quản trị viên/Người bán không được đấu giá!");
                return;
            }
            if (bidAmount < (currentProduct.getCurrentPrice() + currentProduct.getStepPrice())) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Giá đặt phải lớn hơn giá hiện tại + bước giá!");
                return;
            }
            SocketClient.sendRequest(new AuctionRequest("PLACE_BID", new Object[]{currentProduct.getId(), sessionUser.getUsername(), bidAmount}));
            txtBidAmount.clear();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng nhập số hợp lệ!");
        }
    }

    // Hàm hỗ trợ hiển thị hộp thoại thông báo
    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setContentText(msg);
        a.showAndWait();
    }

    // Hàm hỗ trợ load ảnh mặc định an toàn
    private void loadDefaultImage() {
        try (InputStream is = getClass().getResourceAsStream("/com/uet/auction/images/macdinh.jpg")) {
            if (is != null) imgProduct.setImage(new Image(is));
        } catch (Exception e) {
            System.err.println("Không thể load ảnh mặc định");
        }
    }
}