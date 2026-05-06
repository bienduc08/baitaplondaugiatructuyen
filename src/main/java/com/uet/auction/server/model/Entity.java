package com.uet.auction.model;

import java.time.LocalDateTime;

public abstract class Entity {
    protected int id;
    protected LocalDateTime createdAt;

    public Entity() {
        this.createdAt = LocalDateTime.now();
    }

    public Entity(int id, LocalDateTime createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public abstract void printInfo();
}