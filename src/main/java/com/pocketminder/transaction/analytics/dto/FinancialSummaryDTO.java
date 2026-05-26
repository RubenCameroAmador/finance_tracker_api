package com.pocketminder.transaction.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class FinancialSummaryDTO {

    private BigDecimal totalIncome;

    private BigDecimal totalExpenses;

    private BigDecimal balance;
}
