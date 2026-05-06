package com.uet.auction.common.Request;

import java.io.Serializable;

public class AuctionRequest implements Serializable {
    private String type; // LOGIN, BID, ADD_PRODUCT
    private Object data; // Chứa đối tượng cụ thể như LoginRequest hoặc ProductDTO

    public AuctionRequest(String type, Object data) {
        this.type = type;
        this.data = data;
    }

    public String getType() { return type; }
    public Object getData() { return data; }
}
