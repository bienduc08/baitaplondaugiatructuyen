package com.uet.auction.client.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType; // [THÊM MỚI] import ButtonType cho showConfirm

import java.util.Optional; // [THÊM MỚI] import Optional cho showConfirm

public class AlertHelper {

    // Hàm này KHÔNG thay đổi so với file gốc
    public static void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Hàm này KHÔNG thay đổi so với file gốc
    public static void showError(String message) {
        showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", message);
    }

    // Hàm này KHÔNG thay đổi so với file gốc
    public static void showInfo(String message) {
        showAlert(Alert.AlertType.INFORMATION, "Thông báo", message);
    }

    /**
     * Hiện thông báo KHÔNG block UI — tự đóng sau 3 giây.
     * Dùng cho các thông báo broadcast (duyệt SP, kết thúc phiên, v.v.)
     * để tránh đóng băng màn hình.
     */
    /**
     * Hiện thông báo KHÔNG block UI — tự đóng sau 3 giây.
     * Phải gọi từ JavaFX thread (Platform.runLater) — KHÔNG wrap thêm runLater bên trong.
     */
    public static void showInfoNonBlocking(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Thông báo");
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Gắn owner window để dialog hiện đúng vị trí và không bị trắng
        try {
            javafx.stage.Stage owner = null;
            for (javafx.stage.Window w : javafx.stage.Window.getWindows()) {
                if (w.isShowing() && w instanceof javafx.stage.Stage) {
                    owner = (javafx.stage.Stage) w;
                    break;
                }
            }
            if (owner != null) {
                alert.initOwner(owner);
            }
        } catch (Exception ignored) {}

        alert.show();

        javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(
                javafx.util.Duration.seconds(3));
        delay.setOnFinished(e -> alert.close());
        delay.play();
    }

    public static void showWarning(String message) {
        showAlert(Alert.AlertType.WARNING, "Cảnh báo", message);
    }

    // [THÊM MỚI] Hàm showConfirm - file gốc không có
    // Dùng trong AdminUserManagementController để hỏi xác nhận trước khi khóa/mở khóa
    public static Optional<ButtonType> showConfirm(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        return alert.showAndWait();
    }
    // Trong lớp ResponseListener.java hoặc một lớp tĩnh quản lý trạng thái
    private static long lastErrorTime = 0;

    public static void showThrottleError(String message) {
        long now = System.currentTimeMillis();
        // Chỉ hiện thông báo nếu khoảng cách giữa 2 lỗi > 2 giây
        if (now - lastErrorTime > 2000) {
            AlertHelper.showError(message);
            lastErrorTime = now;
        }
    }
    // [KẾT THÚC THÊM MỚI]
}