package com.uet.auction.client.controller;

import com.uet.auction.client.network.SocketClient;
import com.uet.auction.common.DTO.NotificationDTO;
import com.uet.auction.common.Request.AuctionRequest;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.util.List;

public class NotificationListController {
    public static NotificationListController instance;
    public static Runnable onBackAction;

    @FXML private VBox notifContainer;

    @FXML
    public void initialize() {
        instance = this;
        // Mỗi khi màn hình này mở ra, chủ động ép server gửi lại danh sách mới nhất
        String username = com.uet.auction.client.util.SessionManager.getCurrentUsername();
        if (username != null) {
            SocketClient.sendRequest(new AuctionRequest("GET_NOTIFICATIONS", username));
        }
    }

    @FXML
    private void onBackClick() {
        if (onBackAction != null) {
            onBackAction.run();
        }
    }

    /**
     * Hàm vẽ động các thông báo nhận từ Server lên màn hình
     */
    public void displayNotifications(List<NotificationDTO> notifications) {
        notifContainer.getChildren().clear();

        if (notifications == null || notifications.isEmpty()) {
            Label lblEmpty = new Label("Hộp thư trống! Bạn không có thông báo chưa đọc nào.");
            lblEmpty.setStyle("-fx-font-style: italic; -fx-text-fill: grey; -fx-font-size: 14px;");
            notifContainer.getChildren().add(lblEmpty);
            return;
        }

        for (NotificationDTO notif : notifications) {
            // Khung bọc ngoài cho mỗi thông báo (Card)
            HBox card = new HBox();
            card.setSpacing(15);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setStyle("-fx-background-color: #ffffff; -fx-padding: 12; -fx-background-radius: 6; "
                    + "-fx-border-color: #e0e0e0; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 1);");

            // Phần text bên trái card
            VBox textSide = new VBox();
            textSide.setSpacing(4);
            HBox.setHgrow(textSide, Priority.ALWAYS);

            Label lblType = new Label("[" + notif.getType() + "]");
            lblType.setStyle("-fx-font-weight: bold; -fx-text-fill: #1976D2; -fx-font-size: 13px;");

            Label lblMsg = new Label(notif.getMessage());
            lblMsg.setWrapText(true);
            lblMsg.setStyle("-fx-font-size: 14px; -fx-text-fill: #222;");

            Label lblTime = new Label("🕒 " + notif.getCreatedAtStr());
            lblTime.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");

            textSide.getChildren().addAll(lblType, lblMsg, lblTime);

            // Nút "Đã đọc" bên phải card
            Button btnMarkRead = new Button("Đã đọc ✓");
            btnMarkRead.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
            btnMarkRead.setOnAction(e -> {
                // 1. Gửi lệnh báo Server xóa/đổi trạng thái thông báo này trong DB
                SocketClient.sendRequest(new AuctionRequest("MARK_NOTIFICATION_READ", notif.getId()));

                // 2. Xóa luôn card này khỏi giao diện ngay lập tức cho mượt
                notifContainer.getChildren().remove(card);

                // 3. Bảo server nạp lại danh sách để tự động trừ số lượng hiển thị trên icon Chuông
                String username = com.uet.auction.client.util.SessionManager.getCurrentUsername();
                if (username != null) {
                    SocketClient.sendRequest(new AuctionRequest("GET_NOTIFICATIONS", username));
                }
            });

            card.getChildren().addAll(textSide, btnMarkRead);
            notifContainer.getChildren().add(card);
        }
    }
}