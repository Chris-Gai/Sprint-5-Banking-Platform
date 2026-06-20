package com.example.bankapi.service;

import com.example.bankapi.dto.AccountResponse;
import com.example.bankapi.dto.DepositRequest;
import com.example.bankapi.dto.OpenAccountRequest;
import com.example.bankapi.dto.WithdrawRequest;
import com.example.bankapi.entity.Account;
import com.example.bankapi.entity.AccountStatus;
import com.example.bankapi.entity.BankTransaction;
import com.example.bankapi.entity.TransactionType;
import com.example.bankapi.exception.AccountNotFoundException;
import com.example.bankapi.exception.InsufficientFundsException;
import com.example.bankapi.repository.AccountRepository;
import com.example.bankapi.repository.BankTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final BankTransactionRepository transactionRepository;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public AccountResponse openAccount(Long ownerId, OpenAccountRequest request) {
        BigDecimal initial = request != null && request.initialDeposit() != null
                ? request.initialDeposit()
                : BigDecimal.ZERO;

        Account account = Account.builder()
                .accountNumber(generateAccountNumber())
                .ownerId(ownerId)
                .balance(initial)
                .status(AccountStatus.ACTIVE)
                .build();

        Account saved = accountRepository.save(account);

        if (initial.compareTo(BigDecimal.ZERO) > 0) {
            recordTransaction(TransactionType.DEPOSIT, null, saved.getId(), initial, "Initial deposit");
        }

        return AccountResponse.from(saved);
    }

    public List<AccountResponse> listAccounts(Long ownerId) {
        return accountRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream()
                .map(AccountResponse::from)
                .toList();
    }

    public AccountResponse getAccount(Long ownerId, Long accountId) {
        return AccountResponse.from(findOwnedAccount(ownerId, accountId));
    }

    @Transactional
    public AccountResponse deposit(Long ownerId, Long accountId, DepositRequest request) {
        Account account = findOwnedAccount(ownerId, accountId);
        account.setBalance(account.getBalance().add(request.amount()));
        Account saved = accountRepository.save(account);
        recordTransaction(TransactionType.DEPOSIT, null, accountId, request.amount(), request.description());
        return AccountResponse.from(saved);
    }

    @Transactional
    public AccountResponse withdraw(Long ownerId, Long accountId, WithdrawRequest request) {
        Account account = findOwnedAccount(ownerId, accountId);
        if (account.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds in account " + account.getAccountNumber());
        }
        account.setBalance(account.getBalance().subtract(request.amount()));
        Account saved = accountRepository.save(account);
        recordTransaction(TransactionType.WITHDRAWAL, accountId, null, request.amount(), request.description());
        return AccountResponse.from(saved);
    }

    private Account findOwnedAccount(Long ownerId, Long accountId) {
        return accountRepository.findByIdAndOwnerId(accountId, ownerId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));
    }

    private void recordTransaction(TransactionType type, Long fromId, Long toId, BigDecimal amount, String description) {
        BankTransaction tx = BankTransaction.builder()
                .transactionRef(UUID.randomUUID().toString())
                .type(type)
                .fromAccountId(fromId)
                .toAccountId(toId)
                .amount(amount)
                .description(description)
                .build();
        transactionRepository.save(tx);
    }

    private String generateAccountNumber() {
        String candidate;
        do {
            candidate = String.format("%010d", RANDOM.nextLong(0, 10_000_000_000L));
        } while (accountRepository.existsByAccountNumber(candidate));
        return candidate;
    }
}
