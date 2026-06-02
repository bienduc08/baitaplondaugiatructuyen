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
    private final AuctionService auctionService = AuctionService.getInstance();

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
                                (String) regData[0],
                                (String) regData[1],
                                (String) regData[2],
                                (String) regData[3],
                                (String) regData[4],
                                (String) regData[5]
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
                        // SECURITY: dùng username từ session server, không tin client gửi lên
                        if (loggedInUser == null) {
                            sendResponse(new AuctionResponse(false, "BID_RESULT", "Bạn chưa đăng nhập!", null));
                            break;
                        }
                        Object[] bidData = (Object[]) request.getData();
                        int productId2 = ((Number) bidData[0]).intValue();
                        String bidder  = loggedInUser.getUsername(); // ← lấy từ session
                        double amount  = ((Number) bidData[2]).doubleValue();
                        response = auctionService.placeBid(productId2, bidder, amount);
                        sendResponse(response);
                        if (response.isSuccess()) {
                            auctionService.triggerAutoBid(productId2, bidder); // kích hoạt auto-bid
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
                    // Khi nhận được response cho "GET_DASHBOARD_STATS"
                    // Phác thảo luồng xử lý bên Server (ClientHandler)
                    // Nếu Server báo thành công
                    // THỐNG KÊ DASHBOARD CHO ADMIN
                    // Khi nhận được response cho "GET_DASHBOARD_STATS"
                    // Phác thảo luồng xử lý bên Server (ClientHandler)
                    case "UPDATE_PROFILE":
                        Object[] updateData = (Object[]) request.getData();
                        String updateUsername = (String) updateData[0];
                        String fullName = (String) updateData[1];
                        String phone = (String) updateData[2];
                        String oldPass = (String) updateData[3];
                        String newPass = (String) updateData[4];

                        // Chuyển việc xử lý cho AuthService
                        response = authService.updateProfile(updateUsername, fullName, phone, oldPass, newPass);

                        // Nếu thành công, cập nhật luôn biến loggedInUser trên Server
                        if (response.isSuccess() && response.getData() instanceof UserDTO) {
                            loggedInUser = (UserDTO) response.getData();
                        }

                        // Gửi kết quả về cho giao diện Client
                        sendResponse(response);
                        break;
                    case "REGISTER_AUTO_BID":
                        if (loggedInUser == null) {
                            sendResponse(new AuctionResponse(false, "REGISTER_AUTO_BID_RESULT", "Bạn chưa đăng nhập!", null));
                            break;
                        }

                        Object[] autoBidData = (Object[]) request.getData();

                        // Lấy chính xác 5 phần tử theo đúng thứ tự mà Client gửi lên
                        int pId = ((Number) autoBidData[0]).intValue();
                        int bidderId = ((Number) autoBidData[1]).intValue();
                        String autoBidderUsername = (String) autoBidData[2];
                        double maxPrice = ((Number) autoBidData[3]).doubleValue();
                        double increment = ((Number) autoBidData[4]).doubleValue();

                        // Khởi tạo Object cấu hình truyền đúng tham số
                        com.uet.auction.server.model.AutoBidConfig config =
                                new com.uet.auction.server.model.AutoBidConfig(pId, bidderId, autoBidderUsername, maxPrice, increment);

                        // Truyền đối tượng vào service
                        response = auctionService.registerAutoBid(config);

                        response.setType("REGISTER_AUTO_BID_RESULT");
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