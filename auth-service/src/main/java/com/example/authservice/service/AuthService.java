package com.example.authservice.service;

import com.example.authservice.dto.AuthResponse;
import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.RegisterRequest;
import com.example.authservice.entity.Role;
import com.example.authservice.entity.User;
import com.example.authservice.exception.DuplicateUserException;
import com.example.authservice.repository.UserRepository;
import com.example.authservice.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateUserException("Username already taken: " + request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateUserException("Email already registered: " + request.email());
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();

        // save first - token subject needs the DB-generated userId
        User saved = userRepository.save(user);

        return buildAuthResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new DuplicateUserException("User not found"));

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        try {
            String token = jwtService.generateToken(user, user.getId());
            return new AuthResponse(token, "Bearer", jwtService.getExpirationMs(), user.getId(), user.getUsername());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate token", e);
        }
    }
}
