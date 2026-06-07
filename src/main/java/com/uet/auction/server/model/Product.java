package com.uet.auction.server.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Product implements Serializable {
    private int id;
    private String name;
    private BigDecimal startingPrice;
    private BigDecimal currentPrice;
    private String description;
    private String sellerName;
    private String ownerName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;

    public Product() {}

    public Product(int id, String name, BigDecimal startingPrice, BigDecimal currentPrice,
                   String sellerName, LocalDateTime startTime, LocalDateTime endTime, String status) {
        this.id = id;
        this.name = name;
        this.startingPrice = startingPrice;
        this.currentPrice = currentPrice;
        this.sellerName = sellerName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getStartingPrice() { return startingPrice; }
    public void setStartingPrice(BigDecimal startingPrice) { this.startingPrice = startingPrice; }

    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}