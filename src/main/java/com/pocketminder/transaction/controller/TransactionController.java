package com.pocketminder.transaction.controller;

import com.pocketminder.transaction.dto.*;
import com.pocketminder.transaction.entity.Transaction;
import com.pocketminder.transaction.mapper.TransactionMapper;
import com.pocketminder.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;

    @PostMapping
    public TransactionResponseDTO createTransaction(
            @Valid @RequestBody
            CreateTransactionDTO request
    ) {

        Transaction transaction =
                transactionService
                        .createManualTransaction(request);

        return transactionMapper
                .toResponse(transaction);
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
