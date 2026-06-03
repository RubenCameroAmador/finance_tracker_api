package com.pocketminder.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pocketminder.auth.dto.AuthResponseDTO;
import com.pocketminder.auth.dto.LoginRequestDTO;
import com.pocketminder.auth.dto.RegisterRequestDTO;
import com.pocketminder.auth.entity.User;
import com.pocketminder.auth.security.JwtService;
import com.pocketminder.auth.service.AuthService;
import com.pocketminder.auth.service.CustomUserDetailsService;
import com.pocketminder.common.exception.EmailAlreadyExistsException;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void register_shouldReturn200WhenSuccessful() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setName("New User");
        request.setEmail("new@example.com");
        request.setPassword("password123");

        User savedUser = User.builder()
                .id(1L)
                .name("New User")
                .email("new@example.com")
                .password("encoded")
                .build();

        when(authService.register(any(RegisterRequestDTO.class))).thenReturn(savedUser);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("New User"))
                .andExpect(jsonPath("$.email").value("new@example.com"));
    }

    @Test
    void register_shouldReturn400WhenEmailMissing() throws Exception {
        String request = """
                {"name": "Test", "password": "pass123"}
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shouldReturn400WhenPasswordMissing() throws Exception {
        String request = """
                {"name": "Test", "email": "test@test.com"}
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shouldReturn400WhenNameMissing() throws Exception {
        String request = """
                {"email": "test@test.com", "password": "pass123"}
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shouldReturn400WhenEmailInvalid() throws Exception {
        String request = """
                {"name": "Test", "email": "not-an-email", "password": "pass123"}
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shouldReturn409WhenEmailAlreadyExists() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setName("Existing");
        request.setEmail("existing@example.com");
        request.setPassword("password");

        when(authService.register(any(RegisterRequestDTO.class)))
                .thenThrow(new EmailAlreadyExistsException("Email already exists"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already exists"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void login_shouldReturn200WithToken() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("user@example.com");
        request.setPassword("password");

        when(authService.login(any(LoginRequestDTO.class)))
                .thenReturn(AuthResponseDTO.builder().token("jwt-token").build());

        mockMvc.perform(get("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void me_shouldReturnAuthenticatedMessage() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isOk())
                .andExpect(content().string("Authenticated!"));
    }
}
