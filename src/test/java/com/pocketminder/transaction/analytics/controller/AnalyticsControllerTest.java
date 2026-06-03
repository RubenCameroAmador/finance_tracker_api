package com.pocketminder.transaction.analytics.controller;

import com.pocketminder.auth.security.JwtService;
import com.pocketminder.auth.service.CustomUserDetailsService;
import com.pocketminder.transaction.analytics.dto.CategorySummaryDTO;
import com.pocketminder.transaction.analytics.dto.FinancialSummaryDTO;
import com.pocketminder.transaction.analytics.service.AnalyticsService;
import com.pocketminder.transaction.entity.TransactionCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnalyticsController.class)
@AutoConfigureMockMvc(addFilters = false)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalyticsService analyticsService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getSummary_shouldReturnFinancialSummary() throws Exception {
        FinancialSummaryDTO summary = FinancialSummaryDTO.builder()
                .totalIncome(new BigDecimal("1000000"))
                .totalExpenses(new BigDecimal("600000"))
                .balance(new BigDecimal("400000"))
                .build();

        when(analyticsService.getFinancialSummary()).thenReturn(summary);

        mockMvc.perform(get("/analytics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(1000000))
                .andExpect(jsonPath("$.totalExpenses").value(600000))
                .andExpect(jsonPath("$.balance").value(400000));
    }

    @Test
    void getSummary_shouldReturnZeroBalanceWhenNoTransactions() throws Exception {
        FinancialSummaryDTO summary = FinancialSummaryDTO.builder()
                .totalIncome(BigDecimal.ZERO)
                .totalExpenses(BigDecimal.ZERO)
                .balance(BigDecimal.ZERO)
                .build();

        when(analyticsService.getFinancialSummary()).thenReturn(summary);

        mockMvc.perform(get("/analytics/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(0))
                .andExpect(jsonPath("$.totalExpenses").value(0))
                .andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    void getExpenseCategories_shouldReturnCategoryList() throws Exception {
        List<CategorySummaryDTO> categories = List.of(
                new CategorySummaryDTO(TransactionCategory.FOOD, new BigDecimal("300000")),
                new CategorySummaryDTO(TransactionCategory.TRANSPORT, new BigDecimal("150000"))
        );

        when(analyticsService.getExpenseCategoriesSummary()).thenReturn(categories);

        mockMvc.perform(get("/analytics/expenses/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("FOOD"))
                .andExpect(jsonPath("$[0].total").value(300000))
                .andExpect(jsonPath("$[1].category").value("TRANSPORT"))
                .andExpect(jsonPath("$[1].total").value(150000));
    }

    @Test
    void getExpenseCategories_shouldReturnEmptyList() throws Exception {
        when(analyticsService.getExpenseCategoriesSummary()).thenReturn(List.of());

        mockMvc.perform(get("/analytics/expenses/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
