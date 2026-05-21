package com.uet.auction.common.Request;

import java.io.Serializable;

public class AuctionRequest implements Serializable {
    private static final long serialVersionUID = 1L; // THÊM

    private String type;
    private Object data;

    public AuctionRequest(String type, Object data) {
        this.type = type;
        this.data = data;
    }

    public String getType() { return type; }
    public Object getData() { return data; }
}