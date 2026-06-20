package com.example.bankapi.exception;

// Thrown when the same Idempotency-Key is reused with a different request body
public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException(String message) {
        super(message);
    }
}
