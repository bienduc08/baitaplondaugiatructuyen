package com.uet.auction.server.service;

/**
 * KHÔNG SỬ DỤNG — dead code.
 * Thông tin user đang đăng nhập được lưu tại ClientHandler.loggedInUser (per-connection).
 * Đừng nhầm với com.uet.auction.client.util.SessionManager dùng phía Client.
 *
 * @deprecated Không dùng class này. Xem ClientHandler.loggedInUser.
 */
@Deprecated
public class SessionManager {
    private SessionManager() {}
}