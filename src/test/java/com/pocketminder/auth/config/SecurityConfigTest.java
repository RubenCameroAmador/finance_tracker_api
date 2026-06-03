package com.pocketminder.auth.config;

import com.pocketminder.auth.security.JwtAuthenticationFilter;
import com.pocketminder.auth.security.JwtService;
import com.pocketminder.auth.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {SecurityConfigTest.TestConfig.class, SecurityConfig.class, SecurityConfigTest.TestController.class})
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @TestConfiguration
    @Import(SecurityConfig.class)
    static class TestConfig {
        @SuppressWarnings("unused")
        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
            return new JwtAuthenticationFilter(jwtService, userDetailsService);
        }
    }

    @Test
    void register_shouldBePermittedWithoutAuth() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content("{\"name\":\"Test\",\"email\":\"test@test.com\",\"password\":\"pass\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void login_shouldBePermittedWithoutAuth() throws Exception {
        mockMvc.perform(get("/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"test@test.com\",\"password\":\"pass\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpoint_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/transactions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userEndpoint_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/user/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void analyticsEndpoint_shouldRequireAuth() throws Exception {
        mockMvc.perform(get("/analytics/summary"))
                .andExpect(status().isUnauthorized());
    }

    @RestController
    static class TestController {
        @PostMapping("/auth/register")
        String register() { return "ok"; }

        @GetMapping("/auth/login")
        String login() { return "ok"; }

        @GetMapping("/transactions")
        String transactions() { return "ok"; }

        @GetMapping("/user/me")
        String userMe() { return "ok"; }

        @GetMapping("/analytics/summary")
        String analytics() { return "ok"; }
    }
}
