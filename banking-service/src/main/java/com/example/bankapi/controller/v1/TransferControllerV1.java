package com.example.bankapi.controller.v1;

import com.example.bankapi.dto.TransactionResponse;
import com.example.bankapi.dto.TransferRequest;
import com.example.bankapi.security.JwtUserExtractor;
import com.example.bankapi.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferControllerV1 {

    private final TransferService transferService;
    private final JwtUserExtractor jwtUserExtractor;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse transfer(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TransferRequest request) {
        return transferService.transfer(jwtUserExtractor.userId(jwt), request);
    }
}
