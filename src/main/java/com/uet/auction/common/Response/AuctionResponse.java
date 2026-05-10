package com.uet.auction.common.Response;

import com.uet.auction.common.DTO.ProductDTO;

import java.io.Serializable;
import java.util.List;

/**
 * Lớp phản hồi chung từ Server gửi về cho Client
 */
public class AuctionResponse implements Serializable {
    // serialVersionUID giúp đồng bộ hóa khi truyền đối tượng qua Socket
    private static final long serialVersionUID = 1L;

    private boolean success;   // Trạng thái thành công hay thất bại
    private String type;      // Loại phản hồi (Ví dụ: LOGIN_RESULT, BID_RESULT, UPDATE_PRICE)
    private String message;   // Thông báo đi kèm (Ví dụ: "Đăng nhập thành công")
    private Object data;      // Dữ liệu thực tế (Có thể là UserDTO, List<ProductDTO>, v.v.)

    // Constructor mặc định
    public AuctionResponse() {
    }

    // Constructor đầy đủ cho các phản hồi có kèm dữ liệu
    public AuctionResponse(boolean success, String type, Object data) {
        this.success = success;
        this.type = type;
        this.data = data;
    }

    // Constructor có thêm message (Thường dùng cho các thông báo lỗi hoặc thành công)
    public AuctionResponse(boolean success, String type, String message, Object data) {
        this.success = success;
        this.type = type;
        this.message = message;
        this.data = data;
    }

    // --- GETTERS & SETTERS ---

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}