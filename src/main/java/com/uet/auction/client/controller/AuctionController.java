package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.AlertHelper;
import com.uet.auction.common.Request.AuctionRequest;
import com.uet.auction.common.Response.AuctionResponse;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.application.Platform;

public class AuctionController {
    @FXML private Label currentPriceLabel;
    @FXML private TextField bidInput;
    private int currentProductId;
    private String currentUser;

    public void initializeAuction(int productId, String username, double price) {
        this.currentProductId = productId;
        this.currentUser = username;
        Platform.runLater(() -> currentPriceLabel.setText(String.valueOf(price)));
    }

    @FXML
    public void onPlaceBid() {
        try {
            double amount = Double.parseDouble(bidInput.getText());
            Object[] bidData = new Object[]{currentProductId, currentUser, amount};

            SocketClient.sendRequest(new AuctionRequest("PLACE_BID", bidData));
            // Đợi ResponseListener xử lý kết quả
        } catch (NumberFormatException e) {
            AlertHelper.showError("Nhập số tiền hợp lệ!");
        }
    }
}