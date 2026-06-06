package com.uet.auction.common.DTO;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử đơn vị cho lớp dữ liệu trung chuyển UserDTO.
 * Đảm bảo các thông tin người dùng gửi qua mạng (không chứa mật khẩu)
 * được khởi tạo, đọc/ghi và chuyển đổi sang chuỗi debug một cách chính xác.
 */
public class UserDTOTest {

    /**
     * Kiểm thử Constructor mặc định.
     * Xác nhận các giá trị ban đầu trống/null/0.0 trước khi nhận dữ liệu từ JSON.
     */
    @Test
    public void testDefaultConstructor() {
        UserDTO dto = new UserDTO();
        assertNotNull(dto, "Đối tượng UserDTO không được null");
        assertEquals(0, dto.getId(), "ID mặc định phải bằng 0");
        assertNull(dto.getFullName(), "Họ tên mặc định phải là null");
        assertNull(dto.getUsername(), "Username mặc định phải là null");
        assertNull(dto.getGmail(), "Gmail mặc định phải là null");
        assertNull(dto.getPhoneNumber(), "Số điện thoại mặc định phải là null");
        assertNull(dto.getRole(), "Vai trò mặc định phải là null");
        assertNull(dto.getStatus(), "Trạng thái tài khoản mặc định phải là null");
        assertEquals(0.0, dto.getBalance(), "Số dư mặc định phải là 0.0");
    }

    /**
     * Kiểm thử Constructor đầy đủ tham số.
     * Xác minh dữ liệu được gán đúng vào các cột tương ứng để chuẩn bị gửi về Client hiển thị.
     */
    @Test
    public void testParameterizedConstructor() {
        UserDTO dto = new UserDTO("Jane Doe", "jane@gmail.com", "0909090909", 55, "jane_username", "SELLER");

        assertEquals(55, dto.getId());
        assertEquals("Jane Doe", dto.getFullName());
        assertEquals("jane@gmail.com", dto.getGmail());
        assertEquals("0909090909", dto.getPhoneNumber());
        assertEquals("jane_username", dto.getUsername());
        assertEquals("SELLER", dto.getRole());
    }

    /**
     * Kiểm thử các hàm Getter và Setter.
     * Đảm bảo Client và thư viện ánh xạ JSON (như Gson) có thể chỉnh sửa và đọc các giá trị bình thường.
     */
    @Test
    public void testSettersAndGetters() {
        UserDTO dto = new UserDTO();
        
        dto.setId(77);
        dto.setFullName("David Beckham");
        dto.setGmail("david@gmail.com");
        dto.setPhoneNumber("0911111111");
        dto.setUsername("beckham7");
        dto.setRole("ADMIN");
        dto.setMessage("Thao tác thành công");
        dto.setBalance(88000.5);
        dto.setStatus("ACTIVE");

        assertEquals(77, dto.getId());
        assertEquals("David Beckham", dto.getFullName());
        assertEquals("david@gmail.com", dto.getGmail());
        assertEquals("0911111111", dto.getPhoneNumber());
        assertEquals("beckham7", dto.getUsername());
        assertEquals("ADMIN", dto.getRole());
        assertEquals("Thao tác thành công", dto.getMessage());
        assertEquals(88000.5, dto.getBalance());
        assertEquals("ACTIVE", dto.getStatus());
    }

    /**
     * Kiểm thử hàm toString().
     * Đảm bảo chuỗi văn bản sinh ra khi in log chứa đầy đủ thông tin nhận diện để lập trình viên tiện theo dõi.
     */
    @Test
    public void testToString() {
        UserDTO dto = new UserDTO("Jane Doe", "jane@gmail.com", "0909090909", 55, "jane_username", "SELLER");
        dto.setBalance(5000.0);
        dto.setStatus("LOCKED");

        String str = dto.toString();
        assertNotNull(str, "Chuỗi toString không được null");
        assertTrue(str.contains("jane_username"), "Chuỗi phải chứa Username");
        assertTrue(str.contains("SELLER"), "Chuỗi phải chứa Vai trò");
        assertTrue(str.contains("55"), "Chuỗi phải chứa ID");
        assertTrue(str.contains("5000.0"), "Chuỗi phải chứa Số dư");
        assertTrue(str.contains("LOCKED"), "Chuỗi phải chứa Trạng thái tài khoản");
    }
}
