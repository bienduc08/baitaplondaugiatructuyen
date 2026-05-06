package com.uet.auction.model;

import java.math.BigDecimal;

public class Vehicle extends com.uet.auction.model.Item {
    private String make;
    private String vehicleModel;
    private int year;
    private int mileage;

    public Vehicle() { super(); }

    public Vehicle(String name, String description, BigDecimal startingPrice, String sellerId,
                   String make, String vehicleModel, int year, int mileage) {
        super(name, description, startingPrice, sellerId, com.uet.auction.model.ItemCategory.VEHICLE);
        this.make = make;
        this.vehicleModel = vehicleModel;
        this.year = year;
        this.mileage = mileage;
    }

    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }

    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String vehicleModel) { this.vehicleModel = vehicleModel; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getMileage() { return mileage; }
    public void setMileage(int mileage) { this.mileage = mileage; }

    @Override
    public void printInfo() {
        System.out.println("[VEHICLE] " + make + " " + vehicleModel
                + " (" + year + ") | Đã đi: " + mileage + "km | Giá KĐ: " + startingPrice);
    }
}