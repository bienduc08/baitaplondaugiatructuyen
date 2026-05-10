package com.uet.auction.client.network;

import com.uet.auction.client.controller.*;
import com.uet.auction.client.util.AlertHelper;
import com.uet.auction.common.DTO.BidDTO;
import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.common.Response.AuctionResponse;
import javafx.application.Platform;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;

public class ResponseListener implements Runnable {

    private final ObjectInputStream in;

    public ResponseListener(ObjectInputStream in) { this.in = in; }

    @Override
    @SuppressWarnings("unchecked")
    public void run() {
        try {
            while (true) {
                AuctionResponse res = (AuctionResponse) in.readObject();
                String type = res.getType();

                switch (type) {

                    case "LOGIN_RESULT":
                        if (LoginController.instance != null)
                            LoginController.instance.handleLoginResponse(res);
                        break;

                    case "REGISTER_RESULT":
                        if (RegisterController.instance != null)
                            RegisterController.instance.handleRegisterResponse(
                                    res.isSuccess(), res.getMessage());
                        break;

                    case "GET_PRODUCTS_RESULT":
                        List<ProductDTO> products = (List<ProductDTO>) res.getData();
                        Platform.runLater(() -> {
                            // GET_PRODUCTS_RESULT dùng cho cả Admin (PENDING) lẫn User (OPEN)
                            if (AdminController.instance != null)
                                AdminController.instance.updatePendingList(products);
                            if (UserController.instance != null)
                                UserController.instance.displayProducts(products);
                        });
                        break;

                    // Admin load tất cả sản phẩm
                    case "GET_ALL_PRODUCTS_RESULT":
                        List<ProductDTO> allProducts = (List<ProductDTO>) res.getData();
                        Platform.runLater(() -> {
                            if (AdminController.instance != null)
                                AdminController.instance.updatePendingList(allProducts);
                        });
                        break;

                    case "GET_MY_PRODUCTS_RESULT":
                        List<ProductDTO> myProducts = (List<ProductDTO>) res.getData();
                        if (SellerController.instance != null)
                            SellerController.instance.displayMyProducts(myProducts);
                        break;

                    case "ADD_PRODUCT_RESULT":
                        if (SellerController.instance != null) {
                            SellerController.instance.handleAddProductResult(
                                    res.isSuccess(), res.getMessage());
                        } else {
                            Platform.runLater(() -> {
                                if (res.isSuccess()) AlertHelper.showInfo(res.getMessage());
                                else AlertHelper.showError(res.getMessage());
                            });
                        }
                        break;

                    case "CHANGE_STATUS_RESULT":
                        Platform.runLater(() -> {
                            if (res.isSuccess()) AlertHelper.showInfo(res.getMessage());
                            else AlertHelper.showError(res.getMessage());
                        });
                        break;

                    case "BID_RESULT":
                        Platform.runLater(() -> {
                            if (res.isSuccess()) AlertHelper.showInfo("✔ Đặt giá thành công!");
                            else AlertHelper.showError("✘ " + res.getMessage());
                        });
                        break;

                    // Broadcast từ server khi có giá mới hoặc status thay đổi
                    case "UPDATE_PRICE":
                        Platform.runLater(() -> {
                            if (UserController.instance   != null) UserController.instance.loadProducts();
                            if (AdminController.instance  != null) AdminController.instance.loadPendingProducts();
                            if (SellerController.instance != null) SellerController.instance.loadMyProducts();
                        });
                        break;

                    case "GET_BID_HISTORY_RESULT":
                        if (res.isSuccess()) {
                            List<BidDTO> bids = (List<BidDTO>) res.getData();
                            if (BidHistoryController.instance != null)
                                BidHistoryController.instance.displayBidHistory(bids);
                        } else {
                            Platform.runLater(() ->
                                    AlertHelper.showError("Không thể tải lịch sử: " + res.getMessage()));
                        }
                        break;

                    default:
                        System.out.println("Phản hồi không xác định: " + type);
                        break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Mất kết nối tới Server.");
        }
    }
}