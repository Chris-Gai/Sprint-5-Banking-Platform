package com.example.authservice.controller;

import com.example.authservice.security.JwtService;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class JwksController {

    private final JwtService jwtService;

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        RSAKey jwk = new RSAKey.Builder(jwtService.getPublicKey())
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyID(jwtService.getKeyId())
                .build();
        return new JWKSet(jwk).toJSONObject();
    }
}
