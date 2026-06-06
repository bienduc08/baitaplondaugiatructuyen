package com.uet.auction.common.DTO;

import java.io.Serializable;

public class NotificationDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String username;
    private String message;
    private String type;
    private boolean isRead;
    private String createdAtStr;

    public NotificationDTO(int id, String username, String message, String type, boolean isRead, String createdAtStr) {
        this.id = id;
        this.username = username;
        this.message = message;
        this.type = type;
        this.isRead = isRead;
        this.createdAtStr = createdAtStr;
    }

    // Getters
    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getMessage() { return message; }
    public String getType() { return type; }
    public boolean isRead() { return isRead; }
    public String getCreatedAtStr() { return createdAtStr; }

    // Setters
    public void setRead(boolean read) { isRead = read; }
}
