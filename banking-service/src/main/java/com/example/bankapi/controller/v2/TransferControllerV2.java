package com.example.bankapi.controller.v2;

import com.example.bankapi.dto.TransferRequest;
import com.example.bankapi.security.JwtUserExtractor;
import com.example.bankapi.service.IdempotencyService;
import com.example.bankapi.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/transfers")
@RequiredArgsConstructor
public class TransferControllerV2 {

    private final TransferService transferService;
    private final IdempotencyService idempotencyService;
    private final JwtUserExtractor jwtUserExtractor;

    @PostMapping
    public ResponseEntity<?> transfer(@AuthenticationPrincipal Jwt jwt,
                                       @RequestHeader("Idempotency-Key") String idempotencyKey,
                                       @Valid @RequestBody TransferRequest request) {
        Long userId = jwtUserExtractor.userId(jwt);
        return idempotencyService.execute(idempotencyKey, userId, "/api/v2/transfers", request,
                () -> ResponseEntity.status(HttpStatus.CREATED).body(transferService.transfer(userId, request)));
    }
}
