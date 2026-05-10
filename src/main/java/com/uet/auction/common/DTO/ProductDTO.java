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
    private String sellerName;    // Người đăng bán
    private String ownerName;     // Người đang giữ mức giá cao nhất
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    // PENDING, OPEN, CLOSED, REJECTED

    // Constructor không tham số (Bắt buộc phải có để các thư viện như Jackson/Serialization hoạt động)
    public ProductDTO() {
    }

    // Constructor đầy đủ để tạo nhanh đối tượng tại Server từ kết quả Database
    public ProductDTO(int id, String name, double currentPrice, String sellerName, String ownerName, LocalDateTime endTime, String status) {
        this.id = id;
        this.name = name;
        this.currentPrice = currentPrice;
        this.sellerName = sellerName;
        this.ownerName = ownerName;
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