package com.pocketminder.transaction.dto;

import com.pocketminder.transaction.entity.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionResponseDTO {

    private Long id;

    private String title;

    private String description;

    private BigDecimal amount;

    private TransactionType type;

    private TransactionCategory category;

    private TransactionSource source;

    private Boolean autoDetected;

    private LocalDateTime transactionDate;
}