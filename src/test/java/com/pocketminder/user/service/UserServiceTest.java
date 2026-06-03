package com.pocketminder.user.service;

import com.pocketminder.auth.entity.User;
import com.pocketminder.auth.repository.UserRepository;
import com.pocketminder.user.dto.UpdateUserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void getCurrentUser_shouldReturnUserWhenAuthenticated() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("user@example.com");

        User expectedUser = User.builder().id(1L).name("Test").email("user@example.com").build();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(expectedUser));

        User result = userService.getCurrentUser();

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("user@example.com", result.getEmail());
    }

    @Test
    void getCurrentUser_shouldThrowWhenAuthenticationIsNull() {
        when(securityContext.getAuthentication()).thenReturn(null);

        assertThrows(NullPointerException.class,
                () -> userService.getCurrentUser());
    }

    @Test
    void getCurrentUser_shouldThrowWhenEmailNotFound() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("unknown@example.com");
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> userService.getCurrentUser());
    }

    @Test
    void updateProfile_shouldUpdateName() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("user@example.com");

        User existingUser = User.builder().id(1L).name("Old Name").email("user@example.com").build();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserDTO request = new UpdateUserDTO();
        request.setName("New Name");

        User result = userService.updateProfile(request);

        assertEquals("New Name", result.getName());
        verify(userRepository).save(existingUser);
    }
}
