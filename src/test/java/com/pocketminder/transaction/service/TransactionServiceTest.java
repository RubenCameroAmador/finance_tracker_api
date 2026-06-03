package com.pocketminder.transaction.service;

import com.pocketminder.auth.entity.User;
import com.pocketminder.transaction.dto.InternalTransactionRequest;
import com.pocketminder.transaction.entity.*;
import com.pocketminder.transaction.repository.TransactionRepository;
import com.pocketminder.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserService userService;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(transactionRepository, userService);
    }

    @Test
    void createTransaction_shouldSetCreatedAtAndUser() {
        User currentUser = User.builder().id(1L).email("user@example.com").build();
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InternalTransactionRequest request = InternalTransactionRequest.builder()
                .title("Test Transaction")
                .description("Test Description")
                .amount(new BigDecimal("10000"))
                .type(TransactionType.EXPENSE)
                .category(TransactionCategory.FOOD)
                .source(TransactionSource.MANUAL)
                .transactionDate(LocalDateTime.of(2026, 6, 1, 12, 0))
                .build();

        Transaction result = transactionService.createTransaction(request);

        assertEquals(currentUser, result.getUser());
        assertEquals("Test Transaction", result.getTitle());
        assertEquals(new BigDecimal("10000"), result.getAmount());
        assertEquals(TransactionType.EXPENSE, result.getType());
        assertEquals(TransactionCategory.FOOD, result.getCategory());
        assertEquals(TransactionSource.MANUAL, result.getSource());
        assertNotNull(result.getCreatedAt());
        verify(transactionRepository).save(result);
    }

    @Test
    void createTransaction_shouldHandleSmsAutoDetectedFields() {
        User currentUser = User.builder().id(1L).email("user@example.com").build();
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InternalTransactionRequest request = InternalTransactionRequest.builder()
                .title("MCDONALD'S")
                .description("SMS: Pagaste $25000 a MCDONALD'S")
                .amount(new BigDecimal("25000"))
                .type(TransactionType.EXPENSE)
                .category(TransactionCategory.OTHER)
                .source(TransactionSource.SMS)
                .rawMessage("Pagaste $25000 a MCDONALD'S desde tu cuenta")
                .detectedMerchant("MCDONALD'S")
                .autoDetected(true)
                .transactionDate(LocalDateTime.now())
                .build();

        Transaction result = transactionService.createTransaction(request);

        assertTrue(result.getAutoDetected());
        assertEquals("MCDONALD'S", result.getDetectedMerchant());
        assertNotNull(result.getRawMessage());
        assertEquals(TransactionSource.SMS, result.getSource());
    }

    @Test
    void getMyTransactions_shouldReturnUserTransactions() {
        User currentUser = User.builder().id(1L).email("user@example.com").build();
        when(userService.getCurrentUser()).thenReturn(currentUser);

        List<Transaction> expectedTransactions = List.of(
                Transaction.builder().id(1L).title("Txn1").build(),
                Transaction.builder().id(2L).title("Txn2").build()
        );
        when(transactionRepository.findByUser(currentUser)).thenReturn(expectedTransactions);

        List<Transaction> result = transactionService.getMyTransactions();

        assertEquals(2, result.size());
        assertEquals("Txn1", result.get(0).getTitle());
        assertEquals("Txn2", result.get(1).getTitle());
    }

    @Test
    void getMyTransactions_shouldReturnEmptyListWhenNoTransactions() {
        User currentUser = User.builder().id(1L).email("user@example.com").build();
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(transactionRepository.findByUser(currentUser)).thenReturn(List.of());

        List<Transaction> result = transactionService.getMyTransactions();

        assertTrue(result.isEmpty());
    }
}
