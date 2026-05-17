package com.uet.auction.server.network;

import com.uet.auction.common.DTO.ProductDTO;
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
    private ObjectInputStream in;
    private ObjectOutputStream out;

    private final AuthService    authService    = new AuthService();
    private final AuctionService auctionService = new AuctionService();

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
                        sendResponse(response);
                        break;

                    case "REGISTER":
                        Object[] regData = (Object[]) request.getData();
                        response = authService.register(
                                (String) regData[0], (String) regData[1], (String) regData[2]);
                        sendResponse(response);
                        break;

                    case "ADD_PRODUCT":
                        ProductDTO product = (ProductDTO) request.getData();
                        response = auctionService.addProduct(product);
                        sendResponse(response);

                        // ĐÃ SỬA: Nếu thêm thành công, phát tín hiệu cho tất cả các máy (bao gồm Admin) tự cập nhật bảng
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

                    // ==========================================
                    // ĐÃ KHÔI PHỤC: CÁC CASE DUYỆT BÀI CỦA ADMIN
                    // ==========================================
                    case "APPROVE_PRODUCT":
                        ProductDTO pApprove = (ProductDTO) request.getData();
                        response = auctionService.changeProductStatus(pApprove.getId(), "OPEN");
                        sendResponse(response);
                        if (response.isSuccess()) {
                            SocketServer.broadcast(new AuctionResponse(true, "UPDATE_PRICE", null));
                        }
                        break;

                    case "REJECT_PRODUCT":
                        ProductDTO pReject = (ProductDTO) request.getData();
                        response = auctionService.changeProductStatus(pReject.getId(), "REJECTED");
                        sendResponse(response);
                        if (response.isSuccess()) {
                            SocketServer.broadcast(new AuctionResponse(true, "UPDATE_PRICE", null));
                        }
                        break;

                    case "CHANGE_PRODUCT_STATUS":
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

                    default:
                        sendResponse(new AuctionResponse(false, "ERROR",
                                "Yêu cầu không hợp lệ: " + request.getType(), null));
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println("Client ngắt kết nối: " + e.getMessage());
            SocketServer.removeClient(this);
        }
    }

    public void sendResponse(AuctionResponse response) {
        try {
            out.reset();
            out.writeObject(response);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}