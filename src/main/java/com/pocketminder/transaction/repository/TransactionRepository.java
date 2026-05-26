package com.pocketminder.transaction.repository;

import com.pocketminder.auth.entity.User;
import com.pocketminder.transaction.entity.Transaction;
import com.pocketminder.transaction.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository
        extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUser(User user);

    List<Transaction> findByUserAndType(
            User user,
            TransactionType type
    );
}
