package com.uet.auction.server.network;

import com.uet.auction.common.Request.AuctionRequest;
import com.uet.auction.common.Response.AuctionResponse;
import com.uet.auction.server.service.AuctionService;
import com.uet.auction.server.service.AuthService;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    private AuthService authService = new AuthService();
    private AuctionService auctionService = new AuctionService();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            while (true) {
                AuctionRequest request = (AuctionRequest) in.readObject();
                AuctionResponse response = null;

                switch (request.getType()) {
                    case "LOGIN":
                        response = authService.login(request);
                        sendResponse(response);
                        break;
                    case "REGISTER":
                        Object[] regData = (Object[]) request.getData();
                        response = authService.register((String) regData[0], (String) regData[1], (String) regData[2]);
                        sendResponse(response);
                        break;
                    case "ADD_PRODUCT":
                        Object[] addData = (Object[]) request.getData();
                        response = auctionService.addProduct(addData);
                        sendResponse(response);
                        break;
                    case "GET_PENDING_PRODUCTS":
                        response = auctionService.getProductsByStatus("PENDING");
                        sendResponse(response);
                        break;
                    case "GET_OPEN_PRODUCTS":
                        response = auctionService.getProductsByStatus("OPEN");
                        sendResponse(response);
                        break;
                    case "CHANGE_PRODUCT_STATUS":
                        Object[] statusData = (Object[]) request.getData();
                        response = auctionService.changeProductStatus((int) statusData[0], (String) statusData[1]);
                        sendResponse(response);
                        break;
                    case "PLACE_BID":
                        Object[] bidData = (Object[]) request.getData();
                        response = auctionService.placeBid((int) bidData[0], (String) bidData[1], (double) bidData[2]);
                        sendResponse(response);
                        // Kích hoạt Real-time
                        if (response.isSuccess()) {
                            SocketServer.broadcast(new AuctionResponse(true, "UPDATE_PRICE", null));
                        }
                        break;
                    default:
                        sendResponse(new AuctionResponse(false, "ERROR", "Unknown request"));
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println("Client disconnected.");
            SocketServer.removeClient(this);
        }
    }

    public void sendResponse(AuctionResponse response) {
        try {
            out.writeObject(response);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}