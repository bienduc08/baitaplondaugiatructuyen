package com.uet.auction.controller;

import com.uet.auction.model.User;

import java.math.BigDecimal;

public class BidRequest {

    private final User       user;
    private final int        auctionId;
    private final BigDecimal amount;

    public BidRequest(User user, int auctionId, BigDecimal amount) {
        this.user      = user;
        this.auctionId = auctionId;
        this.amount    = amount;
    }

    public User       getUser()      { return user; }
    public int        getAuctionId() { return auctionId; }
    public BigDecimal getAmount()    { return amount; }
}