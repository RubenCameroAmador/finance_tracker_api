package com.pocketminder.transaction.provider.sms;

import com.pocketminder.transaction.dto.InternalTransactionRequest;
import com.pocketminder.transaction.entity.Transaction;
import com.pocketminder.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SmsTransactionService {

    private final SmsParserFactory parserFactory;
    private final TransactionService transactionService;

    public Transaction processSms(
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