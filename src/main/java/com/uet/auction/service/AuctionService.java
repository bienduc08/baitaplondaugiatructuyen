package com.uet.auction.service;

import com.uet.auction.controller.UserView;
import com.uet.auction.model.Bid;
import com.uet.auction.model.Product;
import com.uet.auction.model.User;
import com.uet.auction.repository.DataStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

//import static com.sun.management.internal.GcInfoCompositeData.getEndTime;

//Dịch vụ đâ giá
@Service
public class AuctionService {
    private List<Product> products = new ArrayList<>();

    // Hoặc nếu dùng DataStorage:
    @Autowired
    private DataStorage dataStorage;
    /**
     * Hàm xử lý đặt giá (Mục 3.1.3 trong đề bài)
     * Trả về true nếu đặt giá thành công, false nếu vi phạm quy tắc.
     */
    public boolean placeBid(User user, Integer id, double bidAmount) {
        // 1. Kiểm tra thời gian: Đã hết giờ đấu giá chưa?
        if (LocalDateTime.now().isAfter(DataStorage.getProductById(id).getEndTime())) {
            System.out.println("Lỗi: Phiên đấu giá đã kết thúc!");
            return false;
        }

        // 2. Kiểm tra giá: Giá mới phải cao hơn giá hiện tại (Mục 3.1.3)
        if (bidAmount <= DataStorage.getProductById(id).getEndingPrice()) {
            System.out.println("Lỗi: Giá đặt phải cao hơn giá hiện tại (" +DataStorage.findProductById(id).getEndingPrice() + ")!");
            return false;
        }

        // 3. Kiểm tra số dư: Người dùng có đủ tiền không?
        if (user.getBalance() < bidAmount) {
            System.out.println("Lỗi: Số dư tài khoản không đủ để đặt mức giá này!");
            return false;
        }

        // 4. Nếu mọi thứ OK -> Cập nhật thông tin sản phẩm
        DataStorage.findProductById(id).setEndingPrice( bidAmount);
        DataStorage.getProductById(id).setHighestBidder(user);

        // 5. Lưu vào lịch sử đặt giá (Mục 3.1.3 - Log)
        Bid newBid = new Bid(user, DataStorage.findProductById(id), bidAmount);
        // (Sau này bạn có thể lưu newBid vào một danh sách trong Repository)

        System.out.println("Chúc mừng " + user.getUsername() + " đã dẫn đầu với mức giá: " + bidAmount);
        return true;
    }
    private List<UserView> activeViews = new ArrayList<>();

    // Hàm để mỗi khi một User mở máy lên thì "đăng ký" vào hệ thống
    public void registerView(UserView view) {
        activeViews.add(view);
    }

    public void notify(String username, String message) {
        // 1. Tìm user trong hệ thống (DataStorage)
        User targetUser = dataStorage.getUserByUsername(username);

        if (targetUser != null) {
            // 2. Thêm thông báo vào danh sách thông báo của User đó
            // (Bạn nên thêm List<String> notifications trong lớp User)
            targetUser.addNotification(message);

            System.out.println("[THÔNG BÁO RIÊNG] Tới " + username + ": " + message);
        }
    }

    public void notifyAllUsers(String message) {
        // Trong bài tập lớn, bạn có thể lưu message này vào một danh sách thông báo chung
        dataStorage.addSystemLog(message);

        // Nếu bạn có một View đang mở, hãy cập nhật TextArea của nó
        System.out.println("[HỆ THỐNG]: " + message);
    }


    public void sendToAdminForApproval(Product product) {

        if (product == null) {
            System.out.println("Lỗi: Sản phẩm không hợp lệ.");
            return;
        }

        // Logic kiểm tra dữ liệu trước khi gửi
        if (product.getPrice() <= 0) {
            System.out.println("Lỗi: Giá khởi điểm phải lớn hơn 0.");
            return;
        }

        // Đẩy sản phẩm vào kho dữ liệu chờ duyệt
        dataStorage.addProduct(product);

        System.out.println("Hệ thống: Sản phẩm '" + product.getName() + "' đã được gửi tới Admin.");
    }

    public void processApproval(Product p, boolean approved, String note) {
        if (approved) {
            // Bê từ danh sách chờ sang danh sách chính thức
            dataStorage.getPendingProducts().remove(p);
            dataStorage.getAllProducts().add(p);
            p.setActive(true);

            notifyAllUsers("Sản phẩm mới '" + p.getName() + "' đã được Admin duyệt lên sàn!");
        } else {
            // Xóa khỏi danh sách chờ và báo lỗi cho Seller
            dataStorage.getPendingProducts().remove(p);
            // Giả sử bạn có hàm gửi thông báo riêng cho User
            notify(p.getOwner(), "Sản phẩm bị từ chối. Lý do: " + note);
        }
    }
    public void updateAuctionTime(int productId, LocalDateTime start, LocalDateTime end) {
        Product p = dataStorage.getProductById(productId);
        if (p != null) {
            p.setStartTime(start);
            p.setEndTime(end);

            // Thông báo cho mọi người biết lịch trình đã thay đổi
            notifyAllUsers("Cập nhật lịch đấu giá sản phẩm " + p.getName() +
                    ": Bắt đầu lúc " + start + ", kết thúc lúc " + end);
        }
    }


}
