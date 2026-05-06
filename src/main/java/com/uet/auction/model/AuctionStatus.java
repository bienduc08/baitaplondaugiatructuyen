package com.uet.auction.model;

public enum AuctionStatus {
    OPEN("Mở"),
    RUNNING("Đang diễn ra"),
    FINISHED("Kết thúc"),
    PAID("Đã thanh toán"),
    CANCELED("Đã hủy");

    private final String display;

    AuctionStatus(String display) { this.display = display; }

    public String getDisplay() { return display; }

    @Override
    public String toString() { return display; }
}