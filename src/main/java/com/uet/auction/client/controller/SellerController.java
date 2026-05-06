package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.AlertHelper;
import com.uet.auction.client.util.SceneManager;
import com.uet.auction.common.Request.AuctionRequest;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class SellerController {
    @FXML private TextField itemNameField;
    @FXML private TextField startPriceField;
    @FXML private DatePicker startDatePicker; // Thêm vào giao diện FXML
    @FXML private DatePicker endDatePicker;

    public static SellerController instance;
    private String currentSeller = "seller_test";

    @FXML
    public void initialize() {
        instance = this;
    }

    @FXML
    public void onSellItemClick() {
        String name = itemNameField.getText();
        String priceStr = startPriceField.getText();

        if(name.isEmpty() || priceStr.isEmpty()) {
            AlertHelper.showError("Nhập đủ thông tin!");
            return;
        }

        try {
            double price = Double.parseDouble(priceStr);
            // Mặc định cho đấu giá 1 ngày
            Object[] itemData = new Object[]{name, price, 1};
            SocketClient.sendRequest(new AuctionRequest("ADD_PRODUCT", itemData));
        } catch (Exception e) {
            AlertHelper.showError("Giá không hợp lệ");
        }
    }

    @FXML
    public void onLogoutClick() {
        Stage stage = (Stage) itemNameField.getScene().getWindow();
        SceneManager.switchScene(stage, "/com/uet/auction/view/Login.fxml");
    }
    @FXML
    public void onSellItemClick() {
        try {
            String name = itemNameField.getText();
            double price = Double.parseDouble(startPriceField.getText());

            // Lấy ngày bắt đầu và kết thúc (Mặc định thời gian là 00:00, nếu muốn chính xác giờ phút, bạn có thể tự thiết kế thêm TextField nhập Giờ/Phút)
            LocalDateTime startTime = startDatePicker.getValue().atStartOfDay();
            LocalDateTime endTime = endDatePicker.getValue().atTime(LocalTime.MAX);

            if (startTime.isAfter(endTime)) {
                AlertHelper.showError("Thời gian kết thúc phải sau thời gian bắt đầu!");
                return;
            }

            // Gói dữ liệu
            Object[] itemData = new Object[]{name, price, currentSeller, startTime, endTime};
            SocketClient.sendRequest(new AuctionRequest("ADD_PRODUCT", itemData));

        } catch (Exception e) {
            AlertHelper.showError("Vui lòng điền đủ và đúng thông tin!");
        }
    }
}