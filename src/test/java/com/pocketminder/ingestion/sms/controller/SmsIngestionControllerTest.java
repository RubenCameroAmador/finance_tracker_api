package com.pocketminder.ingestion.sms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pocketminder.auth.security.JwtService;
import com.pocketminder.auth.service.CustomUserDetailsService;
import com.pocketminder.ingestion.sms.dto.SmsMessageDTO;
import com.pocketminder.ingestion.sms.service.SmsIngestionService;
import com.pocketminder.transaction.dto.TransactionResponseDTO;
import com.pocketminder.transaction.entity.Transaction;
import com.pocketminder.transaction.entity.TransactionType;
import com.pocketminder.transaction.mapper.TransactionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SmsIngestionController.class)
@AutoConfigureMockMvc(addFilters = false)
class SmsIngestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private SmsIngestionService smsIngestionService;

    @MockitoBean
    private TransactionMapper transactionMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void ingestSms_shouldReturn200WithTransaction() throws Exception {
        SmsMessageDTO request = new SmsMessageDTO();
        request.setMessage("Pagaste $25000 a MCDONALD'S desde tu cuenta");

        TransactionResponseDTO response = TransactionResponseDTO.builder()
                .id(1L)
                .title("MCDONALD'S")
                .amount(new BigDecimal("25000"))
                .type(TransactionType.EXPENSE)
                .build();

        when(smsIngestionService.ingest(any(String.class))).thenReturn(new Transaction());
        when(transactionMapper.toResponse(any())).thenReturn(response);

        mockMvc.perform(post("/ingestion/sms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("MCDONALD'S"))
                .andExpect(jsonPath("$.amount").value(25000));
    }

    @Test
    void ingestSms_shouldReturn400WhenMessageBlank() throws Exception {
        String request = """
                {"message": ""}
                """;

        mockMvc.perform(post("/ingestion/sms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ingestSms_shouldReturn400WhenMessageMissing() throws Exception {
        String request = """
                {}
                """;

        mockMvc.perform(post("/ingestion/sms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }
}
