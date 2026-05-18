package com.uet.auction.common.DTO;

import java.io.Serializable;

public class AuctionDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String productName;
    private double currentPrice;   // Giá cao nhất hiện tại
    private double myBid;          // Giá mà User này đã đặt
    private String status;         // "Dẫn đầu", "Bị vượt mặt", "Kết thúc"
    private String timeLeft;       // Thời gian còn lại (định dạng HH:mm:ss)
    private String imagePath;      // Đường dẫn ảnh sản phẩm

    public AuctionDTO(int id, String productName, double currentPrice, double myBid, String status, String timeLeft, String imagePath) {
        this.id = id;
        this.productName = productName;
        this.currentPrice = currentPrice;
        this.myBid = myBid;
        this.status = status;
        this.timeLeft = timeLeft;
        this.imagePath = imagePath;
    }

    // Getter và Setter cho tất cả các trường
    public int getId() { return id; }
    public String getProductName() { return productName; }
    public double getCurrentPrice() { return currentPrice; }
    public double getMyBid() { return myBid; }
    public String getStatus() { return status; }
    public String getTimeLeft() { return timeLeft; }
    public String getImagePath() { return imagePath; }
}