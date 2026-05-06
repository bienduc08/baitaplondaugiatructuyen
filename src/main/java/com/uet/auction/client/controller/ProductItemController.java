package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.AlertHelper;
import com.uet.auction.client.util.SessionManager;
import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.common.Request.AuctionRequest;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import java.time.format.DateTimeFormatter;

public class ProductItemController {
    @FXML private Label nameLabel;
    @FXML private Label priceLabel;
    @FXML private Label sellerLabel;
    @FXML private Label ownerLabel;
    @FXML private Label timeLabel;
    @FXML private TextField bidInput;

    private ProductDTO currentProduct;

    // Đổ dữ liệu từ UserController vào Thẻ sản phẩm này
    public void setData(ProductDTO product) {
        this.currentProduct = product;
        nameLabel.setText(product.getName());
        priceLabel.setText(String.format("%,.0f VNĐ", product.getCurrentPrice()));
        sellerLabel.setText("Người bán: " + product.getSellerName());

        String owner = product.getOwnerName() != null ? product.getOwnerName() : "Chưa có ai";
        ownerLabel.setText("Đang giữ đỉnh: " + owner);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        if (product.getEndTime() != null) {
            timeLabel.setText("Hết hạn: " + product.getEndTime().format(formatter));
        }
    }

    @FXML
    public void onBidButtonClick() {
        try {
            double bidAmount = Double.parseDouble(bidInput.getText());

            if (bidAmount <= currentProduct.getCurrentPrice()) {
                AlertHelper.showError("Giá đặt phải LỚN HƠN giá hiện tại!");
                return;
            }

            String currentUser = SessionManager.getCurrentUsername();
            if (currentUser == null) {
                AlertHelper.showError("Bạn cần đăng nhập!");
                return;
            }

            // Gửi dữ liệu: [ID Sản phẩm, Tên người mua, Giá tiền]
            Object[] bidData = new Object[]{currentProduct.getId(), currentUser, bidAmount};
            SocketClient.sendRequest(new AuctionRequest("PLACE_BID", bidData));

            bidInput.clear(); // Xóa ô nhập sau khi gửi

        } catch (NumberFormatException e) {
            AlertHelper.showError("Vui lòng nhập một số tiền hợp lệ!");
        }
    }
}