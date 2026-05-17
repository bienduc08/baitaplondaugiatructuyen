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

                    // TRẢ DỮ LIỆU VỀ TRANG DUYỆT CỦA ADMIN (AdminPendingController)
                    case "GET_ALL_PRODUCTS_RESULT":
                        List<ProductDTO> allProducts = (List<ProductDTO>) res.getData();
                        Platform.runLater(() -> {
                            if (AdminPendingController.instance != null)
                                AdminPendingController.instance.updatePendingList(allProducts);
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
                                // Tự động chuyển tab hoặc load lại danh sách nếu đang ở màn SellerMyProducts
                                if (SellerMyProductsController.instance != null) {
                                    // Gọi lại API lấy danh sách mới
                                    SocketClient.sendRequest(new com.uet.auction.common.Request.AuctionRequest("GET_MY_PRODUCTS", com.uet.auction.client.util.SessionManager.getCurrentUsername()));
                                }
                            } else {
                                AlertHelper.showError(res.getMessage());
                            }
                        });
                        break;

                    case "CHANGE_STATUS_RESULT":
                        Platform.runLater(() -> {
                            if (res.isSuccess()) {
                                AlertHelper.showInfo(res.getMessage());
                                // Cập nhật lại danh sách Admin ngay lập tức
                                if (AdminPendingController.instance != null) {
                                    AdminPendingController.instance.loadPendingProducts();
                                }
                            } else {
                                AlertHelper.showError(res.getMessage());
                            }
                        });
                        break;

                    case "BID_RESULT":
                        Platform.runLater(() -> {
                            if (res.isSuccess()) AlertHelper.showInfo("✔ Đặt giá thành công!");
                            else AlertHelper.showError("✘ " + res.getMessage());
                        });
                        break;

                    // KHI CÓ SỰ THAY ĐỔI GIÁ TỪ BẤT KỲ AI -> RELOAD LẠI CÁC MÀN HÌNH ĐANG MỞ
                    case "UPDATE_PRICE":
                        Platform.runLater(() -> {
                            if (HomecontentController.instance != null)
                                HomecontentController.instance.loadProducts();
                            if (AdminPendingController.instance != null)
                                AdminPendingController.instance.loadPendingProducts();
                            if (SellerMyProductsController.instance != null) {
                                SocketClient.sendRequest(new com.uet.auction.common.Request.AuctionRequest("GET_MY_PRODUCTS", com.uet.auction.client.util.SessionManager.getCurrentUsername()));
                            }
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

                    case "GET_MY_BIDS_RESULT":
                        if (res.isSuccess()) {
                            List<BidDTO> myBids = (List<BidDTO>) res.getData();
                            Platform.runLater(() -> {
                                if (ProfileController.instance != null) {
                                    // Chuyển dữ liệu vào Profile hoặc Bảng JoinedAuctions
                                    // (Tùy thuộc vào bạn thiết kế bảng nào nhận dữ liệu này)
                                }
                            });
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