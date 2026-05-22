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

    @FXML private Label lblProductName, lblCurrentPrice, lblDescription, lblTimeRemaining,lblStepPrice;
    @FXML private Label lblSellerName, lblTopBidder;
    @FXML private TableView<BidDTO> tblRecentBids;
    @FXML private TableColumn<BidDTO, String> colUser, colBidTime;
    @FXML private TableColumn<BidDTO, Double> colBidPrice;
    @FXML private TextField txtBidAmount;
    @FXML private ImageView imgProduct;

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
        if (product.getEndTime() != null) {
            startCountdown(product.getEndTime());
        } else {
            if (lblTimeRemaining != null) lblTimeRemaining.setText("Hết hạn: —");
        }
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
        // Dừng bộ đếm cũ nếu có trước khi chạy bộ mới
        if (countdown != null) {
            countdown.stop();
        }

        if (endTime == null) {
            if (lblTimeRemaining != null) lblTimeRemaining.setText("Thời gian: Không xác định");
            return;
        }

        countdown = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            LocalDateTime now = LocalDateTime.now();

            // Nếu thời gian hiện tại đã vượt qua thời gian kết thúc
            if (now.isAfter(endTime)) {
                if (lblTimeRemaining != null) {
                    lblTimeRemaining.setText("⏰ Phiên đấu giá đã kết thúc");
                    lblTimeRemaining.setStyle("-fx-text-fill: #7f8c8d; -fx-font-weight: bold; -fx-font-size: 14;");
                }
                txtBidAmount.setDisable(true); // Khóa ô nhập giá
                countdown.stop();
                return;
            }

            // Tính toán số ngày, giờ, phút, giây còn lại
            long totalSeconds = ChronoUnit.SECONDS.between(now, endTime);
            long days    = totalSeconds / 86400;
            long hours   = (totalSeconds % 86400) / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            long secs    = totalSeconds % 60;

            String timeString;
            if (days > 0) {
                timeString = String.format("Còn lại: %d Ngày %02d:%02d:%02d", days, hours, minutes, secs);
            } else {
                timeString = String.format("Còn lại: %02d:%02d:%02d", hours, minutes, secs);
            }

            // Cập nhật lên giao diện
            if (lblTimeRemaining != null) {
                lblTimeRemaining.setText(timeString);

                // Hiệu ứng: Đổi màu sang đỏ khi còn dưới 1 tiếng (3600 giây)
                if (totalSeconds < 3600) {
                    lblTimeRemaining.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 14;");
                } else {
                    lblTimeRemaining.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold; -fx-font-size: 14;"); // Màu xanh
                }
            }
        }));

        countdown.setCycleCount(Timeline.INDEFINITE); // Chạy lặp lại vô hạn
        countdown.play(); // Bắt đầu đếm
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
        String bidText = txtBidAmount.getText();
        if (bidText == null || bidText.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Vui lòng nhập mức giá bạn muốn đặt!");
            return;
        }

        try {
            double bidAmount = Double.parseDouble(bidText.replace(",", "").trim());

            // SỬ DỤNG SESSION MANAGER CỦA CLIENT
            UserDTO sessionUser = SessionManager.getCurrentUser();

            if (sessionUser == null) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Phiên đăng nhập không hợp lệ, vui lòng đăng nhập lại!");
                return;
            }

            // 1. KIỂM TRA QUYỀN bằng biến sessionUser
            String role = sessionUser.getRole();
            if ("SELLER".equals(role) || "ADMIN".equals(role)) {
                showAlert(Alert.AlertType.ERROR, "Từ chối truy cập", "Quản trị viên và Người bán không được phép tham gia đặt giá!");
                return; // Dừng lại, không cho đặt
            }

            if (currentProduct != null) {
                double currentPrice = currentProduct.getCurrentPrice();
                double stepPrice = currentProduct.getStepPrice(); // Lấy bước giá của sản phẩm
                double minValidPrice = currentPrice + stepPrice;  // Giá hợp lệ tối thiểu

                // 2. KIỂM TRA BƯỚC GIÁ
                if (bidAmount < minValidPrice) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi đặt giá",
                            String.format("Mức giá đặt không hợp lệ!\nGiá hiện tại: %,.0f VNĐ\nBước giá: %,.0f VNĐ\n=> Bạn phải đặt tối thiểu: %,.0f VNĐ",
                                    currentPrice, stepPrice, minValidPrice));
                    return;
                }

                // 3. KIỂM TRA CÓ ĐANG GIỮ ĐỈNH HAY KHÔNG bằng biến sessionUser
                if (sessionUser.getUsername().equals(currentProduct.getOwnerName())) {
                    showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Bạn đang giữ mức giá cao nhất, không thể tự đặt thêm!");
                    return;
                }

                // Nếu thỏa mãn hết thì gửi Request lên Server bằng biến sessionUser
                AuctionRequest request = new AuctionRequest("PLACE_BID", new Object[]{currentProduct.getId(), sessionUser.getUsername(), bidAmount});
                SocketClient.sendRequest(request);

                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã gửi yêu cầu đặt giá: " + String.format("%,.0f VNĐ", bidAmount));
                txtBidAmount.clear();
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không tìm thấy thông tin sản phẩm!");
            }

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi định dạng", "Vui lòng chỉ nhập số hợp lệ!");
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