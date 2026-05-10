package com.uet.auction.client.util;

import com.uet.auction.common.DTO.UserDTO;

public class SessionManager {
    private static UserDTO currentUser;

    public static void setCurrentUser(UserDTO user) {
        currentUser = user;
    }

    public static UserDTO getCurrentUser() {
        return currentUser;
    }

    public static String getCurrentUsername() {
        return currentUser != null ? currentUser.getUsername() : null;
    }

    public static void clearSession() {
        currentUser = null;
    }
}