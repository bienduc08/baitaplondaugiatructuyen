package com.uet.auction.common.exception;

public class AuctionException extends Exception {
    private String errorCode;

    public AuctionException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}