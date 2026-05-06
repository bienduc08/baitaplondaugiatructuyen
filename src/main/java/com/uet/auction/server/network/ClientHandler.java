package com.uet.auction.server.network;

import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.common.Request.AuctionRequest;
import com.uet.auction.common.Request.LoginRequest;
import com.uet.auction.common.Response.AuctionResponse;
import com.uet.auction.common.Response.LoginResponse;
import com.uet.auction.common.DTO.UserDTO;
import com.uet.auction.server.DAO.ProductDAO;
import com.uet.auction.server.DAO.UserDAO;
import com.uet.auction.server.service.AuctionService;
import com.uet.auction.server.service.AuthService;
import com.uet.auction.server.service.BidService;

import java.io.*;
import java.net.Socket;
import java.util.List;


public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private UserDAO userDAO;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.userDAO = new UserDAO(); // Khởi tạo DAO để làm việc với MySQL
        try {
            // ObjectOutputStream phải khởi tạo trước ObjectInputStream để tránh treo luồng
            this.out = new ObjectOutputStream(clientSocket.getOutputStream());
            this.in = new ObjectInputStream(clientSocket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Trong hàm run() của ClientHandler
    // Khai báo các service ở đầu class ClientHandler
    private AuthService authService = new AuthService();
    private AuctionService auctionService = new AuctionService();
    private BidService bidService = new BidService();

    @Override
    public void run() {
        try {
            Object input;
            while ((input = in.readObject()) != null) {
                if (input instanceof AuctionRequest) {
                    AuctionRequest request = (AuctionRequest) input;
                    AuctionResponse response = null;

                    // Điều hướng Request đến đúng Service
                    switch (request.getType()) {
                        case "LOGIN":
                            LoginRequest loginData = (LoginRequest) request.getData();
                            response = authService.authenticate(loginData);
                            break;

                        case "GET_PRODUCTS":
                            response = auctionService.listAllProducts();
                            break;

                        case "PLACE_BID":
                            // Giả sử data gửi lên là mảng Object[] {productId, username, amount}
                            Object[] bidData = (Object[]) request.getData();
                            int productId = (int) bidData[0];
                            String username = (String) bidData[1];
                            double amount = (double) bidData[2];

                            response = bidService.processBid(productId, username, amount);
                            break;
                        case "REGISTER":
                            String[] regData = (String[]) request.getData();
                            response = authService.register(regData[0], regData[1]);
                            sendResponse(response);
                            break;
                        case "ADD_PRODUCT":
                            Object[] prodData = (Object[]) request.getData();
                            String pName = (String) prodData[0];
                            double pPrice = (double) prodData[1];
                            int pDays = (int) prodData[2];

                            response = auctionService.addProduct(pName, pPrice, pDays);
                            sendResponse(response);
                            break;
                        case "GET_PENDING_PRODUCTS":
                            response = auctionService.getProductsByStatus("PENDING"); // Cho Admin
                            sendResponse(response);
                            break;

                        case "GET_OPEN_PRODUCTS":
                            response = auctionService.getProductsByStatus("OPEN"); // Cho User
                            sendResponse(response);
                            break;

                        case "CHANGE_PRODUCT_STATUS":
                            Object[] statusData = (Object[]) request.getData();
                            int pId = (int) statusData[0];
                            String pStatus = (String) statusData[1];
                            response = auctionService.changeProductStatus(pId, pStatus);
                            sendResponse(response);
                            break;

                        default:
                            response = new AuctionResponse(false, "Yêu cầu không hợp lệ!", null);
                    }

                    // Gửi câu trả lời về cho Client
                    sendResponse(response);
                }
            }
        } catch (Exception e) {
            System.out.println("Client ngắt kết nối.");
        } finally {
            closeConnection();
        }
    }

    private void handleLogin(LoginRequest request) {
        System.out.println("Đang xử lý đăng nhập cho: " + request.getUsername());

        // Gọi UserDAO để kiểm tra trong MySQL
        boolean isValid = userDAO.checkLogin(request.getUsername(), request.getPassword());

        LoginResponse response;
        if (isValid) {
            // Giả sử bạn lấy thông tin User từ DB ra DTO
            UserDTO userDTO = new UserDTO(request.getUsername(), 1000.0); // Ví dụ số dư 1000
            response = new LoginResponse(true, "Đăng nhập thành công!", userDTO);
        } else {
            response = new LoginResponse(false, "Sai tài khoản hoặc mật khẩu!", null);
        }

        sendResponse(response);
    }

    private void sendResponse(Object response) {
        try {
            out.writeObject(response);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
