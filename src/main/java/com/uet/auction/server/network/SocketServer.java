package com.uet.auction.server.network;

import com.uet.auction.common.Response.AuctionResponse;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SocketServer {

    // SỬA: đổi ArrayList → synchronizedList để tránh ConcurrentModificationException
    // khi broadcast() duyệt list đồng thời với removeClient() xóa list từ các thread khác nhau
    private static final List<ClientHandler> clients =
            Collections.synchronizedList(new ArrayList<>());

    public void start(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server khởi động trên cổng " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client kết nối: " + socket.getInetAddress());
                ClientHandler handler = new ClientHandler(socket);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Gửi thông báo tới tất cả client đang kết nối (dùng cho real-time update giá) */
    public static void broadcast(AuctionResponse response) {
        // SỬA: synchronized block để tránh lỗi khi list bị sửa đổi đồng thời
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendResponse(response);
            }
        }
    }

    /** Xóa client khi họ ngắt kết nối */
    public static void removeClient(ClientHandler client) {
        clients.remove(client);
    }
}