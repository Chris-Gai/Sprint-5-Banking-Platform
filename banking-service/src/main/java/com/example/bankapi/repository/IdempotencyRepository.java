package com.example.bankapi.repository;

import com.example.bankapi.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, Long> {
    Optional<IdempotencyRecord> findByIdempotencyKeyAndUserId(String idempotencyKey, Long userId);
}
