package com.uet.auction.common.Response;

import java.io.Serializable;

public class AuctionResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    private boolean success;
    private String message;
    private Object data;
    private String type;
    // Chứa UserDTO hoặc List<ProductDTO>...

    public AuctionResponse(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
}