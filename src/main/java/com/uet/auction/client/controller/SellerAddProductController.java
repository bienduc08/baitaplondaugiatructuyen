package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.AlertHelper;
import com.uet.auction.client.util.SessionManager;
import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.common.Request.AuctionRequest;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class SellerAddProductController {
    @FXML private TextField txtName;
    @FXML private TextField txtStartPrice;
    @FXML private TextArea txtDescription;
    @FXML private DatePicker dpEndDate;

    @FXML
    public void onSubmitClick() {
        try {
            String name = txtName.getText().trim();
            double startPrice = Double.parseDouble(txtStartPrice.getText().trim());
            LocalDateTime endTime = LocalDateTime.of(dpEndDate.getValue(), LocalTime.of(23, 59));

            ProductDTO product = new ProductDTO();
            product.setName(name);
            product.setStartingPrice(startPrice);
            product.setCurrentPrice(startPrice);
            product.setDescription(txtDescription.getText().trim());
            product.setSellerName(SessionManager.getCurrentUsername());
            product.setStartTime(LocalDateTime.now());
            product.setEndTime(endTime);
            product.setStatus("PENDING");

            SocketClient.sendRequest(new AuctionRequest("ADD_PRODUCT", product));
            AlertHelper.showInfo("Đã gửi yêu cầu tạo phiên đấu giá!");

            // Xóa form sau khi gửi
            txtName.clear(); txtStartPrice.clear(); txtDescription.clear(); dpEndDate.setValue(null);

        } catch (Exception e) {
            AlertHelper.showError("Vui lòng nhập đúng định dạng dữ liệu!");
        }
    }

    @FXML public void onCancelClick() {
        txtName.clear(); txtStartPrice.clear(); txtDescription.clear();
    }
}