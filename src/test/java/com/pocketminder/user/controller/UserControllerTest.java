package com.pocketminder.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pocketminder.auth.security.JwtService;
import com.pocketminder.auth.service.CustomUserDetailsService;
import com.pocketminder.user.dto.UpdateUserDTO;
import com.pocketminder.user.dto.UserResponseDTO;
import com.pocketminder.user.mapper.UserMapper;
import com.pocketminder.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void me_shouldReturnUserProfile() throws Exception {
        UserResponseDTO response = UserResponseDTO.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .build();

        when(userService.getCurrentUser()).thenReturn(null);
        when(userMapper.toResponse(any())).thenReturn(response);

        mockMvc.perform(get("/user/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void updateProfile_shouldReturnUpdatedUser() throws Exception {
        UpdateUserDTO request = new UpdateUserDTO();
        request.setName("Updated Name");

        UserResponseDTO response = UserResponseDTO.builder()
                .id(1L)
                .name("Updated Name")
                .email("test@example.com")
                .build();

        when(userService.updateProfile(any())).thenReturn(null);
        when(userMapper.toResponse(any())).thenReturn(response);

        mockMvc.perform(put("/user/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    @Test
    void updateProfile_shouldReturn400WhenNameBlank() throws Exception {
        String request = """
                {"name": ""}
                """;

        mockMvc.perform(put("/user/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProfile_shouldReturn400WhenNameMissing() throws Exception {
        String request = """
                {}
                """;

        mockMvc.perform(put("/user/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }
}
