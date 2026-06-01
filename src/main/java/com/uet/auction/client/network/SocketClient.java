package com.uet.auction.client.network;

import com.uet.auction.common.Request.AuctionRequest;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;

public class SocketClient {
    private static Socket             socket;
    private static ObjectOutputStream out;
    private static ObjectInputStream  in;
    private static volatile boolean isReconnecting = false;

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
            startAutoReconnect();
        }
    }

    /**
     * Gửi request lên server một cách thread-safe.
     * Dùng synchronized để tránh nhiều event handler ghi đồng thời gây corrupt data.
     */
    public static void sendRequest(AuctionRequest request) {
        if (!isConnected()) {
            System.err.println("Chưa kết nối server hoặc mất mạng!");
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
    public static void startAutoReconnect() {
        // Nếu đang trong quá trình kết nối lại rồi thì không tạo thêm Thread mới
        if (isReconnecting) return;
        isReconnecting = true;

        new Thread(() -> {
            Platform.runLater(() ->
                    com.uet.auction.client.util.AlertHelper.showError("Mất kết nối tới Server. Hệ thống đang tự động thử lại...")
            );

            while (isReconnecting) {
                try {
                    Thread.sleep(3000); // Đợi 3 giây trước mỗi lần thử để tránh treo máy
                    System.out.println("Đang thử kết nối lại với Server...");

                    // Đóng các luồng cũ (nếu có) để giải phóng tài nguyên
                    if (socket != null && !socket.isClosed()) socket.close();

                    // Thử kết nối lại
                    socket = new Socket("localhost", 8080);
                    out = new ObjectOutputStream(socket.getOutputStream());
                    in  = new ObjectInputStream(socket.getInputStream());

                    // Nếu chạy xuống được đến đây nghĩa là kết nối THÀNH CÔNG
                    isReconnecting = false;
                    System.out.println("Khôi phục kết nối thành công!");

                    Platform.runLater(() ->
                            com.uet.auction.client.util.AlertHelper.showInfo("Đã khôi phục kết nối với Server!")
                    );

                    // QUAN TRỌNG: Phải khởi động lại luồng lắng nghe mới
                    new Thread(new ResponseListener(in)).start();

                } catch (Exception e) {
                    System.out.println("Vẫn chưa tìm thấy Server. Tiếp tục thử lại...");
                }
            }
        }).start();
    }
}