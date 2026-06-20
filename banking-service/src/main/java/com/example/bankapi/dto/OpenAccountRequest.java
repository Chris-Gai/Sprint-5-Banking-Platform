package com.example.bankapi.dto;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record OpenAccountRequest(
        @DecimalMin(value = "0.00") BigDecimal initialDeposit
) {}
