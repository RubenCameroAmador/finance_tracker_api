package com.pocketminder.transaction.repository;

import com.pocketminder.auth.entity.User;
import com.pocketminder.transaction.analytics.dto.CategorySummaryDTO;
import com.pocketminder.transaction.entity.Transaction;
import com.pocketminder.transaction.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface TransactionRepository
        extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUser(User user);

    List<Transaction> findByUserAndType(
            User user,
            TransactionType type
    );

    @Query("""
    SELECT COALESCE(SUM(t.amount), 0)
    FROM Transaction t
    WHERE t.user = :user
    AND t.type = :type
    """)
    BigDecimal getTotalByType(
            @Param("user") User user,
            @Param("type") TransactionType type
    );

    @Query("""
    SELECT new com.pocketminder.transaction.analytics.dto.CategorySummaryDTO(
        t.category,
        SUM(t.amount)
    )
    FROM Transaction t
    WHERE t.user = :user
    AND t.type = 'EXPENSE'
    GROUP BY t.category
    """)
    List<CategorySummaryDTO>
    getExpensesByCategory(
            @Param("user") User user
    );
}
