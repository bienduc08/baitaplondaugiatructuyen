package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.AlertHelper;
import com.uet.auction.client.util.SessionManager;
import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.common.Request.AuctionRequest;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class ProductItemController {

    @FXML private Label nameLabel;
    @FXML private Label priceLabel;
    @FXML private Label sellerLabel;
    @FXML private Label ownerLabel;
    @FXML private Label timeLabel;
    @FXML private TextField bidInput;
    @FXML private Label descriptionLabel;

    private ProductDTO currentProduct;
    private Timeline countdown;

    public void setData(ProductDTO product) {
        this.currentProduct = product;

        nameLabel.setText(product.getName());
        priceLabel.setText(String.format("%,.0f VNĐ", product.getCurrentPrice()));
        sellerLabel.setText("Người bán: " + (product.getSellerName() != null ? product.getSellerName() : "—"));
        if (descriptionLabel != null) {
            String desc = product.getDescription();
            descriptionLabel.setText((desc != null && !desc.isBlank()) ? desc : "");
            descriptionLabel.setVisible(desc != null && !desc.isBlank());
            descriptionLabel.setManaged(desc != null && !desc.isBlank());
        }

        String owner = (product.getOwnerName() != null && !product.getOwnerName().isBlank())
                ? product.getOwnerName() : "Chưa có ai";
        ownerLabel.setText("Đang giữ đỉnh: " + owner);

        // Khởi động đếm ngược nếu có end_time
        if (product.getEndTime() != null) {
            startCountdown(product.getEndTime());
        } else {
            timeLabel.setText("Hết hạn: —");
        }
    }

    /**
     * Đếm ngược thời gian còn lại — cập nhật mỗi giây.
     * Thay thế việc chỉ hiển thị ngày tĩnh.
     */
    private void startCountdown(LocalDateTime endTime) {
        // Hủy countdown cũ nếu có (tránh leak khi card được dùng lại)
        if (countdown != null) countdown.stop();

        countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(endTime)) {
                timeLabel.setText("⏰ Đã kết thúc");
                timeLabel.setStyle("-fx-text-fill: #e74c3c;");
                countdown.stop();
                return;
            }
            long totalSeconds = ChronoUnit.SECONDS.between(now, endTime);
            long days    = totalSeconds / 86400;
            long hours   = (totalSeconds % 86400) / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            long secs    = totalSeconds % 60;

            String text;
            if (days > 0) {
                text = String.format("⏱ Còn %d ngày %02d:%02d:%02d", days, hours, minutes, secs);
            } else {
                text = String.format("⏱ Còn %02d:%02d:%02d", hours, minutes, secs);
            }
            timeLabel.setText(text);
            // Tô đỏ khi còn dưới 1 tiếng
            timeLabel.setStyle(totalSeconds < 3600
                    ? "-fx-text-fill: #e74c3c; -fx-font-weight: bold;"
                    : "-fx-text-fill: #27ae60;");
        }));
        countdown.setCycleCount(Timeline.INDEFINITE);
        countdown.play();
    }

    @FXML
    public void onBidButtonClick() {
        String text = bidInput.getText().trim();
        if (text.isEmpty()) {
            AlertHelper.showError("Vui lòng nhập số tiền muốn đặt!");
            return;
        }

        // SỬA: xóa dấu phẩy/chấm phân cách ngàn trước khi parse
        // Người dùng có thể nhập "22,000,000" hoặc "22.000.000"
        String cleanText = text.replace(",", "");

        try {
            double bidAmount = Double.parseDouble(cleanText);

            if (bidAmount <= 0) {
                AlertHelper.showError("Số tiền phải lớn hơn 0!");
                return;
            }

            double currentPrice = currentProduct.getCurrentPrice();
            if (bidAmount <= currentPrice) {
                AlertHelper.showError(String.format(
                        "Giá đặt phải LỚN HƠN giá hiện tại!\nGiá hiện tại: %,.0f VNĐ\nBạn nhập: %,.0f VNĐ",
                        currentPrice, bidAmount));
                return;
            }

            String currentUser = SessionManager.getCurrentUsername();
            if (currentUser == null) {
                AlertHelper.showError("Bạn cần đăng nhập để đặt giá!");
                return;
            }

            // Disable nút để tránh double-click gửi 2 lần
            javafx.scene.Node source = bidInput.getParent();
            Object[] bidData = new Object[]{currentProduct.getId(), currentUser, bidAmount};
            SocketClient.sendRequest(new AuctionRequest("PLACE_BID", bidData));
            bidInput.clear();

        } catch (NumberFormatException e) {
            AlertHelper.showError("Số tiền không hợp lệ!\nVui lòng chỉ nhập số (VD: 25000000)");
        }
    }

    @FXML
    public void onViewHistoryClick() {
        if (currentProduct == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/uet/auction/view/BidHistoryView.fxml"));
            Parent root = loader.load();
            BidHistoryController ctrl = loader.getController();
            ctrl.setProductContext(currentProduct.getId(), currentProduct.getName());

            Stage stage = new Stage();
            stage.setTitle("Lịch sử đấu giá — " + currentProduct.getName());
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            AlertHelper.showError("Không thể mở lịch sử đấu giá!");
        }
    }
}