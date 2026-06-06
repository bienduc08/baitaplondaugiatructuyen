package com.uet.auction.common.DTO;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử đơn vị cho lớp dữ liệu trung chuyển BidDTO.
 * Đảm bảo các thông tin đặt giá khi gửi về màn hình lịch sử JavaFX của Client
 * được định dạng, đọc/ghi và chuyển đổi chuỗi log chính xác.
 */
public class BidDTOTest {

    /**
     * Kiểm thử Constructor mặc định.
     * Đảm bảo các thông tin ban đầu trống/null để thư viện Gson/Jackson map dữ liệu sạch.
     */
    @Test
    public void testDefaultConstructor() {
        BidDTO dto = new BidDTO();
        assertNotNull(dto, "Đối tượng BidDTO không được null");
        assertNull(dto.getId(), "ID mặc định phải là null");
        assertNull(dto.getProductId(), "ProductId mặc định phải là null");
        assertNull(dto.getBiddername(), "Tên người đặt mặc định phải là null");
        assertNull(dto.getPrice(), "Mức giá mặc định phải là null");
        assertNull(dto.getTime(), "Thời gian mặc định phải là null");
        assertNull(dto.getStatus(), "Trạng thái đặt giá mặc định phải là null");
    }

    /**
     * Kiểm thử Constructor đầy đủ tham số.
     * Xác minh dữ liệu đầu vào (tên người đặt dạng chữ, thời gian dạng chữ) được lưu trữ đúng.
     */
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

    /**
     * Kiểm thử các hàm Getter và Setter.
     * Đảm bảo Client (JavaFX TableView) có thể đọc dữ liệu từ các cột thông qua thuộc tính và cơ chế PropertyValueFactory.
     */
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

    /**
     * Kiểm thử hàm toString().
     * Đảm bảo chuỗi văn bản debug chứa đầy đủ thông tin về mức giá, tên người đặt và trạng thái.
     */
    @Test
    public void testToString() {
        BidDTO dto = new BidDTO(12, 345, "bob_bidder", 120000.0, "2026-06-01 21:00:00", "Hợp lệ");

        String str = dto.toString();
        assertNotNull(str, "Chuỗi toString không được null");
        assertTrue(str.contains("bob_bidder"), "Chuỗi phải chứa Tên người đặt");
        assertTrue(str.contains("120000.0"), "Chuỗi phải chứa Mức giá");
        assertTrue(str.contains("Hợp lệ"), "Chuỗi phải chứa Trạng thái đặt giá");
        assertTrue(str.contains("345"), "Chuỗi phải chứa ID sản phẩm");
        assertTrue(str.contains("12"), "Chuỗi phải chứa ID lượt đặt");
    }
}
