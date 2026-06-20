package com.example.bankapi.security;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtUserExtractor {

    // subject is the stable userId (set by auth-service); username is just a display claim
    public Long userId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
