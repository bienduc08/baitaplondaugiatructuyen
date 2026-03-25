package com.uet.auction.model;

import java.time.LocalDate;

public class Product {
    private final int id;
    final String name;
    final String description;
    final double startingPrice;
    double endingPrice;
    final LocalDate startDate;
    final LocalDate endDate;
    public Product(int id, String name, String description, double startingPrice, LocalDate startDate, LocalDate endDate, String name1) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.startDate = startDate;
        this.endDate = endDate;
    }
    public void updateEndingPrice() {
        endingPrice = startingPrice+endingPrice;
    }
}
