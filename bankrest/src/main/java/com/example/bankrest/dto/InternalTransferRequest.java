package com.example.bankrest.dto;

import java.math.BigDecimal;

public record InternalTransferRequest(
    Long fromCardId,
    Long toCardId,
    BigDecimal amount) {
}
