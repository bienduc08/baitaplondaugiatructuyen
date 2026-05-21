package com.uet.auction.server.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Auction extends Entity {
    private int itemId;
    private int sellerId;
    private int highestBidderId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal startingPrice;
    private BigDecimal currentPrice;
    private BigDecimal reservePrice;
    private BigDecimal minBidIncrement;
    private AuctionStatus status;
    private int extensionCount;
    private int totalBids;
    private List<BidTransaction> bidHistory;

    public Auction() {
        super();
        this.status = AuctionStatus.OPEN;
        this.extensionCount = 0;
        this.bidHistory = new ArrayList<>();
    }

    public Auction(int id, LocalDateTime createdAt, int itemId, int sellerId, int highestBidderId,
                   LocalDateTime startTime, LocalDateTime endTime, BigDecimal currentPrice,
                   AuctionStatus status, int extensionCount) {
        super(id, createdAt);
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.highestBidderId = highestBidderId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.currentPrice = currentPrice;
        this.status = status;
        this.extensionCount = extensionCount;
        this.bidHistory = new ArrayList<>();
    }

    public Auction(int itemId, int sellerId, BigDecimal startingPrice,
                   LocalDateTime startTime, LocalDateTime endTime) {
        super();
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AuctionStatus.OPEN;
        this.bidHistory = new ArrayList<>();
        this.totalBids = 0;
        this.extensionCount = 0;
        this.minBidIncrement = new BigDecimal("1000");
    }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }

    public int getSellerId() { return sellerId; }
    public void setSellerId(int sellerId) { this.sellerId = sellerId; }

    public int getHighestBidderId() { return highestBidderId; }
    public void setHighestBidderId(int highestBidderId) { this.highestBidderId = highestBidderId; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public BigDecimal getStartingPrice() { return startingPrice; }
    public void setStartingPrice(BigDecimal startingPrice) { this.startingPrice = startingPrice; }

    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }

    public BigDecimal getReservePrice() { return reservePrice; }
    public void setReservePrice(BigDecimal reservePrice) { this.reservePrice = reservePrice; }

    public BigDecimal getMinBidIncrement() { return minBidIncrement; }
    public void setMinBidIncrement(BigDecimal minBidIncrement) { this.minBidIncrement = minBidIncrement; }

    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }

    public int getExtensionCount() { return extensionCount; }
    public void setExtensionCount(int extensionCount) { this.extensionCount = extensionCount; }

    public int getTotalBids() { return totalBids; }
    public void setTotalBids(int totalBids) { this.totalBids = totalBids; }

    public List<BidTransaction> getBidHistory() { return bidHistory; }
    public void setBidHistory(List<BidTransaction> bidHistory) { this.bidHistory = bidHistory; }

    @Override
    public void printInfo() {
        System.out.println("Phiên đấu giá (Item ID: " + itemId + ") | Trạng thái: " + status
                + " | Giá hiện tại: " + currentPrice);
    }
}