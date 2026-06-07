package com.uet.auction.common.DTO;

import java.io.Serializable;
import java.math.BigDecimal;

public class AuctionDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String productName;
    private BigDecimal currentPrice;   // Giá cao nhất hiện tại
    private BigDecimal myBid;          // Giá mà User này đã đặt
    private String status;         // "Dẫn đầu", "Bị vượt mặt", "Kết thúc"
    private String timeLeft;       // Thời gian còn lại (định dạng HH:mm:ss)
    private String imagePath;      // Đường dẫn ảnh sản phẩm

    public AuctionDTO(int id, String productName, BigDecimal currentPrice, BigDecimal myBid, String status, String timeLeft, String imagePath) {
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
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public BigDecimal getMyBid() { return myBid; }
    public String getStatus() { return status; }
    public String getTimeLeft() { return timeLeft; }
    public String getImagePath() { return imagePath; }
}