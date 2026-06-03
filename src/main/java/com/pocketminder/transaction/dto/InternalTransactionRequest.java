package com.pocketminder.transaction.dto;

import com.pocketminder.transaction.entity.TransactionCategory;
import com.pocketminder.transaction.entity.TransactionSource;
import com.pocketminder.transaction.entity.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class InternalTransactionRequest {

    private String title;

    private String description;

    private BigDecimal amount;

    private TransactionType type;

    private TransactionCategory category;

    private TransactionSource source;

    private String rawMessage;

    private String detectedMerchant;

    private Boolean autoDetected;

    private LocalDateTime transactionDate;
}