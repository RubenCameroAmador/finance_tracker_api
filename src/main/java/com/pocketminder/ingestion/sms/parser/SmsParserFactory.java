package com.pocketminder.ingestion.sms.parser;

import com.pocketminder.common.exception.UnsupportedBankException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SmsParserFactory {

    private final List<SmsParser> parsers;

    public SmsParser getParser(
            String message
    ) {

        return parsers.stream()
                .filter(parser ->
                        parser.supports(message)
                )
                .findFirst()
                .orElseThrow(() ->
                        new UnsupportedBankException(
                                "Unsupported bank"
                        )
                );
    }
}