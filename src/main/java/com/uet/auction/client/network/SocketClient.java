package com.uet.auction.client.network;

import com.uet.auction.common.Request.AuctionRequest;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;

public class SocketClient {
    private static Socket             socket;
    private static ObjectOutputStream out;
    private static ObjectInputStream  in;

    // Lock để đảm bảo chỉ một thread ghi vào output stream tại một thời điểm
    private static final Object SEND_LOCK = new Object();

    /** Gọi hàm này từ AuctionApplication.java khi app vừa mở lên */
    public static void connect() {
        try {
            socket = new Socket("localhost", 8080);
            out = new ObjectOutputStream(socket.getOutputStream());
            in  = new ObjectInputStream(socket.getInputStream());

            // Khởi động luồng lắng nghe phản hồi từ server
            new Thread(new ResponseListener(in)).start();
            System.out.println("Đã kết nối với Server!");
        } catch (Exception e) {
            System.err.println("Không thể kết nối Server!");
            Platform.runLater(() ->
                    com.uet.auction.client.util.AlertHelper.showError(
                            "Không thể kết nối tới server. Hãy đảm bảo Server đang chạy!"));
        }
    }

    public static void setOut(ObjectOutputStream out) {
        SocketClient.out = out;
    }

    /**
     * Gửi request lên server một cách thread-safe.
     * Dùng synchronized để tránh nhiều event handler ghi đồng thời gây corrupt data.
     */
    public static void sendRequest(AuctionRequest request) {
        if (out == null) {
            System.err.println("Chưa kết nối server!");
            Platform.runLater(() ->
                    com.uet.auction.client.util.AlertHelper.showError(
                            "Không thể kết nối tới server. Vui lòng thử lại!"));
            return;
        }
        synchronized (SEND_LOCK) {
            try {
                out.reset(); // Xóa cache object cũ tránh gửi dữ liệu sai
                out.writeObject(request);
                out.flush();
            } catch (Exception e) {
                System.err.println("Lỗi gửi request: " + e.getMessage());
            }
        }
    }

    public static boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}