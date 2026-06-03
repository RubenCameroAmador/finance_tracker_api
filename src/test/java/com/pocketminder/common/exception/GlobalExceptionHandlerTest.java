package com.pocketminder.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SomeService someService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController(someService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void handleEmailAlreadyExists_shouldReturn409() throws Exception {
        when(someService.triggerEmailAlreadyExists())
                .thenThrow(new EmailAlreadyExistsException("Email already registered"));

        mockMvc.perform(get("/test/email-exists"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already registered"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void handleUnsupportedBank_shouldReturn400() throws Exception {
        when(someService.triggerUnsupportedBank())
                .thenThrow(new UnsupportedBankException("Bank not supported"));

        mockMvc.perform(get("/test/unsupported-bank"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bank not supported"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void handleValidation_shouldReturn400() throws Exception {
        String request = """
                {"name": ""}
                """;

        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void handleGenericException_shouldReturn500() throws Exception {
        when(someService.triggerGeneric())
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(get("/test/generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Unexpected error"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void handleGenericException_shouldNotExposeInternalDetails() throws Exception {
        when(someService.triggerGeneric())
                .thenThrow(new RuntimeException("Internal: database connection failed"));

        mockMvc.perform(get("/test/generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Internal: database connection failed"));
    }

    interface SomeService {
        String triggerEmailAlreadyExists();
        String triggerUnsupportedBank();
        String triggerGeneric();
    }

    @RestController
    static class TestController {
        private final SomeService someService;

        TestController(SomeService someService) {
            this.someService = someService;
        }

        @GetMapping("/test/email-exists")
        String emailExists() {
            return someService.triggerEmailAlreadyExists();
        }

        @GetMapping("/test/unsupported-bank")
        String unsupportedBank() {
            return someService.triggerUnsupportedBank();
        }

        @GetMapping("/test/generic")
        String generic() {
            return someService.triggerGeneric();
        }

        @PostMapping("/test/validate")
        String validate(@Valid @RequestBody TestRequest request) {
            return "ok";
        }
    }

    @Data
    static class TestRequest {
        @NotBlank
        private String name;
    }
}
