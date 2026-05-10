package com.uet.auction.server.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Bid extends Entity implements Serializable {
    private int userId;
    private int productId;
    private BigDecimal bidAmount;

    public Bid() { super(); }

    public Bid(int id, LocalDateTime createdAt, int userId, int productId, BigDecimal bidAmount) {
        super(id, createdAt);
        this.userId = userId;
        this.productId = productId;
        this.bidAmount = bidAmount;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public BigDecimal getBidAmount() { return bidAmount; }
    public void setBidAmount(BigDecimal bidAmount) { this.bidAmount = bidAmount; }

    @Override
    public void printInfo() {
        System.out.println("[BID] User ID " + userId + " đặt giá " + bidAmount
                + " cho sản phẩm ID " + productId);
    }
}