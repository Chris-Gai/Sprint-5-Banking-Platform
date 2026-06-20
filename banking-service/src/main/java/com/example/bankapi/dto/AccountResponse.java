package com.example.bankapi.dto;

import com.example.bankapi.entity.Account;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AccountResponse(
        Long id,
        String accountNumber,
        BigDecimal balance,
        String status,
        LocalDateTime createdAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getBalance(),
                account.getStatus().name(),
                account.getCreatedAt());
    }
}
