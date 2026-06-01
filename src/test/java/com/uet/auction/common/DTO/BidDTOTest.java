package com.uet.auction.common.DTO;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BidDTOTest {

    @Test
    public void testDefaultConstructor() {
        BidDTO dto = new BidDTO();
        assertNotNull(dto);
        assertNull(dto.getId());
        assertNull(dto.getProductId());
        assertNull(dto.getBiddername());
        assertNull(dto.getPrice());
        assertNull(dto.getTime());
        assertNull(dto.getStatus());
    }

    @Test
    public void testParameterizedConstructor() {
        BidDTO dto = new BidDTO(12, 345, "bob_bidder", 120000.0, "2026-06-01 21:00:00", "Hợp lệ");

        assertEquals(12, dto.getId());
        assertEquals(345, dto.getProductId());
        assertEquals("bob_bidder", dto.getBiddername());
        assertEquals(120000.0, dto.getPrice());
        assertEquals("2026-06-01 21:00:00", dto.getTime());
        assertEquals("Hợp lệ", dto.getStatus());
    }

    @Test
    public void testSettersAndGetters() {
        BidDTO dto = new BidDTO();

        dto.setId(99);
        dto.setProductId(888);
        dto.setUserName("charlie");
        dto.setPrice(150000.5);
        dto.setTime("2026-06-01 21:15:00");
        dto.setStatus("Bị hủy");

        assertEquals(99, dto.getId());
        assertEquals(888, dto.getProductId());
        assertEquals("charlie", dto.getBiddername());
        assertEquals(150000.5, dto.getPrice());
        assertEquals("2026-06-01 21:15:00", dto.getTime());
        assertEquals("Bị hủy", dto.getStatus());
    }

    @Test
    public void testToString() {
        BidDTO dto = new BidDTO(12, 345, "bob_bidder", 120000.0, "2026-06-01 21:00:00", "Hợp lệ");

        String str = dto.toString();
        assertNotNull(str);
        assertTrue(str.contains("bob_bidder"));
        assertTrue(str.contains("120000.0"));
        assertTrue(str.contains("Hợp lệ"));
        assertTrue(str.contains("345"));
        assertTrue(str.contains("12"));
    }
}
