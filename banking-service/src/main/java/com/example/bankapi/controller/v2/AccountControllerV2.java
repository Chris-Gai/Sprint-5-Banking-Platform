package com.example.bankapi.controller.v2;

import com.example.bankapi.dto.AccountResponse;
import com.example.bankapi.dto.DepositRequest;
import com.example.bankapi.dto.OpenAccountRequest;
import com.example.bankapi.dto.WithdrawRequest;
import com.example.bankapi.security.JwtUserExtractor;
import com.example.bankapi.service.AccountService;
import com.example.bankapi.service.IdempotencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/accounts")
@RequiredArgsConstructor
public class AccountControllerV2 {

    private final AccountService accountService;
    private final IdempotencyService idempotencyService;
    private final JwtUserExtractor jwtUserExtractor;

    @GetMapping
    public List<AccountResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return accountService.listAccounts(jwtUserExtractor.userId(jwt));
    }

    @GetMapping("/{id}")
    public AccountResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return accountService.getAccount(jwtUserExtractor.userId(jwt), id);
    }

    @PostMapping
    public ResponseEntity<?> open(@AuthenticationPrincipal Jwt jwt,
                                   @RequestHeader("Idempotency-Key") String idempotencyKey,
                                   @RequestBody(required = false) OpenAccountRequest request) {
        Long userId = jwtUserExtractor.userId(jwt);
        return idempotencyService.execute(idempotencyKey, userId, "/api/v2/accounts", request,
                () -> ResponseEntity.status(HttpStatus.CREATED).body(accountService.openAccount(userId, request)));
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<?> deposit(@AuthenticationPrincipal Jwt jwt,
                                      @RequestHeader("Idempotency-Key") String idempotencyKey,
                                      @PathVariable Long id,
                                      @Valid @RequestBody DepositRequest request) {
        Long userId = jwtUserExtractor.userId(jwt);
        return idempotencyService.execute(idempotencyKey, userId, "/api/v2/accounts/" + id + "/deposit", request,
                () -> ResponseEntity.ok(accountService.deposit(userId, id, request)));
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<?> withdraw(@AuthenticationPrincipal Jwt jwt,
                                       @RequestHeader("Idempotency-Key") String idempotencyKey,
                                       @PathVariable Long id,
                                       @Valid @RequestBody WithdrawRequest request) {
        Long userId = jwtUserExtractor.userId(jwt);
        return idempotencyService.execute(idempotencyKey, userId, "/api/v2/accounts/" + id + "/withdraw", request,
                () -> ResponseEntity.ok(accountService.withdraw(userId, id, request)));
    }
}
