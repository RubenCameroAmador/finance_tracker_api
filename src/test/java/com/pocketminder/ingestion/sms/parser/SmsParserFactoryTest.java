package com.pocketminder.ingestion.sms.parser;

import com.pocketminder.common.exception.UnsupportedBankException;
import com.pocketminder.transaction.dto.InternalTransactionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SmsParserFactoryTest {

    private SmsParserFactory factory;
    private BancolombiaSmsParser bancolombiaParser;

    @BeforeEach
    void setUp() {
        bancolombiaParser = new BancolombiaSmsParser();
        factory = new SmsParserFactory(List.of(bancolombiaParser));
    }

    @Test
    void getParser_shouldReturnParserWhenMessageMatches() {
        SmsParser result = factory.getParser("Recibiste $50,000 de Bancolombia");
        assertSame(bancolombiaParser, result);
    }

    @Test
    void getParser_shouldThrowWhenNoParserMatches() {
        assertThrows(UnsupportedBankException.class,
                () -> factory.getParser("Compra en Davivienda por $20,000"));
    }

    @Test
    void getParser_shouldThrowWhenMessageIsNull() {
        assertThrows(UnsupportedBankException.class,
                () -> factory.getParser(null));
    }

    @Test
    void getParser_shouldThrowWhenParsersListIsEmpty() {
        SmsParserFactory emptyFactory = new SmsParserFactory(List.of());
        assertThrows(UnsupportedBankException.class,
                () -> emptyFactory.getParser("Recibiste $50,000 de Bancolombia"));
    }

    @Test
    void getParser_shouldReturnFirstMatchingParserWhenMultipleRegistered() {
        SmsParser secondParser = new SmsParser() {
            @Override
            public boolean supports(String message) {
                return message.contains("Davivienda");
            }

            @Override
            public InternalTransactionRequest parse(String message) {
                return null;
            }
        };
        SmsParserFactory multiFactory = new SmsParserFactory(List.of(bancolombiaParser, secondParser));

        SmsParser result = multiFactory.getParser("Recibiste $50,000 de Bancolombia");
        assertSame(bancolombiaParser, result);
    }
}
