package com.pocketminder.transaction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pocketminder.auth.security.JwtService;
import com.pocketminder.auth.service.CustomUserDetailsService;
import com.pocketminder.transaction.dto.CreateTransactionDTO;
import com.pocketminder.transaction.dto.TransactionResponseDTO;
import com.pocketminder.transaction.entity.Transaction;
import com.pocketminder.transaction.entity.TransactionCategory;
import com.pocketminder.transaction.entity.TransactionType;
import com.pocketminder.transaction.mapper.TransactionMapper;
import com.pocketminder.transaction.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private TransactionMapper transactionMapper;

    @Test
    void createTransaction_shouldReturn200() throws Exception {
        CreateTransactionDTO request = new CreateTransactionDTO();
        request.setTitle("Grocery");
        request.setDescription("Weekly groceries");
        request.setAmount(new BigDecimal("50000"));
        request.setType(TransactionType.EXPENSE);
        request.setCategory(TransactionCategory.FOOD);

        TransactionResponseDTO response = TransactionResponseDTO.builder()
                .id(1L)
                .title("Grocery")
                .amount(new BigDecimal("50000"))
                .type(TransactionType.EXPENSE)
                .category(TransactionCategory.FOOD)
                .build();

        when(transactionService.createTransaction(any())).thenReturn(null);
        when(transactionMapper.toResponse(any()))
                .thenReturn(response);

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Grocery"))
                .andExpect(jsonPath("$.amount").value(50000))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.category").value("FOOD"));
    }

    @Test
    void createTransaction_shouldReturn400WhenTitleMissing() throws Exception {
        String request = """
                {"amount": 50000, "type": "EXPENSE", "category": "FOOD"}
                """;

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTransaction_shouldReturn400WhenAmountNull() throws Exception {
        String request = """
                {"title": "Test", "type": "EXPENSE", "category": "FOOD"}
                """;

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTransaction_shouldReturn400WhenTypeNull() throws Exception {
        String request = """
                {"title": "Test", "amount": 50000, "category": "FOOD"}
                """;

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTransaction_shouldReturn400WhenCategoryNull() throws Exception {
        String request = """
                {"title": "Test", "amount": 50000, "type": "EXPENSE"}
                """;

        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMyTransactions_shouldReturn200WithList() throws Exception {
        TransactionResponseDTO txn1 = TransactionResponseDTO.builder()
                .id(1L).title("Txn1").amount(new BigDecimal("100")).build();
        TransactionResponseDTO txn2 = TransactionResponseDTO.builder()
                .id(2L).title("Txn2").amount(new BigDecimal("200")).build();

        when(transactionService.getMyTransactions()).thenReturn(List.of(new Transaction(), new Transaction()));
        when(transactionMapper.toResponse(any())).thenReturn(txn1, txn2);

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Txn1"))
                .andExpect(jsonPath("$[1].title").value("Txn2"));
    }

    @Test
    void getMyTransactions_shouldReturnEmptyList() throws Exception {
        when(transactionService.getMyTransactions()).thenReturn(List.of());

        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
