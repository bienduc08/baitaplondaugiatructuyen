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
import javafx.scene.image.ImageView;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
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
        this.currentProduct = product;

        lblProductName.setText(product.getName());
        lblDescription.setText(product.getDescription());
        lblCurrentPrice.setText(String.format("%,.0f VNĐ", product.getCurrentPrice()));
        lblStepPrice.setText(String.format("%,.0f VNĐ", product.getStepPrice()));

        if (lblSellerName != null) {
            lblSellerName.setText(product.getSellerName() != null ? product.getSellerName() : "—");
        }
        if (lblTopBidder != null) {
            lblTopBidder.setText(product.getOwnerName() != null && !product.getOwnerName().isBlank() ? product.getOwnerName() : "Chưa có");
        }

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

        // Nạp dữ liệu lịch sử đấu giá (nếu có)
        if (allBids != null && !allBids.isEmpty()) {
            List<BidDTO> sortedBids = allBids.stream()
                    .sorted((b1, b2) -> {
                        if (b1.getTime() == null && b2.getTime() == null) return 0;
                        if (b1.getTime() == null) return 1;
                        if (b2.getTime() == null) return -1;
                        return b2.getTime().compareTo(b1.getTime());
                    })
                    .collect(Collectors.toList());
            recentBidsList.setAll(sortedBids);
        } else {
            recentBidsList.clear();
        }


        if (product.getEndTime() != null) {
            startCountdown(product.getEndTime());
        } else {
            if (lblTimeRemaining != null) lblTimeRemaining.setText("Hết hạn: —");
        }
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

        if (countdown != null) {
            countdown.stop();
        }

        if (onBackAction != null) {
            onBackAction.run();
        } else {
            System.err.println("Lỗi: Không có hành động quay lại (onBackAction) nào được định nghĩa!");
        }
    }

    public void reloadProductDetails() {
        if (currentProduct != null) {
            SocketClient.sendRequest(new AuctionRequest("GET_BID_HISTORY", currentProduct.getId()));
        }
    }

    public void displayBidHistory(List<BidDTO> bids) {
        if (bids != null) {
            List<BidDTO> sortedBids = bids.stream()
                    .sorted((b1, b2) -> {
                        if (b1.getTime() == null && b2.getTime() == null) return 0;
                        if (b1.getTime() == null) return 1;
                        if (b2.getTime() == null) return -1;
                        return b2.getTime().compareTo(b1.getTime());
                    })
                    .collect(Collectors.toList());
            recentBidsList.setAll(sortedBids);

            if (!sortedBids.isEmpty()) {
                BidDTO highestBid = sortedBids.get(0);
                if (currentProduct != null) {
                    currentProduct.setCurrentPrice(highestBid.getPrice());
                    currentProduct.setOwnerName(highestBid.getBidderName());
                }

                if (lblCurrentPrice != null) {
                    lblCurrentPrice.setText(String.format("%,.0f VNĐ", highestBid.getPrice()));
                }
                if (lblTopBidder != null) {
                    lblTopBidder.setText(highestBid.getBidderName() != null ? highestBid.getBidderName() : "Chưa có");
                }
            } else {
                if (lblTopBidder != null) {
                    lblTopBidder.setText("Chưa có");
                }
                if (currentProduct != null && lblCurrentPrice != null) {
                    lblCurrentPrice.setText(String.format("%,.0f VNĐ", currentProduct.getStartingPrice()));
                }
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
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    // Hàm hỗ trợ load ảnh mặc định an toàn
    private void loadDefaultImage() {
        // Sửa lại đường dẫn này cho đúng với cấu trúc thư mục resources của bạn
        String defaultImagePath = "/com/uet/auction/images/macdinh.jpg";

        java.io.InputStream is = getClass().getResourceAsStream(defaultImagePath);
        if (is != null) {
            imgProduct.setImage(new javafx.scene.image.Image(is));
        } else {
            // Nếu vẫn không tìm thấy ảnh, in ra dòng cảnh báo thay vì làm sập app
            System.err.println("Cảnh báo: Không tìm thấy ảnh mặc định tại " + defaultImagePath);
        }
    }
}