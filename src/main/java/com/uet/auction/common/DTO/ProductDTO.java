package com.uet.auction.common.DTO;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Data Transfer Object cho Sản phẩm.
 * Dùng để truyền dữ liệu sản phẩm giữa Client và Server qua Socket.
 */
public class ProductDTO implements Serializable {
    // serialVersionUID đảm bảo tính tương thích khi truyền đối tượng qua mạng
    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private double startingPrice;
    private double currentPrice;
    private String description;
    private String sellerName;
    private String ownerName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private String category;
    private String imageUrl;
    private double stepPrice;
    private int bidCount;
    private byte[] imageBytes;

    // Constructor không tham số (Bắt buộc phải có để các thư viện như Jackson/Serialization hoạt động)
    public ProductDTO() {
    }

    // Constructor đầy đủ để tạo nhanh đối tượng tại Server từ kết quả Database
    public ProductDTO(int id, String name, double currentPrice,double stepPrice, String sellerName, String ownerName, LocalDateTime startTime, LocalDateTime endTime, String status) {
        this.id = id;
        this.name = name;
        this.currentPrice = currentPrice;
        this.stepPrice = stepPrice;
        this.sellerName = sellerName;
        this.ownerName = ownerName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    // --- GETTERS & SETTERS ---
    // Lưu ý: Tên các Getter phải khớp với PropertyValueFactory trong TableView của JavaFX

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public double getStepPrice() { return stepPrice; }
    public void setStepPrice(double stepPrice) { this.stepPrice = stepPrice; }

    public int getBidCount() { return bidCount; }
    public void setBidCount(int bidCount) { this.bidCount = bidCount; }

    public byte[] getImageBytes() { return imageBytes; }
    public void setImageBytes(byte[] imageBytes) { this.imageBytes = imageBytes; }

    @Override
    public String toString() {
        return "ProductDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", currentPrice=" + currentPrice +
                ", status='" + status + '\'' +
                '}';
    }
}