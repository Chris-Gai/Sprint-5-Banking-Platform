package com.example.bankapi.service;

import com.example.bankapi.entity.IdempotencyRecord;
import com.example.bankapi.exception.IdempotencyConflictException;
import com.example.bankapi.repository.IdempotencyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;

    /**
     * Runs action() at most once per (idempotencyKey, userId, request body).
     * A retried request with the same key + body replays the stored response.
     * The same key with a different body is rejected as a conflict.
     */
    @Transactional
    public <T> ResponseEntity<?> execute(String idempotencyKey, Long userId, String path,
                                          Object requestBody, Supplier<ResponseEntity<T>> action) {
        String requestHash = hash(requestBody);

        Optional<IdempotencyRecord> existing = idempotencyRepository.findByIdempotencyKeyAndUserId(idempotencyKey, userId);
        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            if (!record.getRequestHash().equals(requestHash)) {
                throw new IdempotencyConflictException(
                        "Idempotency-Key " + idempotencyKey + " was already used with a different request body");
            }
            return replay(record);
        }

        ResponseEntity<T> response = action.get();
        save(idempotencyKey, userId, path, requestHash, response);
        return response;
    }

    private ResponseEntity<?> replay(IdempotencyRecord record) {
        try {
            Object body = objectMapper.readValue(record.getResponseBody(), Object.class);
            return ResponseEntity.status(record.getResponseStatus()).body(body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to replay cached idempotent response", e);
        }
    }

    private void save(String key, Long userId, String path, String requestHash, ResponseEntity<?> response) {
        try {
            String bodyJson = objectMapper.writeValueAsString(response.getBody());
            IdempotencyRecord record = IdempotencyRecord.builder()
                    .idempotencyKey(key)
                    .userId(userId)
                    .requestPath(path)
                    .requestHash(requestHash)
                    .responseStatus(response.getStatusCode().value())
                    .responseBody(bodyJson)
                    .build();
            idempotencyRepository.save(record);
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist idempotency record", e);
        }
    }

    private String hash(Object requestBody) {
        try {
            String json = objectMapper.writeValueAsString(requestBody);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash request body", e);
        }
    }
}
