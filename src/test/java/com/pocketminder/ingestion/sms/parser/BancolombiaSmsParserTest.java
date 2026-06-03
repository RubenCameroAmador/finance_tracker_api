package com.pocketminder.ingestion.sms.parser;

import com.pocketminder.transaction.dto.InternalTransactionRequest;
import com.pocketminder.transaction.entity.TransactionCategory;
import com.pocketminder.transaction.entity.TransactionSource;
import com.pocketminder.transaction.entity.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class BancolombiaSmsParserTest {

    private final BancolombiaSmsParser parser = new BancolombiaSmsParser();

    @Test
    void supports_shouldReturnTrueForBancolombiaMessage() {
        assertTrue(parser.supports("Recibiste $50,000 de Bancolombia"));
    }

    @Test
    void supports_shouldReturnFalseForNonBancolombiaMessage() {
        assertFalse(parser.supports("Compra en Davivienda por $20,000"));
    }

    @Test
    void supports_shouldReturnFalseForNullMessage() {
        assertFalse(parser.supports(null));
    }

    @Test
    void parse_shouldExtractAmountWithThousandsSeparator() {
        InternalTransactionRequest result = parser.parse("Recibiste $50,000 de Bancolombia");
        assertEquals(new BigDecimal("50000"), result.getAmount());
    }

    @Test
    void parse_shouldExtractAmountWithoutThousandsSeparator() {
        InternalTransactionRequest result = parser.parse("Pagaste $15000 en Exito");
        assertEquals(new BigDecimal("15000"), result.getAmount());
    }

    @Test
    void parse_shouldExtractAmountWithMultipleThousandsSeparators() {
        InternalTransactionRequest result = parser.parse("Recibiste $1,200,000 de Nomina");
        assertEquals(new BigDecimal("1200000"), result.getAmount());
    }

    @Test
    void parse_shouldDefaultToZeroWhenNoAmountFound() {
        InternalTransactionRequest result = parser.parse("Bancolombia notifica: sin monto");
        assertEquals(BigDecimal.ZERO, result.getAmount());
    }

    @Test
    void parse_shouldDetectIncomeWhenContainsRecibiste() {
        InternalTransactionRequest result = parser.parse("Recibiste $50,000 de Bancolombia");
        assertEquals(TransactionType.INCOME, result.getType());
    }

    @Test
    void parse_shouldDefaultToExpenseWhenNoRecibiste() {
        InternalTransactionRequest result = parser.parse("Pagaste $20,000 en MercadoLibre");
        assertEquals(TransactionType.EXPENSE, result.getType());
    }

    @Test
    void parse_shouldExtractMerchant() {
        InternalTransactionRequest result = parser.parse("Pagaste $20,000 a MCDONALD'S desde tu cuenta");
        assertEquals("MCDONALD'S", result.getDetectedMerchant());
    }

    @Test
    void parse_shouldDefaultMerchantToUnknownWhenNoMatch() {
        InternalTransactionRequest result = parser.parse("Recibiste $50,000 de Bancolombia");
        assertEquals("UNKNOWN", result.getDetectedMerchant());
    }

    @Test
    void parse_shouldSetTitleAsMerchant() {
        InternalTransactionRequest result = parser.parse("Pagaste $20,000 a NETFLIX desde tu cuenta");
        assertEquals("NETFLIX", result.getTitle());
    }

    @Test
    void parse_shouldSetDescriptionAsFullMessage() {
        String message = "Pagaste $20,000 a NETFLIX desde tu cuenta";
        InternalTransactionRequest result = parser.parse(message);
        assertEquals(message, result.getDescription());
    }

    @Test
    void parse_shouldAlwaysSetCategoryOther() {
        InternalTransactionRequest result = parser.parse("Recibiste $50,000 de Bancolombia");
        assertEquals(TransactionCategory.OTHER, result.getCategory());
    }

    @Test
    void parse_shouldAlwaysSetSourceSms() {
        InternalTransactionRequest result = parser.parse("Recibiste $50,000 de Bancolombia");
        assertEquals(TransactionSource.SMS, result.getSource());
    }

    @Test
    void parse_shouldAlwaysSetAutoDetectedTrue() {
        InternalTransactionRequest result = parser.parse("Recibiste $50,000 de Bancolombia");
        assertTrue(result.getAutoDetected());
    }

    @Test
    void parse_shouldSetRawMessageAsFullMessage() {
        String message = "Recibiste $50,000 de Bancolombia";
        InternalTransactionRequest result = parser.parse(message);
        assertEquals(message, result.getRawMessage());
    }

    @Test
    void parse_shouldHandleDebitMessageCorrectly() {
        InternalTransactionRequest result = parser.parse("Compra en EXITO por $85,500");
        assertEquals(TransactionType.EXPENSE, result.getType());
        assertEquals(new BigDecimal("85500"), result.getAmount());
        assertEquals("UNKNOWN", result.getDetectedMerchant());
    }

    @Test
    void parse_shouldHandleTransferIncomeMessage() {
        InternalTransactionRequest result = parser.parse("Recibiste $200,000 a NEQUI desde Bancolombia");
        assertEquals(TransactionType.INCOME, result.getType());
        assertEquals(new BigDecimal("200000"), result.getAmount());
        assertEquals("NEQUI", result.getDetectedMerchant());
    }
}
