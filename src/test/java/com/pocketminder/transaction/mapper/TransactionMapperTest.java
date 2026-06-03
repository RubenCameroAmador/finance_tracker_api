package com.pocketminder.transaction.mapper;

import com.pocketminder.auth.entity.User;
import com.pocketminder.transaction.dto.TransactionResponseDTO;
import com.pocketminder.transaction.entity.Transaction;
import com.pocketminder.transaction.entity.TransactionCategory;
import com.pocketminder.transaction.entity.TransactionSource;
import com.pocketminder.transaction.entity.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TransactionMapperTest {

    private final TransactionMapper mapper = new TransactionMapper();

    @Test
    void toResponse_shouldMapAllFields() {
        LocalDateTime now = LocalDateTime.now();
        Transaction transaction = Transaction.builder()
                .id(1L)
                .title("MCDONALD'S")
                .description("Pago en MCDONALD'S")
                .amount(new BigDecimal("25000"))
                .type(TransactionType.EXPENSE)
                .category(TransactionCategory.FOOD)
                .source(TransactionSource.SMS)
                .autoDetected(true)
                .transactionDate(now)
                .build();

        TransactionResponseDTO dto = mapper.toResponse(transaction);

        assertEquals(1L, dto.getId());
        assertEquals("MCDONALD'S", dto.getTitle());
        assertEquals("Pago en MCDONALD'S", dto.getDescription());
        assertEquals(new BigDecimal("25000"), dto.getAmount());
        assertEquals(TransactionType.EXPENSE, dto.getType());
        assertEquals(TransactionCategory.FOOD, dto.getCategory());
        assertEquals(TransactionSource.SMS, dto.getSource());
        assertTrue(dto.getAutoDetected());
        assertEquals(now, dto.getTransactionDate());
    }

    @Test
    void toResponse_shouldHandleNullAutoDetected() {
        Transaction transaction = Transaction.builder()
                .id(2L)
                .title("Test")
                .amount(new BigDecimal("100"))
                .type(TransactionType.INCOME)
                .category(TransactionCategory.OTHER)
                .source(TransactionSource.MANUAL)
                .build();

        TransactionResponseDTO dto = mapper.toResponse(transaction);

        assertNull(dto.getAutoDetected());
    }

    @Test
    void toResponse_shouldHandleNullTransactionDate() {
        Transaction transaction = Transaction.builder()
                .id(3L)
                .title("Test")
                .amount(new BigDecimal("100"))
                .type(TransactionType.EXPENSE)
                .category(TransactionCategory.BILLS)
                .source(TransactionSource.MANUAL)
                .build();

        TransactionResponseDTO dto = mapper.toResponse(transaction);

        assertNull(dto.getTransactionDate());
    }

    @Test
    void toResponse_shouldNotMapUserEntity() {
        Transaction transaction = Transaction.builder()
                .id(4L)
                .title("Test")
                .amount(new BigDecimal("50"))
                .type(TransactionType.EXPENSE)
                .category(TransactionCategory.OTHER)
                .source(TransactionSource.MANUAL)
                .user(new User())
                .build();

        TransactionResponseDTO dto = mapper.toResponse(transaction);

        assertNull(dto.getAutoDetected());
    }
}
