package com.uet.auction.server.network;

import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.common.DTO.UserDTO;
import com.uet.auction.common.Request.AuctionRequest;
import com.uet.auction.common.Response.AuctionResponse;
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
    private final AuctionService auctionService = new AuctionService();

    // Lưu thông tin user đã đăng nhập của kết nối này để kiểm tra quyền
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
                        // Lưu lại user đã đăng nhập thành công
                        if (response.isSuccess() && response.getData() instanceof UserDTO) {
                            loggedInUser = (UserDTO) response.getData();
                        }
                        sendResponse(response);
                        break;

                    case "REGISTER":
                        Object[] regData = (Object[]) request.getData();
                        response = authService.register(
                                (String) regData[0], (String) regData[1], (String) regData[2]);
                        sendResponse(response);
                        break;

                    case "UPGRADE_TO_SELLER":
                        String upgradeUsername = (String) request.getData();
                        response = authService.upgradeToSeller(upgradeUsername);
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

                    case "GET_MY_PRODUCTS":
                        String sellerName = (String) request.getData();
                        response = auctionService.getProductsBySeller(sellerName);
                        sendResponse(response);
                        break;

                    case "APPROVE_PRODUCT":
                        // SECURITY: Chỉ ADMIN mới được duyệt sản phẩm
                        if (!isAdmin()) {
                            sendResponse(unauthorized());
                            break;
                        }
                        ProductDTO pApprove = (ProductDTO) request.getData();
                        response = auctionService.changeProductStatus(pApprove.getId(), "OPEN");
                        sendResponse(response);
                        if (response.isSuccess()) {
                            SocketServer.broadcast(new AuctionResponse(true, "UPDATE_PRICE", null));
                        }
                        break;

                    case "REJECT_PRODUCT":
                        // SECURITY: Chỉ ADMIN mới được từ chối sản phẩm
                        if (!isAdmin()) {
                            sendResponse(unauthorized());
                            break;
                        }
                        ProductDTO pReject = (ProductDTO) request.getData();
                        response = auctionService.changeProductStatus(pReject.getId(), "REJECTED");
                        sendResponse(response);
                        if (response.isSuccess()) {
                            SocketServer.broadcast(new AuctionResponse(true, "UPDATE_PRICE", null));
                        }
                        break;

                    case "CHANGE_PRODUCT_STATUS":
                        // SECURITY: Chỉ ADMIN mới được thay đổi trạng thái sản phẩm
                        if (!isAdmin()) {
                            sendResponse(unauthorized());
                            break;
                        }
                        Object[] statusData = (Object[]) request.getData();
                        response = auctionService.changeProductStatus(
                                (int) statusData[0], (String) statusData[1]);
                        sendResponse(response);
                        if (response.isSuccess()) {
                            SocketServer.broadcast(new AuctionResponse(true, "UPDATE_PRICE", null));
                        }
                        break;

                    case "PLACE_BID":
                        Object[] bidData = (Object[]) request.getData();
                        int productId2 = ((Number) bidData[0]).intValue();
                        String bidder  = (String) bidData[1];
                        double amount  = ((Number) bidData[2]).doubleValue();
                        response = auctionService.placeBid(productId2, bidder, amount);
                        sendResponse(response);
                        if (response.isSuccess()) {
                            SocketServer.broadcast(new AuctionResponse(true, "UPDATE_PRICE", null));
                        }
                        break;

                    case "GET_JOINED_PRODUCTS":
                        String joinedUsername = (String) request.getData();
                        response = auctionService.getJoinedProducts(joinedUsername);
                        sendResponse(response);
                        break;

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

                    // QUẢN LÝ NGƯỜI DÙNG — CHỈ ADMIN
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

                    // THỐNG KÊ DASHBOARD CHO ADMIN
                    case "GET_DASHBOARD_STATS":
                        if (!isAdmin()) { sendResponse(unauthorized()); break; }
                        try {
                            java.util.List<?> openList    = (java.util.List<?>) auctionService.getProductsByStatus("OPEN").getData();
                            java.util.List<?> closedList  = (java.util.List<?>) auctionService.getProductsByStatus("CLOSED").getData();
                            java.util.List<?> pendingList = (java.util.List<?>) auctionService.getProductsByStatus("PENDING").getData();

                            int openCount    = (openList    != null) ? openList.size()    : 0;
                            int closedCount  = (closedList  != null) ? closedList.size()  : 0;
                            int pendingCount = (pendingList != null) ? pendingList.size() : 0;

                            int[] stats = {openCount, closedCount, pendingCount};
                            response = new AuctionResponse(true, "GET_STATS_SUCCESS", "Lấy thống kê thành công", stats);
                        } catch (Exception e) {
                            response = new AuctionResponse(false, "ERROR", "Lỗi lấy thống kê: " + e.getMessage(), null);
                        }
                        sendResponse(response);
                        break;

                    default:
                        sendResponse(new AuctionResponse(false, "ERROR",
                                "Yêu cầu không hợp lệ: " + request.getType(), null));
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

    /** Kiểm tra người dùng hiện tại có phải ADMIN không */
    private boolean isAdmin() {
        return loggedInUser != null && "ADMIN".equals(loggedInUser.getRole());
    }

    /** Trả về response lỗi unauthorized */
    private AuctionResponse unauthorized() {
        return new AuctionResponse(false, "ERROR", "Bạn không có quyền thực hiện thao tác này!", null);
    }

    public synchronized void sendResponse(AuctionResponse response) {
        try {
            out.reset();
            out.writeObject(response);
            out.flush();
        } catch (IOException e) {
            // Không in lỗi khi client ngắt kết nối (bình thường)
        }
    }
}
