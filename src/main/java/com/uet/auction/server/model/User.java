package com.uet.auction.server.model;
//Người dùng
public class User {
    public Role getRole() {
        return role;
    }

    public void addNotification(String message) {

    }

    public enum Role {
        ADMIN, SELLER, USER
    }
    private int id ;
    private String username;
    private Role role;
    private String password;
    double balance;
    public User(int id, String username,Role role, String password, double balance) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.password = password;
        this.balance = balance;
    }
    public User( String username,Role role) {
        this.username = username;
        this.role = role;

    }
    public double getBalance() {
        return balance;
    }
    public String getUsername() {
        return username;
    }
    public String getPassword() {
        return password;
    }
}
