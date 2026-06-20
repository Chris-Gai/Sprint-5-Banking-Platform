package com.example.bankapi.repository;

import com.example.bankapi.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    Optional<Account> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByAccountNumber(String accountNumber);

    // Locks the row for the duration of the transaction - prevents lost updates during transfers
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(Long id);
}
