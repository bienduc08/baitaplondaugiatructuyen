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

                    // TRẢ DỮ LIỆU VỀ TRANG CHỦ (HomecontentController)
                    case "GET_PRODUCTS_RESULT":
                        List<ProductDTO> products = (List<ProductDTO>) res.getData();
                        Platform.runLater(() -> {
                            if (HomecontentController.instance != null)
                                HomecontentController.instance.displayProducts(products);
                        });
                        break;

                    // =========================================================
                    // ĐÃ SỬA: THÊM LOG CHUYÊN DỤNG CHO TRANG DUYỆT SẢN PHẨM ADMIN
                    // =========================================================
                    case "GET_PENDING_PRODUCTS_RESULT":
                    case "GET_ALL_PRODUCTS_RESULT":
                        List<ProductDTO> allProducts = (List<ProductDTO>) res.getData();

                        Platform.runLater(() -> {
                            if (AdminController.instance != null) {
                                AdminController.instance.updatePendingList(allProducts);
                            }
                        });
                        break;

                    // TRẢ DỮ LIỆU VỀ TRANG SẢN PHẨM CỦA SELLER (SellerMyProductsController)
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
                                if (JoinedAuctionsController.instance != null)
                                    JoinedAuctionsController.instance.displayJoinedAuctions(joinedProducts);
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
                                AlertHelper.showInfo(res.getMessage());
                                // Gọi hàm load lại bảng bên AdminController
                                if (AdminController.instance != null) {
                                    AdminController.instance.loadPendingProducts();
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