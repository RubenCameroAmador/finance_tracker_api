package com.pocketminder.auth.service;

import com.pocketminder.auth.entity.User;
import com.pocketminder.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    private CustomUserDetailsService service;

    @BeforeEach
    void setUp() {
        service = new CustomUserDetailsService(userRepository);
    }

    @Test
    void loadUserByUsername_shouldReturnUserWhenEmailExists() {
        User user = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .password("hashed-password")
                .build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        UserDetails result = service.loadUserByUsername("test@example.com");

        assertNotNull(result);
        assertEquals("test@example.com", result.getUsername());
    }

    @Test
    void loadUserByUsername_shouldThrowWhenEmailNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("unknown@example.com"));
    }

    @Test
    void loadUserByUsername_shouldReturnUserWithNoAuthorities() {
        User user = User.builder()
                .id(1L)
                .name("Test")
                .email("test@example.com")
                .password("hash")
                .build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        UserDetails result = service.loadUserByUsername("test@example.com");

        assertTrue(result.getAuthorities().isEmpty());
    }
}
