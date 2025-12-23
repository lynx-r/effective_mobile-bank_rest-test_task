package com.example.bankrest.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InternalTransferRequest(
                @NotNull(message = "From card ID cannot be null") @Positive(message = "From card ID must be positive") Long fromCardId,
                @NotNull(message = "To card ID cannot be null") @Positive(message = "To card ID must be positive") Long toCardId,
                @NotNull(message = "Amount cannot be null") @Positive(message = "Amount must be positive") @DecimalMin("0.01") @DecimalMax("1000000000") BigDecimal amount) {
}
