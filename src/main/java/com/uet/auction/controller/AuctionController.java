
package com.uet.auction.controller;

import com.uet.auction.model.User;
import com.uet.auction.service.AuctionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//Điều khiển đấu giá
@RestController // Cho Spring biết đây là nơi tiếp nhận API
@RequestMapping("/api/auctions") // Đường dẫn gốc cho các chức năng đấu giá
public class AuctionController {

    // Thay vì dùng 'new', hãy dùng Dependency Injection (Tiêm phụ thuộc)
    private final AuctionService auctionService;

    @Autowired // Spring sẽ tự động tìm và gán AuctionService vào đây
    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }
    @PostMapping("/bid")
    public boolean onBidButtonClick(@RequestBody BidRequest request) {
        // 1. Gọi Service để thực hiện đặt giá
        // Giả sử hàm placeBid trả về thông báo kết quả
        User user = request.getUser();
        if (user.getRole() != User.Role.USER) {
            System.out.println("Cảnh báo: " + user.getUsername() + " không có quyền đấu giá!");
            return false;
        }
        boolean result = auctionService.placeBid(
                request.getUser(),
                request.getId(),
                request.getAmount()
        );

        // 2. Sau khi cập nhật DB/DataStorage thành công, ra lệnh cho các GUI làm mới
        // Nếu dùng JavaFX, ta sẽ gọi hàm refresh ở đây (xem bước 2)

        return result;
    }

}