package com.example.bankapi.service;

import com.example.bankapi.dto.TransactionResponse;
import com.example.bankapi.dto.TransferRequest;
import com.example.bankapi.entity.Account;
import com.example.bankapi.entity.BankTransaction;
import com.example.bankapi.entity.TransactionType;
import com.example.bankapi.exception.AccountNotFoundException;
import com.example.bankapi.exception.InsufficientFundsException;
import com.example.bankapi.repository.AccountRepository;
import com.example.bankapi.repository.BankTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final AccountRepository accountRepository;
    private final BankTransactionRepository transactionRepository;

    @Transactional
    public TransactionResponse transfer(Long ownerId, TransferRequest request) {
        if (request.fromAccountId().equals(request.toAccountId())) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        // Lock both rows in a fixed order (lowest id first) so two concurrent transfers
        // between the same pair of accounts can never deadlock on each other.
        Long firstId = Math.min(request.fromAccountId(), request.toAccountId());
        Long secondId = Math.max(request.fromAccountId(), request.toAccountId());
        Account first = lockAccount(firstId);
        Account second = lockAccount(secondId);

        Account from = first.getId().equals(request.fromAccountId()) ? first : second;
        Account to = first.getId().equals(request.toAccountId()) ? first : second;

        if (!from.getOwnerId().equals(ownerId)) {
            throw new AccountNotFoundException("Account not found: " + from.getId());
        }
        if (from.getBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds in account " + from.getAccountNumber());
        }

        from.setBalance(from.getBalance().subtract(request.amount()));
        to.setBalance(to.getBalance().add(request.amount()));
        accountRepository.save(from);
        accountRepository.save(to);

        BankTransaction tx = BankTransaction.builder()
                .transactionRef(UUID.randomUUID().toString())
                .type(TransactionType.TRANSFER)
                .fromAccountId(from.getId())
                .toAccountId(to.getId())
                .amount(request.amount())
                .description(request.description())
                .build();
        BankTransaction saved = transactionRepository.save(tx);

        return TransactionResponse.from(saved);
    }

    private Account lockAccount(Long id) {
        return accountRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + id));
    }
}
