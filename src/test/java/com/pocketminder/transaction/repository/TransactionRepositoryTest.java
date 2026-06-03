package com.pocketminder.transaction.repository;

import com.pocketminder.auth.entity.User;
import com.pocketminder.auth.repository.UserRepository;
import com.pocketminder.transaction.analytics.dto.CategorySummaryDTO;
import com.pocketminder.transaction.entity.Transaction;
import com.pocketminder.transaction.entity.TransactionCategory;
import com.pocketminder.transaction.entity.TransactionSource;
import com.pocketminder.transaction.entity.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private User otherUser;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .name("Test User")
                .email("test@example.com")
                .password("hash")
                .build());

        otherUser = userRepository.save(User.builder()
                .name("Other User")
                .email("other@example.com")
                .password("hash")
                .build());
    }

    private Transaction createTransaction(User owner, String title, BigDecimal amount,
                                          TransactionType type, TransactionCategory category,
                                          TransactionSource source) {
        return Transaction.builder()
                .title(title)
                .description("Description for " + title)
                .amount(amount)
                .type(type)
                .category(category)
                .source(source)
                .transactionDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .user(owner)
                .build();
    }

    @Test
    void findByUser_shouldReturnUserTransactionsOnly() {
        transactionRepository.save(createTransaction(user, "U1", new BigDecimal("100"), TransactionType.EXPENSE,
                TransactionCategory.FOOD, TransactionSource.MANUAL));
        transactionRepository.save(createTransaction(otherUser, "O1", new BigDecimal("200"), TransactionType.EXPENSE,
                TransactionCategory.FOOD, TransactionSource.MANUAL));

        List<Transaction> userTxns = transactionRepository.findByUser(user);

        assertEquals(1, userTxns.size());
        assertEquals("U1", userTxns.get(0).getTitle());
    }

    @Test
    void findByUserAndType_shouldFilterByType() {
        transactionRepository.save(createTransaction(user, "Income1", new BigDecimal("500"),
                TransactionType.INCOME, TransactionCategory.SALARY, TransactionSource.MANUAL));
        transactionRepository.save(createTransaction(user, "Expense1", new BigDecimal("100"),
                TransactionType.EXPENSE, TransactionCategory.FOOD, TransactionSource.MANUAL));
        transactionRepository.save(createTransaction(user, "Income2", new BigDecimal("300"),
                TransactionType.INCOME, TransactionCategory.INVESTMENT, TransactionSource.MANUAL));

        List<Transaction> incomes = transactionRepository.findByUserAndType(user, TransactionType.INCOME);

        assertEquals(2, incomes.size());
        assertTrue(incomes.stream().allMatch(t -> t.getType() == TransactionType.INCOME));
    }

    @Test
    void getTotalByType_shouldReturnSumForType() {
        transactionRepository.save(createTransaction(user, "Salary", new BigDecimal("1000"),
                TransactionType.INCOME, TransactionCategory.SALARY, TransactionSource.MANUAL));
        transactionRepository.save(createTransaction(user, "Freelance", new BigDecimal("500"),
                TransactionType.INCOME, TransactionCategory.OTHER, TransactionSource.MANUAL));
        transactionRepository.save(createTransaction(user, "Food", new BigDecimal("200"),
                TransactionType.EXPENSE, TransactionCategory.FOOD, TransactionSource.MANUAL));

        BigDecimal totalIncome = transactionRepository.getTotalByType(user, TransactionType.INCOME);
        BigDecimal totalExpense = transactionRepository.getTotalByType(user, TransactionType.EXPENSE);

        assertTrue(new BigDecimal("1500").compareTo(totalIncome) == 0);
        assertTrue(new BigDecimal("200").compareTo(totalExpense) == 0);
    }

    @Test
    void getTotalByType_shouldReturnZeroWhenNoTransactions() {
        BigDecimal total = transactionRepository.getTotalByType(user, TransactionType.INCOME);

        assertEquals(BigDecimal.ZERO, total);
    }

    @Test
    void getTotalByType_shouldOnlySumCurrentUserTransactions() {
        transactionRepository.save(createTransaction(user, "My Salary", new BigDecimal("1000"),
                TransactionType.INCOME, TransactionCategory.SALARY, TransactionSource.MANUAL));
        transactionRepository.save(createTransaction(otherUser, "Other Salary", new BigDecimal("2000"),
                TransactionType.INCOME, TransactionCategory.SALARY, TransactionSource.MANUAL));

        BigDecimal myTotal = transactionRepository.getTotalByType(user, TransactionType.INCOME);

        assertTrue(new BigDecimal("1000").compareTo(myTotal) == 0);
    }

    @Test
    void getExpensesByCategory_shouldGroupByCategory() {
        transactionRepository.save(createTransaction(user, "Lunch", new BigDecimal("50"),
                TransactionType.EXPENSE, TransactionCategory.FOOD, TransactionSource.MANUAL));
        transactionRepository.save(createTransaction(user, "Dinner", new BigDecimal("100"),
                TransactionType.EXPENSE, TransactionCategory.FOOD, TransactionSource.MANUAL));
        transactionRepository.save(createTransaction(user, "Bus", new BigDecimal("30"),
                TransactionType.EXPENSE, TransactionCategory.TRANSPORT, TransactionSource.MANUAL));
        transactionRepository.save(createTransaction(user, "Movie", new BigDecimal("80"),
                TransactionType.EXPENSE, TransactionCategory.ENTERTAINMENT, TransactionSource.MANUAL));

        List<CategorySummaryDTO> summaries = transactionRepository.getExpensesByCategory(user);

        assertEquals(3, summaries.size());

        for (CategorySummaryDTO summary : summaries) {
            switch (summary.getCategory()) {
                case FOOD -> assertTrue(new BigDecimal("150").compareTo(summary.getTotal()) == 0);
                case TRANSPORT -> assertTrue(new BigDecimal("30").compareTo(summary.getTotal()) == 0);
                case ENTERTAINMENT -> assertTrue(new BigDecimal("80").compareTo(summary.getTotal()) == 0);
            }
        }
    }

    @Test
    void getExpensesByCategory_shouldOnlyIncludeExpenses() {
        transactionRepository.save(createTransaction(user, "Salary", new BigDecimal("2000"),
                TransactionType.INCOME, TransactionCategory.SALARY, TransactionSource.MANUAL));
        transactionRepository.save(createTransaction(user, "Food", new BigDecimal("100"),
                TransactionType.EXPENSE, TransactionCategory.FOOD, TransactionSource.MANUAL));

        List<CategorySummaryDTO> summaries = transactionRepository.getExpensesByCategory(user);

        assertEquals(1, summaries.size());
        assertEquals(TransactionCategory.FOOD, summaries.get(0).getCategory());
    }

    @Test
    void getExpensesByCategory_shouldReturnEmptyListWhenNoExpenses() {
        List<CategorySummaryDTO> summaries = transactionRepository.getExpensesByCategory(user);

        assertTrue(summaries.isEmpty());
    }

    @Test
    void save_shouldPersistAllTransactionFields() {
        LocalDateTime txnDate = LocalDateTime.of(2026, 6, 1, 12, 0);
        Transaction transaction = Transaction.builder()
                .title("Full Fields")
                .description("Full Description")
                .amount(new BigDecimal("99999"))
                .type(TransactionType.TRANSFER)
                .category(TransactionCategory.EDUCATION)
                .source(TransactionSource.SMS)
                .rawMessage("Raw SMS text here")
                .detectedMerchant("MERCHANT")
                .autoDetected(true)
                .transactionDate(txnDate)
                .createdAt(LocalDateTime.now())
                .user(user)
                .build();

        Transaction saved = transactionRepository.save(transaction);

        assertNotNull(saved.getId());
        assertEquals("Full Fields", saved.getTitle());
        assertEquals("Full Description", saved.getDescription());
        assertEquals(new BigDecimal("99999"), saved.getAmount());
        assertEquals(TransactionType.TRANSFER, saved.getType());
        assertEquals(TransactionCategory.EDUCATION, saved.getCategory());
        assertEquals(TransactionSource.SMS, saved.getSource());
        assertEquals("Raw SMS text here", saved.getRawMessage());
        assertEquals("MERCHANT", saved.getDetectedMerchant());
        assertTrue(saved.getAutoDetected());
        assertEquals(txnDate, saved.getTransactionDate());
        assertEquals(user.getId(), saved.getUser().getId());
    }
}
