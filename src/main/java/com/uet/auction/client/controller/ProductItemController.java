package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.AlertHelper;
import com.uet.auction.client.util.SessionManager;
import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.common.Request.AuctionRequest;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView; // Đã thêm import ImageView
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class ProductItemController {

    @FXML private Label nameLabel;
    @FXML private Label priceLabel;
    @FXML private Label sellerLabel;
    @FXML private Label ownerLabel;
    @FXML private Label timeLabel;
    @FXML private TextField bidInput;
    @FXML private Label descriptionLabel;

    // ĐÃ THÊM: Khai báo biến imgProduct để hiển thị ảnh
    @FXML private ImageView imgProduct;

    private ProductDTO currentProduct;
    private Timeline countdown;

    public void setProductData(ProductDTO product) {
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
        if (ownerLabel != null) ownerLabel.setText("Đang giữ đỉnh: " + owner);

        if (product.getEndTime() != null) {
            startCountdown(product.getEndTime());
        } else {
            timeLabel.setText("Hết hạn: —");
        }

        // ĐÃ CHUYỂN LOGIC TẢI ẢNH VÀO ĐÚNG HÀM KHỞI TẠO DỮ LIỆU
        if (imgProduct != null) {
            String imageUrl = product.getImageUrl();

            // Lọc bỏ rác đường dẫn (ví dụ: images\sp_...)
            String cleanFileName = (imageUrl != null) ? new java.io.File(imageUrl.trim()).getName() : "";

            if (imageUrl == null || imageUrl.trim().isEmpty() || imageUrl.toLowerCase().contains("macdinh")) {
                loadDefaultImage();
            } else {
                // Trỏ thẳng vào thư mục upload_images trong project
                java.io.File file = new java.io.File("images/" + cleanFileName);

                if (file.exists()) {
                    // Nạp ảnh có giới hạn kích thước (300x300) để tránh tràn bộ nhớ
                    imgProduct.setImage(new javafx.scene.image.Image(file.toURI().toString(), 300, 300, true, true));
                } else {
                    loadDefaultImage();
                }
            }
        }

        updateBidInputState();
    }

    private void updateBidInputState() {
        if (bidInput == null || currentProduct == null) return;
        String currentUser = SessionManager.getCurrentUsername();
        boolean isOwnProduct = currentUser != null && currentUser.equals(currentProduct.getSellerName());
        boolean isLeading = currentUser != null && currentUser.equals(currentProduct.getOwnerName());
        boolean canBid = "OPEN".equals(currentProduct.getStatus()) && !isOwnProduct && !isLeading;
        bidInput.setDisable(!canBid);
    }

    private void startCountdown(LocalDateTime endTime) {
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
            timeLabel.setStyle(totalSeconds < 3600
                    ? "-fx-text-fill: #e74c3c; -fx-font-weight: bold;"
                    : "-fx-text-fill: #27ae60;");
        }));
        countdown.setCycleCount(Timeline.INDEFINITE);
        countdown.play();
    }

    @FXML
    public void onProductCardClick() {
        if (currentProduct == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/uet/auction/view/ProductDetailContent.fxml"));
            Node detailNode = loader.load();

            ProductDetailController ctrl = loader.getController();
            ctrl.setProductData(currentProduct, null);
            // ctrl.reloadProductDetails(); // Nếu bị đỏ dòng này, hãy kiểm tra lại ProductDetailController có hàm này chưa

            javafx.scene.layout.BorderPane activeMainPane = null;

            if (UserController.instance != null && UserController.instance.getMainBorderPane() != null) {
                activeMainPane = UserController.instance.getMainBorderPane();
            } else if (SellerController.instance != null && SellerController.instance.getMainBorderPane() != null) {
                activeMainPane = SellerController.instance.getMainBorderPane();
            } else if (AdminController.instance != null && AdminController.instance.getMainBorderPane() != null) {
                activeMainPane = AdminController.instance.getMainBorderPane();
            }

            if (activeMainPane != null) {
                Node previousCenterView = activeMainPane.getCenter();

                javafx.scene.layout.BorderPane finalActiveMainPane = activeMainPane;
                ProductDetailController.onBackAction = () -> {
                    finalActiveMainPane.setCenter(previousCenterView);
                };

                activeMainPane.setCenter(detailNode);
            } else {
                Stage stage = new Stage();
                stage.setTitle("Chi tiết — " + currentProduct.getName());
                stage.setScene(new Scene((Parent) detailNode));
                stage.show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            AlertHelper.showError("Không thể mở chi tiết sản phẩm!");
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

    @FXML
    public void onBidButtonClick() {
        String text = bidInput.getText().trim();
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

            String currentUser = SessionManager.getCurrentUsername();
            if (currentUser != null && currentUser.equals(currentProduct.getSellerName())) {
                AlertHelper.showError("Bạn không thể đấu giá sản phẩm do chính mình đăng bán!");
                return;
            }

            String currentOwner = currentProduct.getOwnerName();
            if (currentUser != null && currentUser.equals(currentOwner)) {
                AlertHelper.showError("Bạn đang giữ mức giá cao nhất!\nKhông thể đặt thêm cho đến khi bị vượt qua.");
                return;
            }

            double currentPrice = currentProduct.getCurrentPrice();
            double stepPrice    = currentProduct.getStepPrice();
            double minRequired  = currentPrice + stepPrice;
            if (bidAmount < minRequired) {
                AlertHelper.showError(String.format(
                        "Giá đặt phải >= giá hiện tại + bước giá!\nGiá hiện tại: %,.0f VNĐ\nBước giá: %,.0f VNĐ\n=> Tối thiểu phải đặt: %,.0f VNĐ",
                        currentPrice, stepPrice, minRequired));
                return;
            }


            double balance = SessionManager.getCurrentUser().getBalance();
            if (balance < bidAmount) {
                AlertHelper.showError(String.format(
                        "Số dư không đủ!\nSố dư hiện tại: %,.0f VNĐ\nGiá bạn muốn đặt: %,.0f VNĐ",
                        balance, bidAmount));
                return;
            }


            Object[] bidData = new Object[]{currentProduct.getId(), currentUser, bidAmount};
            SocketClient.sendRequest(new AuctionRequest("PLACE_BID", bidData));
            bidInput.clear();

        } catch (NumberFormatException e) {
            AlertHelper.showError("Số tiền không hợp lệ!\nVui lòng chỉ nhập số (VD: 25000000)");
        }
    }

    private void loadDefaultImage() {
        // Nếu ảnh mặc định nằm ngay dưới thư mục resources/images/
        String defaultImagePath = "/com/uet/auction/images/macdinh.jpg";

        java.io.InputStream is = getClass().getResourceAsStream(defaultImagePath);
        if (is != null) {
            imgProduct.setImage(new javafx.scene.image.Image(is));
        } else {
            System.err.println("Cảnh báo: Không tìm thấy ảnh mặc định tại " + defaultImagePath);
        }
    }
}