package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.AlertHelper;
import com.uet.auction.client.util.SessionManager;
import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.common.Request.AuctionRequest;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Controller cho SellerAddProduct.fxml
 * Cho phép Seller đăng sản phẩm đấu giá với thông tin thời gian chi tiết.
 */
public class SellerAddProductController {

    @FXML private TextField txtProductName;
    @FXML private TextArea  txtDescription;
    @FXML private TextField txtStartingPrice;
    @FXML private TextField txtBidStep;

    @FXML private DatePicker dpStartDate;
    @FXML private TextField  txtStartH;
    @FXML private TextField  txtStartM;
    @FXML private TextField  txtStartS;

    @FXML private DatePicker dpEndDate;
    @FXML private TextField  txtEndH;
    @FXML private TextField  txtEndM;
    @FXML private TextField  txtEndS;
    @FXML private ImageView imgPreview;
    private byte[] selectedImageBytes;

    @FXML
    public void initialize() {
        // Giá trị mặc định đã được đặt trong FXML
    }

    /**
     * Xử lý nút "ĐĂNG SẢN PHẨM" — gửi request lên server.
     * Gọi phương thức này bằng cách gắn onAction="#onSubmitClick" vào nút trong FXML nếu cần.
     */
    @FXML
    public void onSubmitClick() {
        String name     = txtProductName != null ? txtProductName.getText().trim() : "";
        String priceStr = txtStartingPrice != null ? txtStartingPrice.getText().trim() : "";

        if (name.isEmpty() || priceStr.isEmpty()) {
            AlertHelper.showError("Vui lòng nhập tên sản phẩm và giá khởi điểm!");
            return;
        }
        if (dpEndDate == null || dpEndDate.getValue() == null) {
            AlertHelper.showError("Vui lòng chọn ngày kết thúc!");
            return;
        }
        if (SessionManager.getCurrentUsername() == null) {
            AlertHelper.showError("Phiên đăng nhập hết hạn, vui lòng đăng nhập lại!");
            return;
        }

        try {
            double startingPrice = Double.parseDouble(priceStr.replace(",", ""));
            if (startingPrice <= 0) {
                AlertHelper.showError("Giá khởi điểm phải lớn hơn 0!");
                return;
            }

            // BẠN CẦN THÊM ĐOẠN ĐỌC BƯỚC GIÁ NÀY VÀO
            String stepStr = (txtBidStep != null && !txtBidStep.getText().trim().isEmpty()) ? txtBidStep.getText().trim() : "10000";
            double stepPrice = Double.parseDouble(stepStr.replace(",", ""));
            if (stepPrice <= 0) {
                AlertHelper.showError("Bước giá phải lớn hơn 0!");
                return;
            }

            // Đọc giờ/phút/giây kết thúc
            int endH = parseTimeField(txtEndH, 23);
            int endM = parseTimeField(txtEndM, 59);
            int endS = parseTimeField(txtEndS, 0);
            LocalDateTime endTime = LocalDateTime.of(dpEndDate.getValue(), LocalTime.of(endH, endM, endS));

            if (endTime.isBefore(LocalDateTime.now())) {
                AlertHelper.showError("Thời gian kết thúc phải ở tương lai!");
                return;
            }

            // Đọc giờ/phút/giây bắt đầu
            LocalDateTime startTime;
            if (dpStartDate != null && dpStartDate.getValue() != null) {
                int startH = parseTimeField(txtStartH, 0);
                int startM = parseTimeField(txtStartM, 0);
                int startS = parseTimeField(txtStartS, 0);
                startTime = LocalDateTime.of(dpStartDate.getValue(), LocalTime.of(startH, startM, startS));
            } else {
                startTime = LocalDateTime.now();
            }

            if (startTime.isAfter(endTime) || startTime.isEqual(endTime)) {
                AlertHelper.showError("Thời gian bắt đầu phải trước thời gian kết thúc!");
                return;
            }

            ProductDTO product = new ProductDTO();
            product.setName(name);
            product.setStartingPrice(startingPrice);
            product.setCurrentPrice(startingPrice);
            product.setStepPrice(stepPrice);
            if (selectedImageBytes != null) {
                product.setImageBytes(selectedImageBytes);
            }

            product.setDescription(txtDescription != null ? txtDescription.getText().trim() : "");
            product.setSellerName(SessionManager.getCurrentUsername());
            product.setStartTime(startTime);
            product.setEndTime(endTime);
            product.setStatus("PENDING");

            SocketClient.sendRequest(new AuctionRequest("ADD_PRODUCT", product));
            AlertHelper.showInfo("⏳ Đã gửi yêu cầu đăng sản phẩm, chờ Admin duyệt!");
            clearForm();

        } catch (NumberFormatException e) {
            AlertHelper.showError("Giá khởi điểm/Bước giá phải là số hợp lệ (VD: 5000000)!");
        }
    }

    private int parseTimeField(TextField field, int defaultValue) {
        if (field == null) return defaultValue;
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void clearForm() {
        if (txtProductName   != null) txtProductName.clear();
        if (txtDescription   != null) txtDescription.clear();
        if (txtStartingPrice != null) txtStartingPrice.setText("0");
        if (txtBidStep       != null) txtBidStep.setText("10,000");
        if (dpStartDate      != null) dpStartDate.setValue(null);
        if (dpEndDate        != null) dpEndDate.setValue(null);
        if (txtStartH != null) txtStartH.setText("00");
        if (txtStartM != null) txtStartM.setText("00");
        if (txtStartS != null) txtStartS.setText("00");
        if (txtEndH   != null) txtEndH.setText("23");
        if (txtEndM   != null) txtEndM.setText("59");
        if (txtEndS   != null) txtEndS.setText("00");
        if (imgPreview != null) imgPreview.setImage(null);
        selectedImageBytes = null;
    }
    @FXML
    public void onChooseImageClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm");
        // Chỉ cho phép chọn các file định dạng ảnh
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                // Đọc file thành mảng byte để lưu tạm vào biến
                selectedImageBytes = Files.readAllBytes(file.toPath());

                // Hiển thị ảnh lên giao diện cho Seller xem trước
                Image image = new Image(file.toURI().toString());
                if (imgPreview != null) {
                    imgPreview.setImage(image);
                }
            } catch (IOException e) {
                AlertHelper.showError("Lỗi khi đọc file ảnh! Vui lòng thử lại.");
                e.printStackTrace();
            }
        }
    }
}