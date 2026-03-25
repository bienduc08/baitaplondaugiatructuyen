package com.uet.auction.model;

public class User {
    int id ;
    String username;
    String password;
    double balance;
    public User(int id, String username, String password, double balance) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.balance = balance;
    }

}
