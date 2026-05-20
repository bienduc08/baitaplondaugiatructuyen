package com.uet.auction.client.network;

import com.uet.auction.client.controller.*;
import com.uet.auction.client.util.AlertHelper;
import com.uet.auction.client.util.SessionManager;
import com.uet.auction.common.DTO.BidDTO;
import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.common.DTO.UserDTO; // [THÊM MỚI] import UserDTO
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
                            if (ProductDetailController.instance != null)
                                ProductDetailController.instance.updateProductFromList(products);
                        });
                        break;

                    case "GET_PENDING_PRODUCTS_RESULT":
                    case "GET_ALL_PRODUCTS_RESULT":
                        List<ProductDTO> allProducts = (List<ProductDTO>) res.getData();
                        Platform.runLater(() -> {
                            if (AdminController.instance != null) {
                                AdminController.instance.updatePendingList(allProducts);
                            }
                        });
                        break;

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
                            if (ProductDetailController.instance != null)
                                ProductDetailController.instance.onPriceUpdateBroadcast();
                            if (JoinedAuctionsController.instance != null)
                                JoinedAuctionsController.instance.reloadJoinedAuctions();
                        });
                        break;

                    case "BID_RESULT":
                        Platform.runLater(() -> {
                            if (res.isSuccess()) {
                                AlertHelper.showInfo(res.getMessage());
                                if (HomecontentController.instance != null)
                                    HomecontentController.instance.loadProducts();
                                if (ProductDetailController.instance != null)
                                    ProductDetailController.instance.refreshAfterBid();
                            } else {
                                AlertHelper.showError(res.getMessage());
                            }
                        });
                        break;

                    case "GET_BID_HISTORY_RESULT":
                        if (res.isSuccess()) {
                            List<BidDTO> bidHistory = (List<BidDTO>) res.getData();
                            Platform.runLater(() -> {
                                if (BidHistoryController.instance != null)
                                    BidHistoryController.instance.displayBidHistory(bidHistory);
                                if (ProductDetailController.instance != null)
                                    ProductDetailController.instance.displayBidHistory(bidHistory);
                            });
                        } else {
                            Platform.runLater(() -> AlertHelper.showError(res.getMessage()));
                        }
                        break;

                    case "GET_MY_BIDS_RESULT":
                        if (res.isSuccess()) {
                            List<BidDTO> myBids = (List<BidDTO>) res.getData();
                            Platform.runLater(() -> {
                                if (ProfileController.instance != null)
                                    ProfileController.instance.displayMyBids(myBids);
                            });
                        } else {
                            Platform.runLater(() -> AlertHelper.showError(res.getMessage()));
                        }
                        break;

                    case "UPGRADE_TO_SELLER_RESULT":
                        Platform.runLater(() -> {
                            if (res.isSuccess()) {
                                AlertHelper.showInfo(res.getMessage());
                                if (ProfileController.instance != null)
                                    ProfileController.instance.handleUpgradeToSellerSuccess();
                            } else {
                                AlertHelper.showError(res.getMessage());
                            }
                        });
                        break;

                    case "DEPOSIT_RESULT":
                        Platform.runLater(() -> {
                            if (res.isSuccess() && res.getData() instanceof Number) {
                                double newBalance = ((Number) res.getData()).doubleValue();
                                if (SessionManager.getCurrentUser() != null) {
                                    SessionManager.getCurrentUser().setBalance(newBalance);
                                }
                                AlertHelper.showInfo(res.getMessage());
                                if (ProfileController.instance != null)
                                    ProfileController.instance.handleDepositSuccess(newBalance);
                                if (UserController.instance != null)
                                    UserController.instance.updateBalance();
                                if (SellerController.instance != null)
                                    SellerController.instance.updateBalance();
                                if (AdminController.instance != null)
                                    AdminController.instance.updateBalance();
                            } else {
                                AlertHelper.showError(res.getMessage());
                                if (ProfileController.instance != null)
                                    ProfileController.instance.handleDepositFailure();
                            }
                        });
                        break;

                    // =========================================================
                    // [THÊM MỚI] XỬ LÝ RESPONSE QUẢN LÝ NGƯỜI DÙNG
                    // 3 case dưới đây hoàn toàn mới, file gốc không có
                    // =========================================================

                    case "GET_ALL_USERS_RESULT": // [THÊM MỚI]
                    case "SEARCH_USER_RESULT":   // [THÊM MỚI]
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

                    case "LOCK_USER_RESULT":   // [THÊM MỚI]
                    case "UNLOCK_USER_RESULT": // [THÊM MỚI]
                        Platform.runLater(() -> {
                            if (res.isSuccess()) {
                                AlertHelper.showInfo(res.getMessage());
                                // Tự động reload lại bảng sau khi thay đổi trạng thái thành công
                                if (AdminUserManagementController.instance != null)
                                    AdminUserManagementController.instance.reloadUsers();
                            } else {
                                AlertHelper.showError(res.getMessage());
                            }
                        });
                        break;

                    // =========================================================
                    // [KẾT THÚC PHẦN THÊM MỚI]
                    // =========================================================

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
