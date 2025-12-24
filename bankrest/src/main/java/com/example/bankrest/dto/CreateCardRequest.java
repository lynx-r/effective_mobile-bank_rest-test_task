package com.example.bankrest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateCardRequest(
    @NotNull(message = "Cardholder ID cannot be null") @Positive(message = "Cardholder ID must be positive") Long cardholderId) {
}
