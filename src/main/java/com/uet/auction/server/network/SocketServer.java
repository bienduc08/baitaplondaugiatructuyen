package com.uet.auction.server.network;

import com.uet.auction.server.util.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SocketServer {
    private int port;
    private ServerSocket serverSocket;
    private boolean isRunning;

    public SocketServer(int port) {
        this.port = port;
        this.isRunning = false;
    }

    public void start() {
        try {
            // Mở cổng để lắng nghe các kết nối từ mạng
            serverSocket = new ServerSocket(port);
            isRunning = true;
            Logger.info("Server started on port " + port);
            System.out.println("Đang chờ Client kết nối...");

            while (isRunning) {
                // Chấp nhận một kết nối mới từ Client
                Socket clientSocket = serverSocket.accept();
                System.out.println("Phát hiện kết nối mới từ: " + clientSocket.getInetAddress());

                // Tạo một luồng (Thread) riêng để xử lý Client này thông qua ClientHandler
                // Việc dùng Thread giúp nhiều người có thể đấu giá cùng lúc
                ClientHandler handler = new ClientHandler(clientSocket);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("Lỗi khi khởi động Server: " + e.getMessage());
        } finally {
            stop();
        }
    }

    public void stop() {
        try {
            isRunning = false;
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            System.out.println("Server đã dừng.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // Thêm vào trong class SocketServer
    private static List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void broadcast(Object response) {
        for (ClientHandler client : clients) {
            client.sendResponse(response); // Gửi giá mới cho mọi người
        }
    }
    // Thêm vào class SocketServer
    private static java.util.List<ClientHandler> activeClients = new java.util.concurrent.CopyOnWriteArrayList<>();

    public static void addClient(ClientHandler handler) {
        activeClients.add(handler);
    }

    public static void broadcast(Object response) {
        for (ClientHandler client : activeClients) {
            client.sendResponse(response); // Hàm sendResponse bạn đã viết trong ClientHandler
        }
    }
}