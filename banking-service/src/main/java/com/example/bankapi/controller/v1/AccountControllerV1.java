package com.example.bankapi.controller.v1;

import com.example.bankapi.dto.AccountResponse;
import com.example.bankapi.dto.DepositRequest;
import com.example.bankapi.dto.OpenAccountRequest;
import com.example.bankapi.dto.WithdrawRequest;
import com.example.bankapi.security.JwtUserExtractor;
import com.example.bankapi.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Deprecated - kept for backward compatibility. New clients should use /api/v2.
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountControllerV1 {

    private final AccountService accountService;
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
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse open(@AuthenticationPrincipal Jwt jwt, @RequestBody(required = false) OpenAccountRequest request) {
        return accountService.openAccount(jwtUserExtractor.userId(jwt), request);
    }

    @PostMapping("/{id}/deposit")
    public AccountResponse deposit(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody DepositRequest request) {
        return accountService.deposit(jwtUserExtractor.userId(jwt), id, request);
    }

    @PostMapping("/{id}/withdraw")
    public AccountResponse withdraw(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody WithdrawRequest request) {
        return accountService.withdraw(jwtUserExtractor.userId(jwt), id, request);
    }
}
