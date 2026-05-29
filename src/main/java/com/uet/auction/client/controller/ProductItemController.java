package com.uet.auction.client.controller;

import com.uet.auction.client.util.AlertHelper;
import com.uet.auction.client.util.SessionManager;
import com.uet.auction.common.DTO.ProductDTO;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView; // Đã thêm import ImageView
import javafx.stage.Stage;
import java.time.LocalDateTime;


public class ProductItemController {

    @FXML private ImageView imgProduct;
    @FXML private Label nameLabel;
    @FXML private Label priceLabel;
    @FXML private Label sellerLabel;
    @FXML private Label ownerLabel;
    @FXML private Label timeLabel;
    @FXML private TextField bidInput;
    @FXML private Label descriptionLabel;

    private ProductDTO currentProduct;
    private Timeline countdown;
    private String lastTimeText = "";
    private static final java.util.Map<String, Image> imageCache = new java.util.HashMap<>();

    public void setProductData(ProductDTO product) {
        this.currentProduct = product;
        this.lastTimeText = "";

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
            // THÊM DÒNG NÀY: Cập nhật ngay lập tức thay vì chờ Timeline trễ 1 giây
            updateTimeDisplay(product, timeLabel);
            startCountdown();
        } else {
            timeLabel.setText("Hết hạn: —");
        }

        if (imgProduct != null) {
            String newImageUrl = product.getImageUrl();
            if (newImageUrl == null || newImageUrl.trim().isEmpty() || newImageUrl.toLowerCase().contains("macdinh")) {
                // Chỉ load default nếu ảnh hiện tại KHÔNG PHẢI là default
                if (imgProduct.getUserData() == null || !imgProduct.getUserData().equals("default")) {
                    loadDefaultImage();
                    imgProduct.setUserData("default");
                }
            } else {
                String cleanFileName = new java.io.File(newImageUrl.trim()).getName();
                String currentImageUrl = (String) imgProduct.getUserData();

                if (!newImageUrl.equals(currentImageUrl)) {
                    imgProduct.setUserData(newImageUrl);
                    java.io.File file = new java.io.File("images/" + cleanFileName);

                    if (file.exists()) {
                        String fileUri = file.toURI().toString();
                        // Kiểm tra xem ảnh đã có trong cache chưa
                        if (imageCache.containsKey(fileUri)) {
                            imgProduct.setImage(imageCache.get(fileUri));
                        } else {
                            Image img = new Image(fileUri, 300, 300, true, true, false);
                            imageCache.put(fileUri, img);
                            imgProduct.setImage(img);
                        }
                    }
                }
            }
        }

        updateBidInputState();
    }

    public void stopCountdown() {
        if (countdown != null) {
            countdown.stop();
        }
    }

    private void updateBidInputState() {
        if (bidInput == null || currentProduct == null) return;
        String currentUser = SessionManager.getCurrentUsername();
        boolean isOwnProduct = currentUser != null && currentUser.equals(currentProduct.getSellerName());
        boolean isLeading = currentUser != null && currentUser.equals(currentProduct.getOwnerName());
        boolean canBid = "OPEN".equals(currentProduct.getStatus()) && !isOwnProduct && !isLeading;
        bidInput.setDisable(!canBid);
    }

    private void startCountdown() {
        if (countdown != null) countdown.stop();

        countdown = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), e -> {
            updateTimeDisplay(currentProduct, timeLabel);
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
            ctrl.setProductData(currentProduct, java.util.Collections.emptyList());
            ctrl.updateStatus(
                    currentProduct.getSellerName() != null ? currentProduct.getSellerName() : "—",
                    currentProduct.getOwnerName()
            );

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
    private void updateTimeDisplay(ProductDTO product, Label timeLabel) {
        if (product == null || product.getEndTime() == null) return;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = product.getStartTime();
        LocalDateTime end = product.getEndTime();

        String newText;
        String newStyle;

        if (now.isBefore(start)) {
            newText = "Bắt đầu sau: " + formatDuration(java.time.Duration.between(now, start));
            newStyle = "-fx-text-fill: #e67e22; -fx-font-weight: bold;";
        } else if (now.isBefore(end)) {
            newText = "Kết thúc sau: " + formatDuration(java.time.Duration.between(now, end));
            newStyle = "-fx-text-fill: #e74c3c; -fx-font-weight: bold;";
        } else {
            newText = "Đã kết thúc";
            newStyle = "-fx-text-fill: #7f8c8d; -fx-font-weight: bold;";
        }

        // Cập nhật TRỰC TIẾP, không dùng Platform.runLater nữa
        if (!newText.equals(lastTimeText)) {
            lastTimeText = newText;
            timeLabel.setText(newText);
            timeLabel.setStyle(newStyle);
        }
    }

    // Hàm phụ trợ cũng phải nhận tham số là java.time.Duration
    private String formatDuration(java.time.Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        // Luôn giữ định dạng đồng nhất, ví dụ: 00 ngày 00:00:00
        // Điều này làm độ dài chuỗi luôn cố định
        return String.format("%02d ngày %02d:%02d:%02d", days, hours, minutes, seconds);
    }
}