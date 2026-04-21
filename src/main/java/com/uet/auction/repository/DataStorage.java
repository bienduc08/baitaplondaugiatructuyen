package com.uet.auction.repository;

import com.uet.auction.model.Product;
import com.uet.auction.model.User;
import org.springframework.stereotype.Repository;;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;


@Repository
public class DataStorage {
    private static List<Product> products = new ArrayList<>();

    public DataStorage() {
        // Cấu trúc: id, name, startingPrice, startTime, endTime, ownerName

        // 1. Sản phẩm đang diễn ra (Bắt đầu từ 1 tiếng trước, kết thúc sau 1 ngày)
    }

    public List<Product> getAllProducts() {
        return products;
    }

    public static Product findProductById(int id) {
        return products.stream()
                .filter(p -> p.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public static Product getProductById(int productId) {
        for (Product p : products) {
            if (p.getId() == productId) {
                return p; // Tìm thấy rồi, trả về cả đối tượng Product
            }
        }
        return null;
    }
    public static  List<Product> getAll() {

        return products;
    }

    public void addProduct(Product newP) {
        this.products.add(newP);
    }
    private List<Product> pendingProducts = new ArrayList<>();
    //=================================================================================
    // Hàm lấy danh sách sản phẩm đang chờ duyệt
    public List<Product> getPendingProducts() {
        return pendingProducts;
    }

    // Hàm thêm sản phẩm vào danh sách chờ (Dùng cho Seller)
    public void addPendingProduct(Product product) {
        if (product != null) {
            this.pendingProducts.add(product);
        }
    }

    // Hàm xóa sản phẩm khỏi danh sách chờ (Dùng sau khi Admin đã duyệt hoặc từ chối)
    public void removePendingProduct(Product product) {
        this.pendingProducts.remove(product);
    }
    public void approveProduct(Product product) {
        // 1. Xóa khỏi danh sách chờ
        pendingProducts.remove(product);
        // 2. Thêm vào danh sách chính thức
        products.add(product);
    }
    //================================================================================
    private static List<User> users = new ArrayList<>();

    // Hàm lấy toàn bộ danh sách người dùng
    public List<User> getUsers() {
        return users;
    }

    // Hàm thêm người dùng mới (Dùng khi Đăng ký)
    public void addUser(User user) {
        users.add(user);
    }

    // Hàm cực kỳ quan trọng để phục vụ lệnh notify của Admin
    public User getUserByUsername(String username) {
        return users.stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }

}