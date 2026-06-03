package com.pocketminder.ingestion.sms.service;

import com.pocketminder.ingestion.sms.parser.SmsParser;
import com.pocketminder.ingestion.sms.parser.SmsParserFactory;
import com.pocketminder.transaction.dto.InternalTransactionRequest;
import com.pocketminder.transaction.entity.Transaction;
import com.pocketminder.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SmsIngestionService {

    private final SmsParserFactory parserFactory;
    private final TransactionService transactionService;

    public Transaction ingest(
            String message
    ) {

        SmsParser parser =
                parserFactory.getParser(message);

        InternalTransactionRequest request =
                parser.parse(message);

        return transactionService
                .createTransaction(request);
    }
}