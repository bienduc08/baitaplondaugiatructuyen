package com.uet.auction.common.exception;

import java.io.Serializable;

/**
 * Ngoại lệ cơ bản cho toàn bộ hệ thống đấu giá.
 * Implement Serializable để có thể gửi thông báo lỗi qua Socket.
 */
public class AuctionException extends Exception implements Serializable {
    private static final long serialVersionUID = 1L;

    public AuctionException(String message) {
        super(message);
    }
}