package com.uet.auction.server.model;
//Đấu giá
public class Bid {


    private final User user;
    private final Product product;
    private final double bidAmount;

    public Bid(User user, Product product, double bidAmount) {
        this.user= user;
        this.product = product;
        this.bidAmount = bidAmount;
    }
    public User getUser() {
        return user;
    }
    public Product getProduct() {
        return product;
    }
    public double getBidAmount() {
        return bidAmount;
    }

}
