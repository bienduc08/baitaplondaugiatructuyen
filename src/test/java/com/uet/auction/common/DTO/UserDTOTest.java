package com.uet.auction.common.DTO;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UserDTOTest {

    @Test
    public void testDefaultConstructor() {
        UserDTO dto = new UserDTO();
        assertNotNull(dto);
        assertEquals(0, dto.getId());
        assertNull(dto.getFullName());
        assertNull(dto.getUsername());
        assertNull(dto.getGmail());
        assertNull(dto.getPhoneNumber());
        assertNull(dto.getRole());
        assertNull(dto.getStatus());
        assertEquals(0.0, dto.getBalance());
    }

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

    @Test
    public void testToString() {
        UserDTO dto = new UserDTO("Jane Doe", "jane@gmail.com", "0909090909", 55, "jane_username", "SELLER");
        dto.setBalance(5000.0);
        dto.setStatus("LOCKED");

        String str = dto.toString();
        assertNotNull(str);
        assertTrue(str.contains("jane_username"));
        assertTrue(str.contains("SELLER"));
        assertTrue(str.contains("55"));
        assertTrue(str.contains("5000.0"));
        assertTrue(str.contains("LOCKED"));
    }
}
