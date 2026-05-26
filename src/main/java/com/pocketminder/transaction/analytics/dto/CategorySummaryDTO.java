package com.pocketminder.transaction.analytics.dto;

import com.pocketminder.transaction.entity.TransactionCategory;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CategorySummaryDTO {

    private TransactionCategory category;

    private BigDecimal total;
}
