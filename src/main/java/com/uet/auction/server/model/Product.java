package com.uet.auction.server.model;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDateTime;

import static jdk.nio.zipfs.ZipFileAttributeView.AttrID.owner;

//Sản phẩm
public class Product {
    int id;
    String name;
    double startingPrice;
    private String ownerName;
    private java.time.LocalDateTime endTime;
    private String status;

    public Product(int id, String name, double startingPrice,String owner) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.startingPrice = startingPrice;
        this.isActive = false;
    }


    public double setEndingPrice(double bidAmount) {
        endingPrice = bidAmount;
        return bidAmount;
    }
    public double getEndingPrice() {
        return endingPrice;
    }
    public ChronoLocalDateTime<?> getEndTime() {
        return endTime;
    }

    public User setHighestBidder(User user) {
        this.HighestBidder = user;
        return user;
    }
    public String getOwner() {
        return owner;
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public void setName(String name) {
        this.name = name;
    }

    public void setStartTime(LocalDateTime start) {
        this.startTime = start;
    }


    public void setEndTime(LocalDateTime end) {
        this.endTime = end;
    }

    public void setActive(boolean b) {
        this.isActive = b;
    }

    public int getPrice() {
        return (int) startingPrice;
    }

    public String getName() {
        return name;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public ChronoLocalDateTime getStartTime() {
        return startTime;

    }
    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    public String getOwnerName() {
    }
}
