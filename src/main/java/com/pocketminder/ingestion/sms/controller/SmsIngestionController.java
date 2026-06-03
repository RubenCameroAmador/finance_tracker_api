package com.pocketminder.ingestion.sms.controller;

import com.pocketminder.ingestion.sms.dto.SmsMessageDTO;
import com.pocketminder.ingestion.sms.service.SmsIngestionService;
import com.pocketminder.transaction.dto.TransactionResponseDTO;
import com.pocketminder.transaction.entity.Transaction;
import com.pocketminder.transaction.mapper.TransactionMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ingestion/sms")
@RequiredArgsConstructor
public class SmsIngestionController {

    private final SmsIngestionService smsIngestionService;
    private final TransactionMapper transactionMapper;

    @PostMapping
    public TransactionResponseDTO ingestSms(
            @Valid @RequestBody SmsMessageDTO request
    ) {

        Transaction transaction =
                smsIngestionService.ingest(
                        request.getMessage()
                );

        return transactionMapper.toResponse(transaction);
    }
}