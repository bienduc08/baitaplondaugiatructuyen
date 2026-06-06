package com.uet.auction.server.network;

import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.common.DTO.UserDTO;
import com.uet.auction.common.Request.AuctionRequest;
import com.uet.auction.common.Response.AuctionResponse;
import com.uet.auction.server.DAO.NotificationDAO;
import com.uet.auction.server.DAO.ProductDAO;
import com.uet.auction.server.model.AutoBidConfig;
import com.uet.auction.server.service.AuctionService;
import com.uet.auction.server.service.AuthService;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private ObjectInputStream  in;
    private ObjectOutputStream out;

    private final AuthService    authService    = new AuthService();
    private final AuctionService auctionService = AuctionService.getInstance();

    private UserDTO loggedInUser = null;

    public ClientHandler(Socket socket) { this.socket = socket; }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in  = new ObjectInputStream(socket.getInputStream());

            while (true) {
                AuctionRequest request = (AuctionRequest) in.readObject();
                AuctionResponse response;

                switch (request.getType()) {

                    case "LOGIN":
                        response = authService.login(request);
                        if (response.isSuccess() && response.getData() instanceof UserDTO) {
                            loggedInUser = (UserDTO) response.getData();
                        }
                        sendResponse(response);
                        break;

                    case "REGISTER":
                        Object[] regData = (Object[]) request.getData();
                        response = authService.register(
                                (String) regData[0], (String) regData[1], (String) regData[2],
                                (String) regData[3], (String) regData[4], (String) regData[5]
                        );
                        sendResponse(response);
                        break;

                    case "DEPOSIT":
                        Object[] depData = (Object[]) request.getData();
                        response = authService.deposit((String) depData[0], ((Number) depData[1]).doubleValue());
                        sendResponse(response);
                        break;

                    case "GET_USER_BALANCE":
                        String balanceUsername = (String) request.getData();
                        double bal = authService.getUserBalance(balanceUsername);
                        response = new AuctionResponse(true, "GET_USER_BALANCE_RESULT", bal);
                        sendResponse(response);
                        break;

                    case "ADD_PRODUCT":
                        ProductDTO product = (ProductDTO) request.getData();
                        response = auctionService.addProduct(product);
                        sendResponse(response);
                        if (response.isSuccess()) {
                            SocketServer.broadcast(new AuctionResponse(true, "UPDATE_PRICE", null));
                        }
                        break;

                    case "UPDATE_PRODUCT":
                        ProductDTO pUpdate = (ProductDTO) request.getData();
                        pUpdate.setStatus("PENDING");
                        response = auctionService.updateProduct(pUpdate);
                        sendResponse(response);
                        if (response.isSuccess()) {
                            SocketServer.broadcast(new AuctionResponse(true, "UPDATE_PRICE", null));
                        }
                        break;

                    case "GET_PENDING_PRODUCTS":
                        response = auctionService.getProductsByStatus("PENDING");
                        sendResponse(response);
                        break;

                    case "GET_OPEN_PRODUCTS":
                        response = auctionService.getProductsByStatus("OPEN");
                        sendResponse(response);
                        break;

                    case "GET_ALL_PRODUCTS":
                        response = auctionService.getProductsByStatus("ALL");
                        sendResponse(response);
                        break;

                    case "GET_CLOSED_PRODUCTS":
                        response = auctionService.getProductsByStatus("CLOSED");
                        sendResponse(response);
                        break;

                    case "GET_MY_PRODUCTS":
                        String sellerName = (String) request.getData();
                        response = auctionService.getProductsBySeller(sellerName);
                        sendResponse(response);
                        break;

                    case "APPROVE_PRODUCT": {
                        if (!isAdmin()) { sendResponse(unauthorized()); break; }
                        ProductDTO pApprove = (ProductDTO) request.getData();
                        boolean ok = new ProductDAO().updateProductStatus(pApprove.getId(), "OPEN");
                        if (ok) {
                            try {
                                NotificationDAO notifDAO = new NotificationDAO();
                                String msg = "Sản phẩm \"" + pApprove.getName() + "\" của bạn đã được duyệt và đang được mở đấu giá!";
                                notifDAO.insertNotification(pApprove.getSellerName(), msg, "SYSTEM");
                                SocketServer.sendToUser(pApprove.getSellerName(),
                                        new AuctionResponse(true, "NOTIFY_REFRESH", "Sản phẩm \"" + pApprove.getName() + "\" đã được Admin duyệt!", null));
                            } catch (Exception e) {
                                System.err.println("Lỗi gửi thông báo APPROVE: " + e.getMessage());
                            }
                            sendResponse(new AuctionResponse(true, "CHANGE_STATUS_RESULT", "✅ Đã duyệt sản phẩm \"" + pApprove.getName() + "\""));
                            SocketServer.broadcast(new AuctionResponse(true, "UPDATE_PRICE", null));
                        } else {
                            sendResponse(new AuctionResponse(false, "CHANGE_STATUS_RESULT", "Lỗi: Không thể cập nhật CSDL!"));
                        }
                        break;
                    }

                    case "REJECT_PRODUCT": {
                        if (!isAdmin()) { sendResponse(unauthorized()); break; }
                        ProductDTO pReject = (ProductDTO) request.getData();
                        boolean ok = new ProductDAO().updateProductStatus(pReject.getId(), "REJECTED");
                        if (ok) {
                            try {
                                NotificationDAO notifDAO = new NotificationDAO();
                                String msg = "Rất tiếc, sản phẩm \"" + pReject.getName() + "\" của bạn đã bị Admin từ chối.";
                                notifDAO.insertNotification(pReject.getSellerName(), msg, "SYSTEM");
                                SocketServer.sendToUser(pReject.getSellerName(),
                                        new AuctionResponse(true, "NOTIFY_REFRESH", "Sản phẩm \"" + pReject.getName() + "\" đã bị Admin từ chối.", null));
                            } catch (Exception e) {
                                System.err.println("Lỗi gửi thông báo REJECT: " + e.getMessage());
                            }
                            sendResponse(new AuctionResponse(true, "CHANGE_STATUS_RESULT", "❌ Đã từ chối sản phẩm \"" + pReject.getName() + "\""));
                            SocketServer.broadcast(new AuctionResponse(true, "UPDATE_PRICE", null));
                        } else {
                            sendResponse(new AuctionResponse(false, "CHANGE_STATUS_RESULT", "Lỗi: Không thể cập nhật CSDL!"));
                        }
                        break;
                    }

                    case "PLACE_BID":
                        Object[] bidData = (Object[]) request.getData();
                        int productId2 = ((Number) bidData[0]).intValue();
                        String bidder  = (String) bidData[1];
                        double amount  = ((Number) bidData[2]).doubleValue();
                        response = auctionService.placeBid(productId2, bidder, amount);
                        sendResponse(response);
                        if (response.isSuccess()) {
                            ProductDAO pdao = new ProductDAO();
                            // FIX: Dùng cùng 1 DAO instance để extend rồi đọc lại,
                            // đảm bảo freshProduct luôn có end_time mới sau anti-sniping
                            pdao.extendAuctionIfLastBid();
                            auctionService.triggerAutoBid(productId2, bidder);

                            java.util.Map<String, Object> priceData = new java.util.HashMap<>();
                            priceData.put("productId", productId2);
                            // Đọc freshProduct SAU khi extend đã chạy xong
                            ProductDTO freshProduct = pdao.getProductById(productId2);
                            if (freshProduct != null) priceData.put("product", freshProduct);
                            SocketServer.broadcast(new AuctionResponse(true, "UPDATE_PRICE", null, priceData));
                        }
                        break;

                    case "REGISTER_AUTO_BID":
                        Object[] autoData = (Object[]) request.getData();
                        int abProductId   = ((Number) autoData[0]).intValue();
                        int abBidderId    = ((Number) autoData[1]).intValue();
                        String abUsername = (String) autoData[2];
                        Double abMax      = ((Number) autoData[3]).doubleValue();
                        Double abIncrement = ((Number) autoData[4]).doubleValue();

                        AutoBidConfig autoBidConfig = new AutoBidConfig(abBidderId, abUsername, abProductId, abMax, abIncrement);
                        response = auctionService.registerAutoBid(autoBidConfig);
                        sendResponse(response);
                        if (response.isSuccess()) {
                            auctionService.triggerAutoBid(abProductId, null);
                            SocketServer.broadcast(new AuctionResponse(true, "UPDATE_PRICE", null));
                        }
                        break;

                    case "GET_JOINED_PRODUCTS":
                        String joinedUsername = (String) request.getData();
                        response = auctionService.getJoinedProducts(joinedUsername);
                        sendResponse(response);
                        break;

                    case "GET_PRODUCT_BY_ID": {
                        int pid = ((Number) request.getData()).intValue();
                        ProductDTO fetched = new ProductDAO().getProductById(pid);
                        response = fetched != null
                                ? new AuctionResponse(true, "GET_PRODUCT_BY_ID_RESULT", fetched)
                                : new AuctionResponse(false, "GET_PRODUCT_BY_ID_RESULT", "Không tìm thấy sản phẩm", null);
                        sendResponse(response);
                        break;
                    }

                    case "GET_BID_HISTORY":
                        int productId = (int) request.getData();
                        response = auctionService.getBidHistory(productId);
                        sendResponse(response);
                        break;

                    case "GET_MY_BIDS":
                        String bidUsername = (String) request.getData();
                        response = auctionService.getMyBids(bidUsername);
                        sendResponse(response);
                        break;

                    case "GET_ALL_USERS":
                        if (!isAdmin()) { sendResponse(unauthorized()); break; }
                        response = authService.getAllUsers();
                        sendResponse(response);
                        break;

                    case "SEARCH_USER":
                        if (!isAdmin()) { sendResponse(unauthorized()); break; }
                        String searchKeyword = (String) request.getData();
                        response = authService.searchUser(searchKeyword);
                        sendResponse(response);
                        break;

                    case "LOCK_USER":
                        if (!isAdmin()) { sendResponse(unauthorized()); break; }
                        UserDTO userToLock = (UserDTO) request.getData();
                        response = authService.changeUserStatus(userToLock.getId(), "LOCKED");
                        sendResponse(response);
                        break;

                    case "UNLOCK_USER":
                        if (!isAdmin()) { sendResponse(unauthorized()); break; }
                        UserDTO userToUnlock = (UserDTO) request.getData();
                        response = authService.changeUserStatus(userToUnlock.getId(), "ACTIVE");
                        sendResponse(response);
                        break;

                    case "UPDATE_PROFILE":
                        Object[] updateData = (Object[]) request.getData();
                        String updateUsername = (String) updateData[0];
                        String fullName  = (String) updateData[1];
                        String phone     = (String) updateData[2];
                        String oldPass   = (String) updateData[3];
                        String newPass   = (String) updateData[4];
                        response = authService.updateProfile(updateUsername, fullName, phone, oldPass, newPass);
                        if (response.isSuccess() && response.getData() instanceof UserDTO) {
                            loggedInUser = (UserDTO) response.getData();
                        }
                        sendResponse(response);
                        break;

                    case "GET_NOTIFICATIONS":
                        String notifUser = (String) request.getData();
                        NotificationDAO notifDAO = new NotificationDAO();
                        response = new AuctionResponse(true, "GET_NOTIFICATIONS_SUCCESS", notifDAO.getUnreadNotifications(notifUser));
                        sendResponse(response);
                        break;

                    case "MARK_NOTIFICATION_READ":
                        int notifId = (int) request.getData();
                        new NotificationDAO().markAsRead(notifId);
                        response = new AuctionResponse(true, "MARK_READ_SUCCESS", null);
                        sendResponse(response);
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("Client ngắt kết nối: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Lỗi xử lý client: " + e.getMessage());
        } finally {
            SocketServer.removeClient(this);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    public String getLoggedInUsername() {
        return loggedInUser != null ? loggedInUser.getUsername() : null;
    }

    private boolean isAdmin() {
        return loggedInUser != null && "ADMIN".equals(loggedInUser.getRole());
    }

    private AuctionResponse unauthorized() {
        return new AuctionResponse(false, "ERROR", "Bạn không có quyền thực hiện thao tác này!", null);
    }

    public synchronized void sendResponse(AuctionResponse response) {
        try {
            out.reset();
            out.writeObject(response);
            out.flush();
        } catch (IOException e) {
        }
    }
}