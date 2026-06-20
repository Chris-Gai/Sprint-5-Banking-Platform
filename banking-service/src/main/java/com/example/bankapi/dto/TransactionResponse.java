package com.example.bankapi.dto;

import com.example.bankapi.entity.BankTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        String transactionRef,
        String type,
        Long fromAccountId,
        Long toAccountId,
        BigDecimal amount,
        String description,
        LocalDateTime createdAt
) {
    public static TransactionResponse from(BankTransaction tx) {
        return new TransactionResponse(
                tx.getTransactionRef(),
                tx.getType().name(),
                tx.getFromAccountId(),
                tx.getToAccountId(),
                tx.getAmount(),
                tx.getDescription(),
                tx.getCreatedAt());
    }
}
