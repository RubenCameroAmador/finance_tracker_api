package com.pocketminder.transaction.analytics.service;

import com.pocketminder.auth.entity.User;
import com.pocketminder.transaction.analytics.dto.CategorySummaryDTO;
import com.pocketminder.transaction.analytics.dto.FinancialSummaryDTO;
import com.pocketminder.transaction.entity.TransactionType;
import com.pocketminder.transaction.repository.TransactionRepository;
import com.pocketminder.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final TransactionRepository transactionRepository;
    private final UserService userService;

    public FinancialSummaryDTO getFinancialSummary() {

        User user = userService.getCurrentUser();

        BigDecimal income =
                transactionRepository.getTotalByType(
                        user,
                        TransactionType.INCOME
                );

        BigDecimal expenses =
                transactionRepository.getTotalByType(
                        user,
                        TransactionType.EXPENSE
                );

        BigDecimal balance =
                income.subtract(expenses);

        return FinancialSummaryDTO.builder()
                .totalIncome(income)
                .totalExpenses(expenses)
                .balance(balance)
                .build();
    }

    public List<CategorySummaryDTO>
    getExpenseCategoriesSummary() {

        User user = userService.getCurrentUser();

        return transactionRepository
                .getExpensesByCategory(user);
    }
}
