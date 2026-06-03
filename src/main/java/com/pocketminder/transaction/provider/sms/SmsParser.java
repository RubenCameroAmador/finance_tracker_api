package com.pocketminder.transaction.provider.sms;

import com.pocketminder.transaction.dto.InternalTransactionRequest;

public interface SmsParser {

    boolean supports(String message);

    InternalTransactionRequest parse(
            String message
    );
}