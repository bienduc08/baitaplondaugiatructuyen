package com.uet.auction.client.network;

import com.uet.auction.client.controller.*;
import com.uet.auction.client.util.AlertHelper;
import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.common.DTO.UserDTO;
import com.uet.auction.common.Response.AuctionResponse;
import com.uet.auction.client.util.SessionManager;
import javafx.application.Platform;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigDecimal;
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
                    case "GET_PENDING_PRODUCTS_RESULT":
                        List<ProductDTO> allProducts = (List<ProductDTO>) res.getData();
                        Platform.runLater(() -> {
                            if (AdminController.instance != null) {
                                AdminController.instance.updatePendingList(allProducts);
                            }
                            if (AdminPendingController.instance != null) {
                                AdminPendingController.instance.updateTableData(allProducts);
                            }
                        });
                        break;
                    case "GET_MY_PRODUCTS_RESULT":
                        List<ProductDTO> myProducts = (List<ProductDTO>) res.getData();
                        Platform.runLater(() -> {
                            if (SellerMyProductsController.instance != null) {
                                SellerMyProductsController.instance.displayMyProducts(myProducts);
                            }
                            if (ProfileSellerController.instance != null) {
                                ProfileSellerController.instance.displayMyProducts(myProducts);
                            }
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
                                if (SellerAddProductController.instance != null)
                                    SellerAddProductController.instance.clearFormAfterSuccess();
                            } else {
                                AlertHelper.showError(res.getMessage());
                            }
                        });
                        break;

                    case "CHANGE_STATUS_RESULT":
                        Platform.runLater(() -> {
                            if (res.isSuccess()) {
                                AlertHelper.showInfo("Cập nhật trạng thái thành công!");
                                if (AdminPendingController.instance != null)
                                    AdminPendingController.instance.loadPendingProducts();
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
                                ProductDetailController.instance.reloadProductDetails();
                            if (SellerMyProductsController.instance != null)
                                SellerMyProductsController.instance.loadMyAuctions();
                            String currentUsr = SessionManager.getCurrentUsername();
                            if (currentUsr != null) {
                                SocketClient.sendRequest(new com.uet.auction.common.Request.AuctionRequest("GET_USER_BALANCE", currentUsr));
                            }
                        });
                        break;

                    case "AUCTION_ENDED":
                        String endedMsg = res.getMessage();
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> endedInfo =
                                (java.util.Map<String, Object>) res.getData();

                        Platform.runLater(() -> {
                            if (endedMsg != null) {
                                AlertHelper.showInfo(endedMsg);
                            }

                            if (HomecontentController.instance != null)
                                HomecontentController.instance.loadProducts();
                            if (ProductDetailController.instance != null)
                                ProductDetailController.instance.reloadProductDetails();
                            if (UserAuctionsController.instance != null) {
                                String username = SessionManager.getCurrentUsername();
                                if (username != null)
                                    SocketClient.sendRequest(new com.uet.auction.common.Request.AuctionRequest("GET_JOINED_PRODUCTS", username));
                            }
                        });
                        break;

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

                    case "BID_RESULT":
                        Platform.runLater(() -> {
                            if (res.isSuccess()) {
                                AlertHelper.showInfo(res.getMessage() != null ? res.getMessage() : "Đặt giá thành công!");
                            } else {
                                AlertHelper.showError(res.getMessage() != null ? res.getMessage() : "Đặt giá thất bại!");
                            }
                        });
                        break;

                    case "REGISTER_AUTO_BID_RESULT":
                        Platform.runLater(() -> {
                            if (res.isSuccess()) {
                                AlertHelper.showInfo(res.getMessage() != null ? res.getMessage() : "Đăng ký đấu tự động thành công!");
                            } else {
                                AlertHelper.showError(res.getMessage() != null ? res.getMessage() : "Đăng ký đấu tự động thất bại!");
                            }
                        });
                        break;

                    case "GET_BID_HISTORY_RESULT":
                        if (res.isSuccess()) {
                            java.util.List<com.uet.auction.common.DTO.BidDTO> bidHistory =
                                    (java.util.List<com.uet.auction.common.DTO.BidDTO>) res.getData();
                            Platform.runLater(() -> {
                                if (BidHistoryController.instance != null)
                                    BidHistoryController.instance.displayBidHistory(bidHistory);
                                if (ProductDetailController.instance != null)
                                    ProductDetailController.instance.displayBidHistory(bidHistory);
                            });
                        }
                        break;

                    case "GET_MY_BIDS_RESULT":
                        if (res.isSuccess()) {
                            List<com.uet.auction.common.DTO.BidDTO> myBids =
                                    (List<com.uet.auction.common.DTO.BidDTO>) res.getData();
                            Platform.runLater(() -> {
                                if (ProfileUserController.instance != null)
                                    ProfileUserController.instance.displayMyBids(myBids);
                            });
                        }
                        break;

                    case "GET_PRODUCT_BY_ID_RESULT":
                        if (res.isSuccess() && res.getData() instanceof ProductDTO) {
                            ProductDTO freshProduct = (ProductDTO) res.getData();
                            Platform.runLater(() -> {
                                if (ProductDetailController.instance != null) {
                                    ProductDetailController.instance.updateProductInfo(freshProduct);
                                }
                            });
                        }
                        break;

                    case "DEPOSIT_RESULT":
                        Platform.runLater(() -> {
                            if (res.isSuccess()) {
                                AlertHelper.showInfo(res.getMessage());
                                BigDecimal newBalance = new BigDecimal(((Number) res.getData()).toString());

                                UserDTO current = SessionManager.getCurrentUser();
                                if (current != null) {
                                    current.setBalance(newBalance);
                                }

                                if (UserController.instance != null) UserController.instance.updateBalance();
                                if (ProfileUserController.instance != null) ProfileUserController.instance.handleDepositSuccess(newBalance);

                                if (SellerController.instance != null) SellerController.instance.updateBalance();
                                if (ProfileSellerController.instance != null) ProfileSellerController.instance.handleDepositSuccess(newBalance);

                                if (AdminController.instance != null) AdminController.instance.updateBalance();
                                if (ProfileAdminController.instance != null) ProfileAdminController.instance.handleDepositSuccess(newBalance);

                            } else {
                                AlertHelper.showError(res.getMessage());
                                if (ProfileUserController.instance != null) ProfileUserController.instance.handleDepositFailure();
                                if (ProfileSellerController.instance != null) ProfileSellerController.instance.handleDepositFailure();
                                if (ProfileAdminController.instance != null) ProfileAdminController.instance.handleDepositFailure();
                            }
                        });
                        break;

                    case "GET_USER_BALANCE_RESULT":
                        if (res.isSuccess()) {
                            BigDecimal bal = new BigDecimal(((Number) res.getData()).toString());
                            Platform.runLater(() -> {
                                UserDTO current = SessionManager.getCurrentUser();
                                if (current != null) {
                                    current.setBalance(bal);
                                }

                                if (UserController.instance != null) UserController.instance.updateBalance();
                                if (ProfileUserController.instance != null) ProfileUserController.instance.handleDepositSuccess(bal);

                                if (SellerController.instance != null) SellerController.instance.updateBalance();
                                if (ProfileSellerController.instance != null) ProfileSellerController.instance.handleDepositSuccess(bal);

                                if (AdminController.instance != null) AdminController.instance.updateBalance();
                                if (ProfileAdminController.instance != null) ProfileAdminController.instance.handleDepositSuccess(bal);
                            });
                        }
                        break;

                    case "UPDATE_PROFILE_SUCCESS":
                        UserDTO updatedUser = (UserDTO) res.getData();
                        SessionManager.setCurrentUser(updatedUser);
                        Platform.runLater(() -> {
                            AlertHelper.showInfo(res.getMessage());
                            if (ProfileEditController.onBackAction != null) {
                                ProfileEditController.onBackAction.run();
                            }
                        });
                        break;

                    case "UPDATE_PROFILE_FAILED":
                        Platform.runLater(() -> {
                            AlertHelper.showError(res.getMessage());
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