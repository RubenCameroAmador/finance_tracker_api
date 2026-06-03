package com.pocketminder.auth.service;

import com.pocketminder.auth.dto.AuthResponseDTO;
import com.pocketminder.auth.dto.LoginRequestDTO;
import com.pocketminder.auth.dto.RegisterRequestDTO;
import com.pocketminder.auth.entity.User;
import com.pocketminder.auth.repository.UserRepository;
import com.pocketminder.auth.security.JwtService;
import com.pocketminder.common.exception.EmailAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService, authenticationManager);
    }

    @Test
    void register_shouldCreateUserWhenEmailDoesNotExist() {
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setName("New User");
        request.setEmail("new@example.com");
        request.setPassword("password123");

        User result = authService.register(request);

        assertNotNull(result);
        assertEquals("New User", result.getName());
        assertEquals("new@example.com", result.getEmail());
        assertEquals("encoded-password", result.getPassword());
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_shouldThrowWhenEmailAlreadyExists() {
        when(userRepository.findByEmail("existing@example.com"))
                .thenReturn(Optional.of(new User()));

        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setName("Existing");
        request.setEmail("existing@example.com");
        request.setPassword("password");

        assertThrows(EmailAlreadyExistsException.class,
                () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_shouldReturnTokenWhenCredentialsValid() {
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(jwtService.generateToken("user@example.com")).thenReturn("jwt-token");

        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("user@example.com");
        request.setPassword("password");

        AuthResponseDTO result = authService.login(request);

        assertNotNull(result);
        assertEquals("jwt-token", result.getToken());
        verify(jwtService).generateToken("user@example.com");
    }

    @Test
    void login_shouldCallAuthenticationManagerWithCorrectCredentials() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("user@example.com");
        request.setPassword("password");

        when(jwtService.generateToken(any())).thenReturn("token");

        authService.login(request);

        verify(authenticationManager).authenticate(argThat(auth ->
                auth.getPrincipal().equals("user@example.com")
                        && auth.getCredentials().equals("password")
        ));
    }

    @Test
    void register_shouldMapAllFieldsFromRequest() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setName("Full Name");
        request.setEmail("full@example.com");
        request.setPassword("securePass1");

        User result = authService.register(request);

        assertEquals("Full Name", result.getName());
        assertEquals("full@example.com", result.getEmail());
        assertEquals("encoded", result.getPassword());
    }
}
