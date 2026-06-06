package com.uet.auction.server.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử đơn vị cho lớp mô hình Auction (Phiên đấu giá).
 * Đảm bảo các thuộc tính mặc định, các Constructor và các hàm cập nhật thông tin
 * hoạt động chính xác theo nghiệp vụ đấu giá.
 */
public class AuctionTest {

    /**
     * Kiểm thử Constructor mặc định.
     * Đảm bảo một phiên đấu giá mới được tạo ra sẽ ở trạng thái OPEN,
     * số lượt gia hạn là 0 và lịch sử đấu giá được khởi tạo rỗng.
     */
    @Test
    public void testDefaultConstructor() {
        Auction auction = new Auction();
        assertNotNull(auction, "Đối tượng Auction không được null");
        assertEquals(AuctionStatus.OPEN, auction.getStatus(), "Trạng thái ban đầu phải là OPEN");
        assertEquals(0, auction.getExtensionCount(), "Số lượt gia hạn ban đầu phải bằng 0");
        assertNotNull(auction.getBidHistory(), "Lịch sử đặt giá phải được khởi tạo");
        assertTrue(auction.getBidHistory().isEmpty(), "Lịch sử đặt giá ban đầu phải rỗng");
    }

    /**
     * Kiểm thử Constructor đầy đủ tham số (thường dùng để ánh xạ dữ liệu từ CSDL lên).
     * Đảm bảo tất cả các trường dữ liệu được gán đúng giá trị.
     */
    @Test
    public void testParameterizedConstructorWithAllFields() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusDays(1);
        BigDecimal currentPrice = new BigDecimal("15000");

        // Khởi tạo Auction với ID, thời gian tạo, mã vật phẩm, mã người bán, mã người trả giá cao nhất, v.v.
        Auction auction = new Auction(12, now, 5, 2, 8, now, end, currentPrice, AuctionStatus.OPEN, 3);

        assertEquals(12, auction.getId());
        assertEquals(now, auction.getCreatedAt());
        assertEquals(5, auction.getItemId());
        assertEquals(2, auction.getSellerId());
        assertEquals(8, auction.getHighestBidderId());
        assertEquals(now, auction.getStartTime());
        assertEquals(end, auction.getEndTime());
        assertEquals(currentPrice, auction.getCurrentPrice());
        assertEquals(AuctionStatus.OPEN, auction.getStatus());
        assertEquals(3, auction.getExtensionCount());
        assertNotNull(auction.getBidHistory());
    }

    /**
     * Kiểm thử Constructor khởi tạo nhanh phiên đấu giá mới của Người bán.
     * Xác minh các giá trị mặc định bổ sung như tổng số lượt đặt giá = 0,
     * bước giá tăng tối thiểu là 1000 VNĐ, v.v.
     */
    @Test
    public void testParameterizedConstructorWithItemDetails() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime end = now.plusHours(5);
        BigDecimal startingPrice = new BigDecimal("50000");

        Auction auction = new Auction(8, 3, startingPrice, now, end);

        assertEquals(8, auction.getItemId(), "Mã vật phẩm phải khớp");
        assertEquals(3, auction.getSellerId(), "Mã người bán phải khớp");
        assertEquals(startingPrice, auction.getStartingPrice(), "Giá khởi điểm phải khớp");
        assertEquals(startingPrice, auction.getCurrentPrice(), "Giá hiện tại ban đầu phải bằng giá khởi điểm");
        assertEquals(now, auction.getStartTime());
        assertEquals(end, auction.getEndTime());
        assertEquals(AuctionStatus.OPEN, auction.getStatus());
        assertEquals(0, auction.getTotalBids(), "Tổng số lượt đặt giá ban đầu phải bằng 0");
        assertEquals(0, auction.getExtensionCount(), "Số lần gia hạn ban đầu phải bằng 0");
        assertEquals(new BigDecimal("1000"), auction.getMinBidIncrement(), "Bước giá tối thiểu mặc định phải là 1000 VNĐ");
        assertNotNull(auction.getBidHistory());
    }

    /**
     * Kiểm thử các hàm Getter và Setter.
     * Đảm bảo việc thay đổi thông tin phiên đấu giá (như nâng giá hiện tại, thay đổi trạng thái,
     * thiết lập bước giá tối thiểu hay cập nhật danh sách đặt giá) hoạt động chính xác.
     */
    @Test
    public void testSettersAndGetters() {
        Auction auction = new Auction();
        BigDecimal price = new BigDecimal("75000");
        BigDecimal reserve = new BigDecimal("100000");
        BigDecimal increment = new BigDecimal("2000");
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusMinutes(30);

        auction.setItemId(10);
        auction.setSellerId(20);
        auction.setHighestBidderId(30);
        auction.setStartingPrice(price);
        auction.setCurrentPrice(price);
        auction.setReservePrice(reserve);
        auction.setMinBidIncrement(increment);
        auction.setStatus(AuctionStatus.FINISHED); // Trạng thái kết thúc phiên đấu giá
        auction.setExtensionCount(2);
        auction.setTotalBids(5);
        
        List<BidTransaction> history = new ArrayList<>();
        auction.setBidHistory(history);

        assertEquals(10, auction.getItemId());
        assertEquals(20, auction.getSellerId());
        assertEquals(30, auction.getHighestBidderId());
        assertEquals(price, auction.getStartingPrice());
        assertEquals(price, auction.getCurrentPrice());
        assertEquals(reserve, auction.getReservePrice());
        assertEquals(increment, auction.getMinBidIncrement());
        assertEquals(AuctionStatus.FINISHED, auction.getStatus());
        assertEquals(2, auction.getExtensionCount());
        assertEquals(5, auction.getTotalBids());
        assertSame(history, auction.getBidHistory(), "Lịch sử đặt giá phải cùng tham chiếu đối tượng");
    }
}
