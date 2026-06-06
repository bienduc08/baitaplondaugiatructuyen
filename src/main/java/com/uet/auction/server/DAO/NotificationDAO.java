package com.uet.auction.server.DAO;

import com.uet.auction.common.DTO.NotificationDTO;
import com.uet.auction.server.config.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {

    /**
     * Tạo một thông báo mới trong Database
     */
    public boolean insertNotification(String username, String message, String type) {
        String sql = "INSERT INTO notifications (username, message, type, is_read, created_at) VALUES (?, ?, ?, FALSE, NOW())";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, message);
            pstmt.setString(3, type);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[NotificationDAO] Lỗi khi tạo thông báo: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Lấy danh sách các thông báo chưa đọc của một user
     */
    public List<NotificationDTO> getUnreadNotifications(String username) {
        List<NotificationDTO> list = new ArrayList<>();
        String sql = "SELECT id, username, message, type, is_read, DATE_FORMAT(created_at, '%d-%m-%Y %H:%i:%s') as created_at_str " +
                "FROM notifications WHERE username = ? AND is_read = FALSE ORDER BY created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                list.add(new NotificationDTO(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("message"),
                        rs.getString("type"),
                        rs.getBoolean("is_read"),
                        rs.getString("created_at_str")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[NotificationDAO] Lỗi khi lấy thông báo: " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Đánh dấu một thông báo là đã đọc
     */
    public boolean markAsRead(int notificationId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, notificationId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[NotificationDAO] Lỗi khi update trạng thái đọc: " + e.getMessage());
            return false;
        }
    }
}