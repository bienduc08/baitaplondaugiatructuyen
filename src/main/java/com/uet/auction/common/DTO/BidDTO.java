package com.uet.auction.common.DTO;

import java.io.Serializable;

public class BidDTO implements Serializable {

    // Đảm bảo tính toàn vẹn dữ liệu khi truyền qua mạng (từ Server về Client)
    private static final long serialVersionUID = 1L;

    private Integer id;             // ID của lượt trả giá
    private Integer productId;      // ID của sản phẩm
    private String userName;      // Tên người trả giá (Khớp với PropertyValueFactory)
    private Double price;           // Số tiền trả giá
    private String time;            // Thời gian (để String cho dễ hiển thị lên JavaFX)
    private String status;          // Trạng thái (VD: "Hợp lệ", "Bị hủy")

    // 1. Constructor mặc định (Bắt buộc phải có để thư viện Gson/Jackson map dữ liệu JSON)
    public BidDTO() {
    }

    // 2. Constructor đầy đủ tham số (Dùng để tạo object nhanh hoặc test)
    public BidDTO(Integer id, Integer productId, String userName, Double price, String time, String status) {
        this.id = id;
        this.productId = productId;
        this.userName = userName;
        this.price = price;
        this.time = time;
        this.status = status;
    }

    // =========================================================
    // 3. GETTERS & SETTERS (BẮT BUỘC ĐỂ TABLEVIEW ĐỌC ĐƯỢC DATA)
    // =========================================================

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // =========================================================
    // 4. TO STRING (Hỗ trợ in ra Console để check lỗi - Debug)
    // =========================================================
    @Override
    public String toString() {
        return "BidDTO{" +
                "id=" + id +
                ", productId=" + productId +
                ", userName='" + userName + '\'' +
                ", price=" + price +
                ", time='" + time + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}