package com.uet.auction.client.network;

import com.uet.auction.client.controller.*;
import com.uet.auction.client.util.AlertHelper;
import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.common.DTO.UserDTO;
import com.uet.auction.common.Response.AuctionResponse;
import com.uet.auction.server.service.SessionManager;
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
                    case "GET_PENDING_PRODUCTS_RESULT": // Gộp chung xử lý vì list giống nhau
                        List<ProductDTO> allProducts = (List<ProductDTO>) res.getData();
                        Platform.runLater(() -> {
                            // 1. Báo cho AdminController đếm số lượng (Open, Pending, Closed)
                            if (AdminController.instance != null) {
                                AdminController.instance.updatePendingList(allProducts);
                            }

                            // 2. Báo cho AdminPendingController để hiển thị chi tiết vào Bảng
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
                            if (ProductDetailController.instance != null)
                                ProductDetailController.instance.reloadProductDetails();
                            if (SellerMyProductsController.instance != null)
                                SellerMyProductsController.instance.loadMyAuctions();
                            String currentUsr = com.uet.auction.client.util.SessionManager.getCurrentUsername();
                            if (currentUsr != null) {
                                SocketClient.sendRequest(new com.uet.auction.common.Request.AuctionRequest("GET_USER_BALANCE", currentUsr));
                            }
                        });
                        break;
                    case "UPDATE_PRODUCT_RESULT":
                        Platform.runLater(() -> {
                            if (res.isSuccess()) {
                                AlertHelper.showInfo(res.getMessage());
                                // Đóng form sửa, kích hoạt quay về màn hình danh sách sản phẩm cũ
                                if (SellerEditProductController.onCancelAction != null) {
                                    SellerEditProductController.onCancelAction.run();
                                }
                                // Tải lại danh sách sản phẩm của tôi để thấy trạng thái đổi thành PENDING
                                if (SellerMyProductsController.instance != null) {
                                    SellerMyProductsController.instance.loadMyAuctions();
                                }
                            } else {
                                AlertHelper.showError(res.getMessage());
                            }
                        });
                        break;

                    case "AUCTION_ENDED":
                        // Server gửi khi một phiên đấu giá vừa kết thúc
                        // message chứa thông báo đầy đủ, data chứa Map thông tin phiên
                        String endedMsg = res.getMessage();
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> endedInfo =
                                (java.util.Map<String, Object>) res.getData();

                        Platform.runLater(() -> {
                            // Hiện popup thông báo người thắng cho tất cả client đang online
                            if (endedMsg != null) {
                                AlertHelper.showInfo(endedMsg);
                            }

                            // Reload giao diện để phản ánh trạng thái CLOSED
                            if (HomecontentController.instance != null)
                                HomecontentController.instance.loadProducts();
                            if (ProductDetailController.instance != null)
                                ProductDetailController.instance.reloadProductDetails();
                            if (UserAuctionsController.instance != null) {
                                String username = com.uet.auction.client.util.SessionManager.getCurrentUsername();
                                if (username != null)
                                    SocketClient.sendRequest(new com.uet.auction.common.Request.AuctionRequest("GET_JOINED_PRODUCTS", username));
                            }
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

                    case "DEPOSIT_RESULT":
                        Platform.runLater(() -> {
                            if (res.isSuccess()) {
                                AlertHelper.showInfo(res.getMessage());
                                double newBalance = ((Number) res.getData()).doubleValue();

                                // 1. Cập nhật Session
                                com.uet.auction.common.DTO.UserDTO current = com.uet.auction.client.util.SessionManager.getCurrentUser();
                                if (current != null) {
                                    current.setBalance(newBalance);
                                }

                                // 2. Cập nhật ĐỒNG THỜI khung chính (Sidebar) VÀ trang Profile của User
                                if (UserController.instance != null) {
                                    UserController.instance.updateBalance(); // Cập nhật sidebar của User
                                }
                                if (ProfileUserController.instance != null) {
                                    ProfileUserController.instance.handleDepositSuccess(newBalance); // Cập nhật Profile của User
                                }

                                // 3. Cập nhật ĐỒNG THỜI khung chính (Sidebar) VÀ trang Profile của Seller
                                if (SellerController.instance != null) {
                                    SellerController.instance.updateBalance(); // Cập nhật sidebar của Seller
                                }
                                if (ProfileSellerController.instance != null) {
                                    ProfileSellerController.instance.handleDepositSuccess(newBalance); // Cập nhật Profile của Seller
                                }

                                // 4. Cập nhật ĐỒNG THỜI khung chính (Sidebar) VÀ trang Profile của Admin
                                if (AdminController.instance != null) {
                                    AdminController.instance.updateBalance(); // Cập nhật sidebar của Admin
                                }
                                if (ProfileAdminController.instance != null) {
                                    ProfileAdminController.instance.handleDepositSuccess(newBalance); // Cập nhật Profile của Admin
                                }

                            } else {
                                AlertHelper.showError(res.getMessage());

                                // Xử lý thất bại (Tắt vòng loading, reset input...)
                                if (ProfileUserController.instance != null) ProfileUserController.instance.handleDepositFailure();
                                if (ProfileSellerController.instance != null) ProfileSellerController.instance.handleDepositFailure();
                                if (ProfileAdminController.instance != null) ProfileAdminController.instance.handleDepositFailure();
                            }
                        });
                        break;

                    case "GET_USER_BALANCE_RESULT":
                        if (res.isSuccess()) {
                            double bal = ((Number) res.getData()).doubleValue();
                            Platform.runLater(() -> {
                                // 1. Cập nhật Session
                                com.uet.auction.common.DTO.UserDTO current = com.uet.auction.client.util.SessionManager.getCurrentUser();
                                if (current != null) {
                                    current.setBalance(bal);
                                }

                                // 2. Cập nhật ĐỒNG THỜI Sidebar và Profile của User
                                if (UserController.instance != null) UserController.instance.updateBalance();
                                if (ProfileUserController.instance != null) ProfileUserController.instance.handleDepositSuccess(bal); // Hoặc gọi hàm updateBalance(bal) nếu bạn viết riêng

                                // 3. Cập nhật ĐỒNG THỜI Sidebar và Profile của Seller
                                if (SellerController.instance != null) SellerController.instance.updateBalance();
                                if (ProfileSellerController.instance != null) ProfileSellerController.instance.handleDepositSuccess(bal);

                                // 4. Cập nhật ĐỒNG THỜI Sidebar và Profile của Admin
                                if (AdminController.instance != null) AdminController.instance.updateBalance();
                                if (ProfileAdminController.instance != null) ProfileAdminController.instance.handleDepositSuccess(bal);
                            });
                        }
                        break;

                    case "UPDATE_PROFILE_SUCCESS":
                        UserDTO updatedUser = (UserDTO) res.getData();

                        // SỬA LỖI 2: Dùng SessionManager của phía Client
                        com.uet.auction.client.util.SessionManager.setCurrentUser(updatedUser);

                        Platform.runLater(() -> {
                            AlertHelper.showInfo(res.getMessage());

                            // Kích hoạt nút quay lại màn hình Profile
                            if (ProfileEditController.onBackAction != null) {
                                ProfileEditController.onBackAction.run();
                            }
                        });
                        break;

                    // Nếu Server báo thất bại (ví dụ: Sai mật khẩu cũ)
                    case "UPDATE_PROFILE_FAILED":
                        Platform.runLater(() -> {
                            AlertHelper.showError(res.getMessage());
                        });
                        break;
                    case "GET_NOTIFICATIONS_SUCCESS":
                        java.util.List<com.uet.auction.common.DTO.NotificationDTO> notifs =
                                (java.util.List<com.uet.auction.common.DTO.NotificationDTO>) res.getData();

                        int unreadCount = notifs.size();

                        javafx.application.Platform.runLater(() -> {
                            if (com.uet.auction.client.controller.UserController.instance != null) {
                                com.uet.auction.client.controller.UserController.instance.updateNotificationCount(unreadCount);
                            }
                            if (com.uet.auction.client.controller.SellerController.instance != null) {
                                com.uet.auction.client.controller.SellerController.instance.updateNotificationCount(unreadCount);
                            }
                            if (com.uet.auction.client.controller.AdminController.instance != null) {
                                com.uet.auction.client.controller.AdminController.instance.updateNotificationCount(unreadCount);
                            }

                            // ---> THÊM DÒNG NÀY: Nếu đang mở màn hình danh sách thông báo thì render ra danh sách luôn
                            if (com.uet.auction.client.controller.NotificationListController.instance != null) {
                                com.uet.auction.client.controller.NotificationListController.instance.displayNotifications(notifs);
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
            // Kích hoạt kết nối lại từ phía listener
            com.uet.auction.client.network.SocketClient.startAutoReconnect();
        }
    }
}