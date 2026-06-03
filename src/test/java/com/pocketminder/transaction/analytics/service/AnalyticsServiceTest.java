package com.pocketminder.transaction.analytics.service;

import com.pocketminder.auth.entity.User;
import com.pocketminder.transaction.analytics.dto.CategorySummaryDTO;
import com.pocketminder.transaction.analytics.dto.FinancialSummaryDTO;
import com.pocketminder.transaction.entity.TransactionCategory;
import com.pocketminder.transaction.entity.TransactionType;
import com.pocketminder.transaction.repository.TransactionRepository;
import com.pocketminder.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserService userService;

    private AnalyticsService analyticsService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(transactionRepository, userService);
        currentUser = User.builder().id(1L).email("user@example.com").build();
        when(userService.getCurrentUser()).thenReturn(currentUser);
    }

    @Test
    void getFinancialSummary_shouldReturnZeroWhenNoTransactions() {
        when(transactionRepository.getTotalByType(currentUser, TransactionType.INCOME))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.getTotalByType(currentUser, TransactionType.EXPENSE))
                .thenReturn(BigDecimal.ZERO);

        FinancialSummaryDTO result = analyticsService.getFinancialSummary();

        assertEquals(BigDecimal.ZERO, result.getTotalIncome());
        assertEquals(BigDecimal.ZERO, result.getTotalExpenses());
        assertEquals(BigDecimal.ZERO, result.getBalance());
    }

    @Test
    void getFinancialSummary_shouldCalculateCorrectBalance() {
        when(transactionRepository.getTotalByType(currentUser, TransactionType.INCOME))
                .thenReturn(new BigDecimal("500000"));
        when(transactionRepository.getTotalByType(currentUser, TransactionType.EXPENSE))
                .thenReturn(new BigDecimal("350000"));

        FinancialSummaryDTO result = analyticsService.getFinancialSummary();

        assertEquals(new BigDecimal("500000"), result.getTotalIncome());
        assertEquals(new BigDecimal("350000"), result.getTotalExpenses());
        assertEquals(new BigDecimal("150000"), result.getBalance());
    }

    @Test
    void getFinancialSummary_shouldHandleNegativeBalance() {
        when(transactionRepository.getTotalByType(currentUser, TransactionType.INCOME))
                .thenReturn(new BigDecimal("100000"));
        when(transactionRepository.getTotalByType(currentUser, TransactionType.EXPENSE))
                .thenReturn(new BigDecimal("250000"));

        FinancialSummaryDTO result = analyticsService.getFinancialSummary();

        assertEquals(new BigDecimal("100000"), result.getTotalIncome());
        assertEquals(new BigDecimal("250000"), result.getTotalExpenses());
        assertEquals(new BigDecimal("-150000"), result.getBalance());
    }

    @Test
    void getExpenseCategoriesSummary_shouldReturnEmptyListWhenNoExpenses() {
        when(transactionRepository.getExpensesByCategory(currentUser)).thenReturn(List.of());

        List<CategorySummaryDTO> result = analyticsService.getExpenseCategoriesSummary();

        assertTrue(result.isEmpty());
    }

    @Test
    void getExpenseCategoriesSummary_shouldReturnCategoryTotals() {
        List<CategorySummaryDTO> expectedSummaries = List.of(
                new CategorySummaryDTO(TransactionCategory.FOOD, new BigDecimal("200000")),
                new CategorySummaryDTO(TransactionCategory.TRANSPORT, new BigDecimal("100000"))
        );
        when(transactionRepository.getExpensesByCategory(currentUser)).thenReturn(expectedSummaries);

        List<CategorySummaryDTO> result = analyticsService.getExpenseCategoriesSummary();

        assertEquals(2, result.size());
        assertEquals(TransactionCategory.FOOD, result.get(0).getCategory());
        assertEquals(new BigDecimal("200000"), result.get(0).getTotal());
        assertEquals(TransactionCategory.TRANSPORT, result.get(1).getCategory());
        assertEquals(new BigDecimal("100000"), result.get(1).getTotal());
    }
}
