package com.uet.auction.model;

import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDateTime;

//Sản phẩm
public class Product {
    private final int id;
    String owner;
    String name;
    double startingPrice;
    double endingPrice;
    ChronoLocalDateTime<?> startTime;
    ChronoLocalDateTime<?> endTime;
    String HighestBidder;
    private boolean isActive ;

    public Product(int id, String name, double startingPrice,String owner) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.startingPrice = startingPrice;
        this.HighestBidder = "";
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
        HighestBidder = this.name;
        return user;
    }
    public String getOwner() {
        return owner;
    }
    public int getId() {
        return id;
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
}
