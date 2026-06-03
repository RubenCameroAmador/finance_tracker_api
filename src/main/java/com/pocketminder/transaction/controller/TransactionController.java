package com.pocketminder.transaction.controller;

import com.pocketminder.transaction.dto.*;
import com.pocketminder.transaction.entity.Transaction;
import com.pocketminder.transaction.entity.TransactionSource;
import com.pocketminder.transaction.mapper.TransactionMapper;
import com.pocketminder.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;

    @PostMapping
    public TransactionResponseDTO createTransaction(
            @Valid @RequestBody CreateTransactionDTO request
    ) {

        InternalTransactionRequest internalRequest =
                InternalTransactionRequest.builder()
                        .title(request.getTitle())
                        .description(request.getDescription())
                        .amount(request.getAmount())
                        .type(request.getType())
                        .category(request.getCategory())
                        .source(TransactionSource.MANUAL)
                        .autoDetected(false)
                        .transactionDate(LocalDateTime.now())
                        .build();

        Transaction transaction =
                transactionService.createTransaction(
                        internalRequest
                );

        return transactionMapper.toResponse(transaction);
    }

    @GetMapping
    public List<TransactionResponseDTO>
    getMyTransactions() {

        return transactionService
                .getMyTransactions()
                .stream()
                .map(transactionMapper::toResponse)
                .toList();
    }
}
