package com.uet.auction.client.util;

import javafx.scene.control.Alert;

public class AlertHelper {
    public static void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void showError(String message) {
        showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", message);
    }

    public static void showInfo(String message) {
        showAlert(Alert.AlertType.INFORMATION, "Thông báo", message);
    }
}
