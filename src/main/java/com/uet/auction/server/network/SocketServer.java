package com.uet.auction.server.network;

import com.uet.auction.common.Response.AuctionResponse;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketServer {

    // SỬA: dùng synchronizedList để tránh ConcurrentModificationException
    private static final List<ClientHandler> clients =
            Collections.synchronizedList(new ArrayList<>());

    // SỬA: dùng ThreadPool thay vì new Thread() không giới hạn
    // Giới hạn tối đa 50 client đồng thời
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(50);

    public void start(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server khởi động trên cổng " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client kết nối: " + socket.getInetAddress());
                ClientHandler handler = new ClientHandler(socket);
                clients.add(handler);
                // SỬA: dùng thread pool thay vì new Thread().start()
                threadPool.submit(handler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Gửi thông báo tới tất cả client ĐÃ ĐĂNG NHẬP (bỏ qua màn hình Login) */
    public static void broadcastToLoggedInUsers(AuctionResponse response) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                // Chỉ gửi nếu username không null (tức là đã login thành công)
                if (client.getLoggedInUsername() != null) {
                    client.sendResponse(response);
                }
            }
        }
    }

    /** Gửi thông báo tới đúng 1 user theo username */
    public static void sendToUser(String username, AuctionResponse response) {
        if (username == null) return;
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (username.equals(client.getLoggedInUsername())) {
                    client.sendResponse(response);
                }
            }
        }
    }

    /** Xóa client khi họ ngắt kết nối */
    public static void removeClient(ClientHandler client) {
        clients.remove(client);
    }
}