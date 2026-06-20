package com.example.bankapi.repository;

import com.example.bankapi.entity.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

    Optional<BankTransaction> findByTransactionRef(String transactionRef);

    List<BankTransaction> findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(Long fromAccountId, Long toAccountId);
}
