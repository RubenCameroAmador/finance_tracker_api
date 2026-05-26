package com.pocketminder.transaction.mapper;

import com.pocketminder.transaction.dto.TransactionResponseDTO;
import com.pocketminder.transaction.entity.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionResponseDTO toResponse(
            Transaction transaction
    ) {

        return TransactionResponseDTO.builder()
                .id(transaction.getId())
                .title(transaction.getTitle())
                .description(transaction.getDescription())
                .amount(transaction.getAmount())
                .type(transaction.getType())
                .category(transaction.getCategory())
                .source(transaction.getSource())
                .autoDetected(transaction.getAutoDetected())
                .transactionDate(
                        transaction.getTransactionDate()
                )
                .build();
    }
}
