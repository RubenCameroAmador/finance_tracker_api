package com.pocketminder.ingestion.sms.parser;

import com.pocketminder.transaction.dto.InternalTransactionRequest;
import com.pocketminder.transaction.entity.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.regex.*;

@Component
public class BancolombiaSmsParser
        implements SmsParser {

    @Override
    public boolean supports(String message) {

        return message.contains("Bancolombia");
    }

    @Override
    public InternalTransactionRequest parse(
            String message
    ) {

        Pattern amountPattern =
                Pattern.compile("\\$([\\d,.]+)");

        Matcher amountMatcher =
                amountPattern.matcher(message);

        BigDecimal amount = BigDecimal.ZERO;

        if (amountMatcher.find()) {

            String amountString =
                    amountMatcher.group(1)
                            .replace(",", "");

            amount = new BigDecimal(amountString);
        }

        TransactionType type;

        if (message.contains("recibiste")) {

            type = TransactionType.INCOME;

        } else {

            type = TransactionType.EXPENSE;
        }

        String merchant = "UNKNOWN";

        Pattern merchantPattern =
                Pattern.compile(
                        "a ([A-Z0-9\\s]+?) desde"
                );

        Matcher merchantMatcher =
                merchantPattern.matcher(message);

        if (merchantMatcher.find()) {

            merchant = merchantMatcher.group(1).trim();
        }

        return InternalTransactionRequest
                .builder()
                .title(merchant)
                .description(message)
                .amount(amount)
                .type(type)
                .category(TransactionCategory.OTHER)
                .source(TransactionSource.SMS)
                .rawMessage(message)
                .detectedMerchant(merchant)
                .autoDetected(true)
                .build();
    }
}