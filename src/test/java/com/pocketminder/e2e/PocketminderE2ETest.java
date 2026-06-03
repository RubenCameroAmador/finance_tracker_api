package com.pocketminder.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pocketminder.auth.dto.AuthResponseDTO;
import com.pocketminder.auth.dto.LoginRequestDTO;
import com.pocketminder.auth.dto.RegisterRequestDTO;
import com.pocketminder.transaction.dto.CreateTransactionDTO;
import com.pocketminder.transaction.entity.TransactionCategory;
import com.pocketminder.transaction.entity.TransactionType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PocketminderE2ETest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static String jwtToken;
    private static final String TEST_EMAIL = "e2e@test.com";
    private static final String TEST_PASSWORD = "e2e-password";
    private static final String TEST_NAME = "E2E User";

    @Test
    @Order(1)
    void step1_registerUser() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setName(TEST_NAME);
        request.setEmail(TEST_EMAIL);
        request.setPassword(TEST_PASSWORD);

        int status = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn().getResponse().getStatus();

        // Accept 200 (new user) or 409 (already exists from previous run)
        assertTrue(status == 200 || status == 409);
    }

    @Test
    @Order(2)
    void step2_loginAndGetToken() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail(TEST_EMAIL);
        request.setPassword(TEST_PASSWORD);

        MvcResult result = mockMvc.perform(get("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        AuthResponseDTO authResponse = objectMapper.readValue(responseBody, AuthResponseDTO.class);
        jwtToken = authResponse.getToken();
    }

    @Test
    @Order(3)
    void step3_getUserProfile() throws Exception {
        mockMvc.perform(get("/user/me")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TEST_EMAIL));
    }

    @Test
    @Order(4)
    void step4_createTransaction() throws Exception {
        CreateTransactionDTO request = new CreateTransactionDTO();
        request.setTitle("E2E Grocery");
        request.setDescription("Test transaction");
        request.setAmount(new BigDecimal("50000"));
        request.setType(TransactionType.EXPENSE);
        request.setCategory(TransactionCategory.FOOD);

        mockMvc.perform(post("/transactions")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("E2E Grocery"))
                .andExpect(jsonPath("$.amount").value(50000))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.category").value("FOOD"));
    }

    @Test
    @Order(5)
    void step5_getTransactions() throws Exception {
        mockMvc.perform(get("/transactions")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("E2E Grocery"));
    }

    @Test
    @Order(6)
    void step6_getFinancialSummary() throws Exception {
        mockMvc.perform(get("/analytics/summary")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalExpenses").isNumber())
                .andExpect(jsonPath("$.totalIncome").isNumber())
                .andExpect(jsonPath("$.balance").isNumber());
    }

    @Test
    @Order(7)
    void step7_getExpenseCategories() throws Exception {
        mockMvc.perform(get("/analytics/expenses/categories")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("FOOD"))
                .andExpect(jsonPath("$[0].total").isNumber());
    }

    @Test
    @Order(8)
    void step8_updateProfile() throws Exception {
        String request = """
                {"name": "E2E Updated"}
                """;

        mockMvc.perform(put("/user/me")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("E2E Updated"))
                .andExpect(jsonPath("$.email").value(TEST_EMAIL));
    }

    @Test
    @Order(9)
    void step9_registerDuplicateEmailShouldFail() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setName("Duplicate");
        request.setEmail(TEST_EMAIL);
        request.setPassword("password");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(10)
    void step10_unauthorizedAccessShouldFail() throws Exception {
        mockMvc.perform(get("/transactions"))
                .andExpect(status().isUnauthorized());
    }
}
