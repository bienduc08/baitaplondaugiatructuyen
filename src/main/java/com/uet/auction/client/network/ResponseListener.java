package com.uet.auction.client.network;

import com.uet.auction.client.controller.*;
import com.uet.auction.client.util.AlertHelper;
import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.common.DTO.UserDTO;
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
                            if (HomecontentController.instance != null)
                                HomecontentController.instance.displayProducts(products);
                        });
                        break;
                    case "GET_ALL_PRODUCTS_RESULT":
                    case "GET_MY_PRODUCTS_RESULT":
                        List<ProductDTO> myProducts = (List<ProductDTO>) res.getData();
                        Platform.runLater(() -> {
                            if (SellerMyProductsController.instance != null)
                                SellerMyProductsController.instance.displayMyProducts(myProducts);
                        });
                        break;

                    case "GET_JOINED_PRODUCTS_RESULT":
                        if (res.isSuccess()) {
                            List<ProductDTO> joinedProducts = (List<ProductDTO>) res.getData();
                            Platform.runLater(() -> {
                                if (UserAuctionsController.instance != null)
                                    UserAuctionsController.instance.displayJoinedAuctions(joinedProducts);
                            });
                        }
                        break;

                    case "ADD_PRODUCT_RESULT":
                        Platform.runLater(() -> {
                            if (res.isSuccess()) {
                                AlertHelper.showInfo(res.getMessage());
                            } else {
                                AlertHelper.showError(res.getMessage());
                            }
                        });
                        break;

                    case "CHANGE_STATUS_RESULT":
                        Platform.runLater(() -> {
                            if (res.isSuccess()) {
                                // ĐÃ SỬA: Chỉ gọi 1 hàm loadPendingProducts() để làm mới dữ liệu và cập nhật bộ đếm
                                if (AdminPendingController.instance != null) {
                                    AdminPendingController.instance.loadPendingProducts();
                                }
                            } else {
                                AlertHelper.showError(res.getMessage());
                            }
                        });
                        break;

                    case "UPDATE_PRICE":
                        Platform.runLater(() -> {
                            if (HomecontentController.instance != null)
                                HomecontentController.instance.loadProducts();
                            if (AdminPendingController.instance != null)
                                AdminPendingController.instance.refreshPendingProducts();
                        });
                        break;

                    // =========================================================
                    // XỬ LÝ RESPONSE QUẢN LÝ NGƯỜI DÙNG
                    // =========================================================

                    case "GET_ALL_USERS_RESULT":
                    case "SEARCH_USER_RESULT":
                        if (res.isSuccess()) {
                            List<UserDTO> users = (List<UserDTO>) res.getData();
                            Platform.runLater(() -> {
                                if (AdminUserManagementController.instance != null)
                                    AdminUserManagementController.instance.updateTableData(users);
                            });
                        } else {
                            Platform.runLater(() -> AlertHelper.showError(res.getMessage()));
                        }
                        break;

                    case "LOCK_USER_RESULT":
                    case "UNLOCK_USER_RESULT":
                        Platform.runLater(() -> {
                            if (res.isSuccess()) {
                                AlertHelper.showInfo(res.getMessage());
                                if (AdminUserManagementController.instance != null)
                                    AdminUserManagementController.instance.reloadUsers();
                            } else {
                                AlertHelper.showError(res.getMessage());
                            }
                        });
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