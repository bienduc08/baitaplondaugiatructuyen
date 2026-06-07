package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.AlertHelper;
import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.common.Request.AuctionRequest;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class SellerEditProductController {
    public static SellerEditProductController instance;
    public static Runnable onCancelAction;

    @FXML private TextField txtName;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtStartingPrice;
    @FXML private TextField txtStepPrice;

    @FXML private ImageView imgPreview;
    @FXML private Label lblImageName;
    @FXML private Button btnRemoveImage;

    @FXML private DatePicker dpStartDate;
    @FXML private DatePicker dpEndDate;
    @FXML private TextField txtStartH, txtStartM, txtStartS;
    @FXML private TextField txtEndH, txtEndM, txtEndS;

    private ProductDTO currentProduct;
    // Dùng đúng tên biến mảng byte giống form Add
    private byte[] selectedImageBytes = null;

    @FXML
    public void initialize() {
        instance = this;
        if (btnRemoveImage != null) {
            btnRemoveImage.setVisible(false);
            btnRemoveImage.setManaged(false);
        }
    }

    /**
     * Hàm nhận dữ liệu từ màn hình danh sách đổ vào các ô input
     */
    public void setProductData(ProductDTO product) {
        this.currentProduct = product;
        txtName.setText(product.getName());
        txtDescription.setText(product.getDescription());
        txtStartingPrice.setText(String.format("%.0f", product.getStartingPrice()));
        txtStepPrice.setText(String.format("%.0f", product.getStepPrice()));

        // Đổ dữ liệu ảnh cũ (Sử dụng đúng hàm getImageBytes())
        if (product.getImageBytes() != null && product.getImageBytes().length > 0) {
            this.selectedImageBytes = product.getImageBytes();
            imgPreview.setImage(new Image(new ByteArrayInputStream(selectedImageBytes)));
            btnRemoveImage.setVisible(true);
            btnRemoveImage.setManaged(true);
        }

        // Đổ dữ liệu thời gian cũ (Dùng thẳng LocalDateTime)
        if (product.getStartTime() != null) {
            LocalDateTime st = product.getStartTime();
            dpStartDate.setValue(st.toLocalDate());
            txtStartH.setText(String.format("%02d", st.getHour()));
            txtStartM.setText(String.format("%02d", st.getMinute()));
            txtStartS.setText(String.format("%02d", st.getSecond()));
        }

        if (product.getEndTime() != null) {
            LocalDateTime et = product.getEndTime();
            dpEndDate.setValue(et.toLocalDate());
            txtEndH.setText(String.format("%02d", et.getHour()));
            txtEndM.setText(String.format("%02d", et.getMinute()));
            txtEndS.setText(String.format("%02d", et.getSecond()));
        }
    }

    /** Xử lý nút Tải ảnh lên (Giống y hệt Form Add) */
    @FXML
    public void onChooseImageClick() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chọn ảnh sản phẩm thay thế");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Ảnh", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );

        Stage stage = (Stage) txtName.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            if (file.length() > 5 * 1024 * 1024) {
                AlertHelper.showError("Ảnh quá lớn! Vui lòng chọn ảnh nhỏ hơn 5MB.");
                return;
            }
            try {
                selectedImageBytes = Files.readAllBytes(file.toPath());
                imgPreview.setImage(new Image(file.toURI().toString()));
                lblImageName.setText(file.getName());
                btnRemoveImage.setVisible(true);
                btnRemoveImage.setManaged(true);
            } catch (IOException e) {
                AlertHelper.showError("Không thể đọc file ảnh: " + e.getMessage());
            }
        }
    }

    /** Xử lý nút Xóa ảnh (Giống y hệt Form Add) */
    @FXML
    public void onRemoveImageClick() {
        selectedImageBytes = null;
        if (imgPreview != null) imgPreview.setImage(null);
        if (lblImageName != null) lblImageName.setText("");
        if (btnRemoveImage != null) {
            btnRemoveImage.setVisible(false);
            btnRemoveImage.setManaged(false);
        }
    }

    @FXML
    private void onCancelClick() {
        if (onCancelAction != null) onCancelAction.run();
    }

    @FXML
    private void onSaveClick() {
        try {
            String name = txtName.getText().trim();
            String desc = txtDescription.getText().trim();
            BigDecimal startPrice = new BigDecimal(txtStartingPrice.getText().trim().replace(",", ""));
            BigDecimal stepPrice = new BigDecimal(txtStepPrice.getText().trim().replace(",", ""));

            if (name.isEmpty() || desc.isEmpty()) {
                AlertHelper.showError("Vui lòng nhập đầy đủ thông tin tên và mô tả!");
                return;
            }
            if (dpStartDate.getValue() == null || dpEndDate.getValue() == null) {
                AlertHelper.showError("Vui lòng chọn ngày bắt đầu và kết thúc!");
                return;
            }

            // Đọc giờ/phút/giây bắt đầu (Dùng LocalDateTime)
            int startH = parseTimeField(txtStartH, 0);
            int startM = parseTimeField(txtStartM, 0);
            int startS = parseTimeField(txtStartS, 0);
            if (startH < 0 || startH > 23 || startM < 0 || startM > 59 || startS < 0 || startS > 59) {
                AlertHelper.showError("Giờ/phút/giây bắt đầu không hợp lệ (Giờ: 0-23, Phút/Giây: 0-59)!");
                return;
            }
            LocalDateTime startTime = LocalDateTime.of(dpStartDate.getValue(), LocalTime.of(startH, startM, startS));

            // Đọc giờ/phút/giây kết thúc (Dùng LocalDateTime)
            int endH = parseTimeField(txtEndH, 23);
            int endM = parseTimeField(txtEndM, 59);
            int endS = parseTimeField(txtEndS, 0);
            if (endH < 0 || endH > 23 || endM < 0 || endM > 59 || endS < 0 || endS > 59) {
                AlertHelper.showError("Giờ/phút/giây kết thúc không hợp lệ (Giờ: 0-23, Phút/Giây: 0-59)!");
                return;
            }
            LocalDateTime endTime = LocalDateTime.of(dpEndDate.getValue(), LocalTime.of(endH, endM, endS));

            if (startTime.isAfter(endTime) || startTime.isEqual(endTime)) {
                AlertHelper.showError("Thời gian bắt đầu phải trước thời gian kết thúc!");
                return;
            }

            // Gán dữ liệu vào DTO
            currentProduct.setName(name);
            currentProduct.setDescription(desc);
            currentProduct.setStartingPrice(startPrice);
            currentProduct.setStepPrice(stepPrice);
            currentProduct.setStartTime(startTime);
            currentProduct.setEndTime(endTime);
            currentProduct.setImageBytes(selectedImageBytes); // Dùng setImageBytes

            // Gửi request UPDATE_PRODUCT lên server
            SocketClient.sendRequest(new AuctionRequest("UPDATE_PRODUCT", currentProduct));

        } catch (NumberFormatException e) {
            AlertHelper.showError("Giá khởi điểm, bước giá và thời gian phải là số hợp lệ!");
        }
    }

    /** Hàm tiện ích hỗ trợ ép kiểu số nguyên cho giờ/phút/giây */
    private int parseTimeField(TextField field, int defaultValue) {
        if (field == null) return defaultValue;
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}