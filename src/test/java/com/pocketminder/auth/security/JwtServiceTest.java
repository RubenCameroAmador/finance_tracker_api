package com.pocketminder.auth.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", "TestSecretKeyThatIsAtLeast32CharactersLong!!");
        jwtService.init();
    }

    @Test
    void generateToken_shouldReturnNonNullToken() {
        String token = jwtService.generateToken("test@example.com");
        assertNotNull(token);
    }

    @Test
    void generateToken_shouldReturnValidJwtFormat() {
        String token = jwtService.generateToken("test@example.com");
        assertTrue(token.split("\\.").length == 3);
    }

    @Test
    void extractEmail_shouldReturnEmailUsedToGenerate() {
        String email = "user@example.com";
        String token = jwtService.generateToken(email);
        assertEquals(email, jwtService.extractEmail(token));
    }

    @Test
    void extractEmail_shouldReturnEmailForDifferentEmail() {
        String email = "another@example.com";
        String token = jwtService.generateToken(email);
        assertEquals(email, jwtService.extractEmail(token));
    }

    @Test
    void extractEmail_shouldThrowForMalformedToken() {
        assertThrows(MalformedJwtException.class,
                () -> jwtService.extractEmail("not-a-jwt-token"));
    }

    @Test
    void extractEmail_shouldThrowForTamperedToken() {
        String token = jwtService.generateToken("test@example.com");
        String tampered = token.substring(0, token.lastIndexOf('.')) + ".tampered";
        assertThrows(Exception.class,
                () -> jwtService.extractEmail(tampered));
    }

    @Test
    void extractEmail_shouldThrowForExpiredToken() {
        String token = io.jsonwebtoken.Jwts.builder()
                .subject("test@example.com")
                .issuedAt(new java.util.Date(System.currentTimeMillis() - 2000))
                .expiration(new java.util.Date(System.currentTimeMillis() - 1000))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        "TestSecretKeyThatIsAtLeast32CharactersLong!!".getBytes(java.nio.charset.StandardCharsets.UTF_8)
                ))
                .compact();

        assertThrows(ExpiredJwtException.class,
                () -> jwtService.extractEmail(token));
    }

    @Test
    void extractEmail_shouldThrowForTokenWithWrongSignature() {
        JwtService otherService = new JwtService();
        ReflectionTestUtils.setField(otherService, "secret", "DifferentSecretKeyThatIsAtLeast32Characters!!");
        otherService.init();

        String token = otherService.generateToken("test@example.com");

        assertThrows(SignatureException.class,
                () -> jwtService.extractEmail(token));
    }

    @Test
    void generateToken_shouldProduceUniqueTokensForSameEmail() throws Exception {
        String token1 = jwtService.generateToken("same@example.com");
        Thread.sleep(1100);
        String token2 = jwtService.generateToken("same@example.com");
        assertNotEquals(token1, token2);
    }
}
