package com.uet.auction.client.network;

import com.uet.auction.client.controller.AdminController;
import com.uet.auction.client.controller.RegisterController;
import com.uet.auction.common.Response.AuctionResponse;
import com.uet.auction.client.controller.LoginController;
import com.uet.auction.client.controller.UserController;
import javafx.application.Platform;

import java.io.ObjectInputStream;

public class ResponseListener implements Runnable {
    private ObjectInputStream in;

    public ResponseListener(ObjectInputStream in) {
        this.in = in;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Object obj = in.readObject();
                if (obj instanceof AuctionResponse) {
                    AuctionResponse res = (AuctionResponse) obj;
                    String type = res.getType(); // Hãy thêm trường "type" vào AuctionResponse giống như AuctionRequest

                    switch (type) {
                        case "LOGIN_RESULT":
                            if (LoginController.instance != null) {
                                LoginController.instance.handleLoginResponse(res.isSuccess(), res.getMessage());
                            }
                            break;
                        case "PRODUCTS_DATA":
                        case "UPDATE_PRICE":
                            if (UserController.instance != null) {
                                UserController.instance.loadProductsFromServer();
                            }
                            break;
                        case "REGISTER_RESULT":
                            if (RegisterController.instance != null) {
                                RegisterController.instance.handleRegisterResponse(res.isSuccess(), res.getMessage());
                            }
                            break;
                        case "ADD_PRODUCT_RESULT":
                            if (AdminController.instance != null) {
                                AdminController.instance.handleAdminResponse(type, res.isSuccess(), res.getMessage());
                            }
                            break;
                        case "UPDATE_PRICE":
                            // Khi Server báo có người vừa đặt giá mới hoặc Admin vừa duyệt bài
                            // Kêu UserController tải lại danh sách sản phẩm ngay lập tức!
                            Platform.runLater(() -> {
                                if (UserController.instance != null) {
                                    UserController.instance.loadProducts();
                                }
                            });
                            break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Mất kết nối với Server.");
        }
    }
}