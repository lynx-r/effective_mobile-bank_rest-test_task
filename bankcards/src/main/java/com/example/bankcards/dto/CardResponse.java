package com.example.bankcards.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.bankcards.entity.CardStatus;

public record CardResponse(
    Long id,
    String ownerName,
    String cardNumberMasked,
    CardStatus status,
    BigDecimal balance,
    Boolean isBlockRequested,
    LocalDateTime blockRequestedAt,
    Long cardholderId) {
}
