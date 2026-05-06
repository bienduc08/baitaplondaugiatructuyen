package com.uet.auction.server.service;

import com.uet.auction.common.DTO.ProductDTO;
import com.uet.auction.server.DAO.ProductDAO;
import com.uet.auction.server.DAO.BidDAO;
import com.uet.auction.common.Response.AuctionResponse;
import com.uet.auction.server.network.SocketServer;

import java.util.List;

public class AuctionService {
    private final ProductDAO productDAO;
    private final BidDAO bidDAO;

    public AuctionService() {
        this.productDAO = new ProductDAO();
        this.bidDAO = new BidDAO();
    }

    // Lấy danh sách sản phẩm mới nhất từ MySQL
    public AuctionResponse listAllProducts() {
        List<ProductDTO> products = productDAO.getAllProducts();
        return new AuctionResponse(true, "Lấy danh sách thành công", products);
    }

    // Xử lý logic khi người dùng bấm nút Đặt giá
    public AuctionResponse placeBid(int productId, String username, double amount) {
        // Kiểm tra xem lượt đặt giá có hợp lệ trong DB không
        boolean success = bidDAO.placeBid(productId, username, amount);

        if (success) {
            return new AuctionResponse(true, "Bạn đã đặt giá thành công!", null);
        } else {
            return new AuctionResponse(false, "Giá đặt phải cao hơn giá hiện tại!", null);
        }
    }
    public AuctionResponse register(String username, String password) {
        boolean success = userDAO.registerUser(username, password);
        if (success) {
            return new AuctionResponse(true, "REGISTER_RESULT", "Tạo tài khoản thành công!");
        } else {
            return new AuctionResponse(false, "REGISTER_RESULT", "Tên đăng nhập đã tồn tại!");
        }
    }
    public AuctionResponse addProduct(String name, double price, int days) {
        boolean success = productDAO.addProduct(name, price, days);
        if (success) {
            // Broadcast để tất cả Client đang mở tab User tự động tải lại danh sách sản phẩm mới!
            SocketServer.broadcast(new AuctionResponse(true, "UPDATE_PRICE", "Có sản phẩm mới!"));
            return new AuctionResponse(true, "ADD_PRODUCT_RESULT", "Thêm sản phẩm thành công!");
        }
        return new AuctionResponse(false, "ADD_PRODUCT_RESULT", "Lỗi CSDL khi thêm sản phẩm.");
    }
}