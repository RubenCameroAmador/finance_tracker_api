package com.pocketminder.user.mapper;

import com.pocketminder.auth.entity.User;
import com.pocketminder.user.dto.UserResponseDTO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {

    private final UserMapper mapper = new UserMapper();

    @Test
    void toResponse_shouldMapAllFields() {
        User user = User.builder()
                .id(1L)
                .name("Ruben")
                .email("ruben@test.com")
                .password("hashed-password")
                .build();

        UserResponseDTO dto = mapper.toResponse(user);

        assertEquals(1L, dto.getId());
        assertEquals("Ruben", dto.getName());
        assertEquals("ruben@test.com", dto.getEmail());
    }

    @Test
    void toResponse_shouldNotExposePassword() {
        User user = User.builder()
                .id(2L)
                .name("Test")
                .email("test@test.com")
                .password("secret-hash")
                .build();

        UserResponseDTO dto = mapper.toResponse(user);

        assertDoesNotThrow(() -> {
            var fields = dto.getClass().getDeclaredFields();
            for (var field : fields) {
                assertFalse(field.getName().contains("password"),
                        "Response DTO should not contain password field");
            }
        });
    }

    @Test
    void toResponse_shouldHandleNullId() {
        User user = User.builder()
                .name("NoId")
                .email("noid@test.com")
                .build();

        UserResponseDTO dto = mapper.toResponse(user);

        assertNull(dto.getId());
        assertEquals("NoId", dto.getName());
        assertEquals("noid@test.com", dto.getEmail());
    }
}
