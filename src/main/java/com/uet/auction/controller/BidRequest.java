package com.uet.auction.controller; // Hoặc package bạn chọn

import com.uet.auction.model.Product;
import com.uet.auction.model.User;

public class BidRequest {
    private User user;
    private Product product;
    private double amount;
    private int id;

    public BidRequest(User user, int id , double amount) {
        this.user = user;
        this.id = id;
        this.amount = amount;


    }

    // Getter và Setter (Hoặc dùng @Data nếu có Lombok)
    public User getUser() { return user; }
    public int getId() { return id; }


    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

}