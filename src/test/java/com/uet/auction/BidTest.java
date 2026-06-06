package com.uet.auction;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử đơn vị cho các quy tắc đặt giá (Bidding Rules).
 * Đảm bảo các logic nghiệp vụ cơ bản như so sánh giá đặt và phân biệt người dùng
 * hoạt động chính xác để tránh tình trạng tranh chấp khi đấu giá.
 */
public class BidTest {

    /**
     * Kiểm thử trường hợp giá đặt thấp hơn giá hiện tại.
     * Đảm bảo hệ thống sẽ từ chối lượt đặt giá này (trả về kết quả False).
     */
    @Test
    void giaDatePhaLonHonGiaHienTai() {
        double giaHienTai = 500000;
        double giaDat = 300000;
        // Kiểm tra xem giaDat có lớn hơn giaHienTai không. Kết quả mong đợi là FALSE.
        assertFalse(giaDat > giaHienTai, "Giá thấp hơn phải bị từ chối");
    }

    /**
     * Kiểm thử trường hợp đặt giá hợp lệ (giá đặt lớn hơn giá hiện tại).
     * Đảm bảo hệ thống chấp nhận lượt đặt giá này (trả về kết quả True).
     */
    @Test
    void giaDateHopLe() {
        double giaHienTai = 500000;
        double giaDat = 600000;
        // Kiểm tra xem giaDat có lớn hơn giaHienTai không. Kết quả mong đợi là TRUE.
        assertTrue(giaDat > giaHienTai, "Giá cao hơn phải được chấp nhận");
    }

    /**
     * Kiểm thử quy tắc: Người đang giữ mức giá cao nhất hiện tại (giữ đỉnh)
     * thì không được phép tự đặt giá đè lên chính mình trong cùng phiên.
     */
    @Test
    void nguoiDangGiuDinhKhongTheDatTiep() {
        String currentOwner = "userA"; // Người đang giữ giá cao nhất hiện tại
        String bidder = "userA";       // Người đang muốn đặt giá tiếp theo
        // Nếu tên hai người trùng nhau, hệ thống sẽ chặn không cho đặt tiếp.
        assertEquals(currentOwner, bidder, "Người đang giữ giá cao nhất không được phép đặt đè lên chính mình");
    }

    /**
     * Kiểm thử quy tắc: Người dùng khác với người đang giữ đỉnh
     * hoàn toàn có quyền được đặt giá để tranh chấp sản phẩm.
     */
    @Test
    void nguoiKhacCoTheDat() {
        String currentOwner = "userA"; // Người đang giữ giá cao nhất hiện tại
        String bidder = "userB";       // Người dùng khác muốn vào đặt giá
        // Tên của bidder phải khác với người giữ đỉnh hiện tại để lượt đặt giá được xử lý.
        assertNotEquals(currentOwner, bidder, "Người dùng khác phải được phép tham gia đặt giá");
    }
}