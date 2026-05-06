package com.uet.auction.client.controller;

// ... imports

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.client.util.AlertHelper;
import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.common.Request.AuctionRequest;
import com.uet.auction.common.Response.AuctionResponse;

import java.awt.*;

public class ProductItemController {
    @FXML private Label nameLabel;
    @FXML private Label priceLabel;
    @FXML private TextField bidAmountField; // Ô để người dùng nhập giá

    private ProductDTO currentProduct;

    public void setData(ProductDTO product) {
        this.currentProduct = product;
        nameLabel.setText(product.getName());
        priceLabel.setText(String.valueOf(product.getCurrentPrice()) + " VNĐ");

        // Gọi CountdownTask đếm ngược ở đây như đã hướng dẫn trước đó
    }

    @FXML
    public void onBidButtonClick() {
        try {
            double amount = Double.parseDouble(bidAmountField.getText());

            // Giả sử bạn lấy username của người đang đăng nhập từ SessionManager hoặc một biến static
            String username = "user_test"; // TODO: Thay bằng username thật

            // Tạo một DTO hoặc gói dữ liệu gửi đi (Bạn có thể tự tạo BidRequest DTO)
            // Hoặc gửi mảng Object:
            Object[] bidData = new Object[]{currentProduct.getId(), username, amount};
            AuctionRequest request = new AuctionRequest("PLACE_BID", bidData);

            AuctionResponse response = SocketClient.sendRequest(request);

            if (response.isSuccess()) {
                AlertHelper.showInfo("Đặt giá thành công!");
                // Không cần làm gì thêm, ResponseListener sẽ tự nghe thấy lệnh UPDATE_PRICE
                // và tự động bảo UserController load lại giá mới cho mọi người!
            } else {
                AlertHelper.showError(response.getMessage());
            }

        } catch (NumberFormatException e) {
            AlertHelper.showError("Vui lòng nhập số tiền hợp lệ!");
        }
    }
}