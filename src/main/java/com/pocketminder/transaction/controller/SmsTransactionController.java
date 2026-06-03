package com.pocketminder.transaction.controller;

import com.pocketminder.transaction.entity.Transaction;
import com.pocketminder.transaction.mapper.TransactionMapper;
import com.pocketminder.transaction.dto.TransactionResponseDTO;
import com.pocketminder.transaction.provider.sms.SmsTransactionService;
import com.pocketminder.transaction.provider.sms.dto.SmsMessageDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions/sms")
@RequiredArgsConstructor
public class SmsTransactionController {

    private final SmsTransactionService smsTransactionService;
    private final TransactionMapper transactionMapper;

    @PostMapping
    public TransactionResponseDTO processSms(
            @Valid @RequestBody SmsMessageDTO request
    ) {

        Transaction transaction =
                smsTransactionService.processSms(
                        request.getMessage()
                );

        return transactionMapper.toResponse(transaction);
    }
}