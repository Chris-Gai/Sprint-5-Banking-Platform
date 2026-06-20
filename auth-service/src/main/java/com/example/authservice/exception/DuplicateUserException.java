package com.example.authservice.exception;

public class DuplicateUserException extends RuntimeException {
    public DuplicateUserException(String message) {
        super(message);
    }
}
