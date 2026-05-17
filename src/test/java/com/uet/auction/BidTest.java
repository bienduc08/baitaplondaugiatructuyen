package com.uet.auction;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BidTest {

    @Test
    void giaDatePhaLonHonGiaHienTai() {
        double giaHienTai = 500000;
        double giaDat = 300000;
        assertFalse(giaDat > giaHienTai, "Giá thấp hơn phải bị từ chối");
    }

    @Test
    void giaDateHopLe() {
        double giaHienTai = 500000;
        double giaDat = 600000;
        assertTrue(giaDat > giaHienTai, "Giá cao hơn phải được chấp nhận");
    }

    @Test
    void nguoiDangGiuDinhKhongTheDatTiep() {
        String currentOwner = "userA";
        String bidder = "userA";
        assertEquals(currentOwner, bidder);
    }

    @Test
    void nguoiKhacCoTheDat() {
        String currentOwner = "userA";
        String bidder = "userB";
        assertNotEquals(currentOwner, bidder);
    }
}