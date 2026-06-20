package com.example.bankapi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    private String accountNumber;

    // No @ManyToOne to a User entity - that entity lives in auth-service's database.
    // The JWT (signed by auth-service) is the trust boundary here, not a foreign key.
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Version
    private Long version;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (balance == null) balance = BigDecimal.ZERO;
        if (status == null) status = AccountStatus.ACTIVE;
    }
}
