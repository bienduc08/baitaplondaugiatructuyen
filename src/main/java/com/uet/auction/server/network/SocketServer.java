package com.uet.auction.server.network;

import com.uet.auction.common.Response.AuctionResponse;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class SocketServer {
    // Lưu danh sách tất cả người dùng đang online
    private static List<ClientHandler> clients = new ArrayList<>();

    public void start(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + socket.getInetAddress());
                ClientHandler handler = new ClientHandler(socket);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Hàm gửi thông báo cho tất cả Client (Rất quan trọng cho Đấu giá Real-time)
    public static void broadcast(AuctionResponse response) {
        for (ClientHandler client : clients) {
            client.sendResponse(response);
        }
    }

    // Xóa client khi họ thoát
    public static void removeClient(ClientHandler client) {
        clients.remove(client);
    }
}