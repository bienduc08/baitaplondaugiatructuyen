package com.uet.auction.client.network;

import com.uet.auction.common.Request.AuctionRequest;
import com.uet.auction.common.Response.AuctionResponse;
import java.io.*;
import java.net.Socket;

public class SocketClient {
    private static Socket socket;
    private static ObjectOutputStream out;
    private static ObjectInputStream in;

    // Gọi hàm này TỪ AuctionApplication.java khi app vừa mở lên
    public static void connect() {
        try {
            socket = new Socket("localhost", 8080);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // Khởi động luồng lắng nghe ở đây luôn cho tiện
            new Thread(new ResponseListener(in)).start();
            System.out.println("Đã kết nối với Server!");
        } catch (Exception e) {
            System.err.println("Không thể kết nối Server!");
        }
    }
    public static void setOut(ObjectOutputStream out) {
        SocketClient.out = out;
    }

    // Hàm gửi tin đi (Không cần nhận về ngay vì ResponseListener sẽ lo việc nhận)

    public static void sendRequest(AuctionRequest request) {
        if (out == null) {
            System.err.println("Chưa kết nối server!");
            javafx.application.Platform.runLater(() ->
                    com.uet.auction.client.util.AlertHelper.showError(
                            "Không thể kết nối tới server. Vui lòng thử lại!"));
            return;
        }
        try {
            out.reset();              // ← THÊM DÒNG NÀY — xóa cache object cũ
            out.writeObject(request);
            out.flush();
        } catch (Exception e) {
            System.err.println("Lỗi gửi request: " + e.getMessage());
        }
    }
}