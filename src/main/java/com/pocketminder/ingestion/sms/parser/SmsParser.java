package com.pocketminder.ingestion.sms.parser;

import com.pocketminder.transaction.dto.InternalTransactionRequest;

public interface SmsParser {

    boolean supports(String message);

    InternalTransactionRequest parse(
            String message
    );
}