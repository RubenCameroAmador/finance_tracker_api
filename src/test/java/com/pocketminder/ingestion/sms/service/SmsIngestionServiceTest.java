package com.pocketminder.ingestion.sms.service;

import com.pocketminder.ingestion.sms.parser.SmsParser;
import com.pocketminder.ingestion.sms.parser.SmsParserFactory;
import com.pocketminder.transaction.dto.InternalTransactionRequest;
import com.pocketminder.transaction.entity.*;
import com.pocketminder.transaction.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmsIngestionServiceTest {

    @Mock
    private SmsParserFactory parserFactory;
    @Mock
    private TransactionService transactionService;
    @Mock
    private SmsParser mockParser;

    private SmsIngestionService ingestionService;

    @BeforeEach
    void setUp() {
        ingestionService = new SmsIngestionService(parserFactory, transactionService);
    }

    @Test
    void ingest_shouldParseAndCreateTransaction() {
        String message = "Pagaste $25000 a MCDONALD'S desde tu cuenta";
        InternalTransactionRequest parsedRequest = InternalTransactionRequest.builder()
                .title("MCDONALD'S")
                .description(message)
                .amount(new BigDecimal("25000"))
                .type(TransactionType.EXPENSE)
                .category(TransactionCategory.OTHER)
                .source(TransactionSource.SMS)
                .rawMessage(message)
                .detectedMerchant("MCDONALD'S")
                .autoDetected(true)
                .build();

        Transaction expectedTransaction = Transaction.builder()
                .id(1L)
                .title("MCDONALD'S")
                .amount(new BigDecimal("25000"))
                .build();

        when(parserFactory.getParser(message)).thenReturn(mockParser);
        when(mockParser.parse(message)).thenReturn(parsedRequest);
        when(transactionService.createTransaction(parsedRequest)).thenReturn(expectedTransaction);

        Transaction result = ingestionService.ingest(message);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("MCDONALD'S", result.getTitle());
        verify(parserFactory).getParser(message);
        verify(mockParser).parse(message);
        verify(transactionService).createTransaction(parsedRequest);
    }

    @Test
    void ingest_shouldThrowWhenParserNotFound() {
        String message = "Mensaje de banco desconocido";
        when(parserFactory.getParser(message))
                .thenThrow(new RuntimeException("Unsupported bank"));

        assertThrows(RuntimeException.class,
                () -> ingestionService.ingest(message));
        verifyNoInteractions(transactionService);
    }
}
