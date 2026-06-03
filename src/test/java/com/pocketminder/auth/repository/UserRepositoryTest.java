package com.pocketminder.auth.repository;

import com.pocketminder.auth.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void save_shouldPersistUser() {
        User user = User.builder()
                .name("Test User")
                .email("test@example.com")
                .password("hashed-password")
                .build();

        User saved = userRepository.save(user);

        assertNotNull(saved.getId());
        assertEquals("Test User", saved.getName());
        assertEquals("test@example.com", saved.getEmail());
        assertEquals("hashed-password", saved.getPassword());
    }

    @Test
    void findByEmail_shouldReturnUserWhenExists() {
        User user = User.builder()
                .name("Find Me")
                .email("find@example.com")
                .password("hash")
                .build();
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("find@example.com");

        assertTrue(found.isPresent());
        assertEquals("Find Me", found.get().getName());
    }

    @Test
    void findByEmail_shouldReturnEmptyWhenNotExists() {
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        assertFalse(found.isPresent());
    }

    @Test
    void findByEmail_shouldBeCaseSensitive() {
        User user = User.builder()
                .name("Case Test")
                .email("case@example.com")
                .password("hash")
                .build();
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("CASE@example.com");

        assertFalse(found.isPresent());
    }

    @Test
    void email_shouldBeUnique() {
        User user1 = User.builder()
                .name("User One")
                .email("unique@example.com")
                .password("hash1")
                .build();
        userRepository.save(user1);

        User user2 = User.builder()
                .name("User Two")
                .email("unique@example.com")
                .password("hash2")
                .build();

        assertThrows(Exception.class, () -> userRepository.save(user2));
    }

    @Test
    void save_shouldGenerateIdOnPersist() {
        User user = User.builder()
                .name("ID Test")
                .email("idtest@example.com")
                .password("hash")
                .build();

        User saved = userRepository.save(user);
        assertNotNull(saved.getId());

        User savedAgain = userRepository.save(user);
        assertEquals(saved.getId(), savedAgain.getId());
    }
}
