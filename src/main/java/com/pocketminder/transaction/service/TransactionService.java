package com.pocketminder.transaction.service;

import com.pocketminder.auth.entity.User;
import com.pocketminder.transaction.dto.CreateTransactionDTO;
import com.pocketminder.transaction.entity.*;
import com.pocketminder.transaction.repository.TransactionRepository;
import com.pocketminder.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserService userService;

    public Transaction createManualTransaction(
            CreateTransactionDTO request
    ) {

        User user = userService.getCurrentUser();

        Transaction transaction =
                Transaction.builder()
                        .title(request.getTitle())
                        .description(
                                request.getDescription()
                        )
                        .amount(request.getAmount())
                        .type(request.getType())
                        .category(request.getCategory())
                        .source(TransactionSource.MANUAL)
                        .autoDetected(false)
                        .transactionDate(
                                LocalDateTime.now()
                        )
                        .createdAt(LocalDateTime.now())
                        .user(user)
                        .build();

        return transactionRepository.save(transaction);
    }

    /*
    todo: implement this function for SMS transactions.
        public Transaction createSmsTransaction(){}
     */

    public List<Transaction> getMyTransactions() {

        User user = userService.getCurrentUser();

        return transactionRepository.findByUser(user);
    }
}