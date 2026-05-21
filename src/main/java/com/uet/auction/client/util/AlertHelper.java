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

    // [THÊM MỚI] Hàm showConfirm - file gốc không có
    // Dùng trong AdminUserManagementController để hỏi xác nhận trước khi khóa/mở khóa
    public static Optional<ButtonType> showConfirm(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        return alert.showAndWait();
    }
    // [KẾT THÚC THÊM MỚI]
}
