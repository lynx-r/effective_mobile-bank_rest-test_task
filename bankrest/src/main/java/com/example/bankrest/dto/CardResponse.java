package com.example.bankrest.dto;

import java.math.BigDecimal;

import com.example.bankrest.entity.CardStatus;

public record CardResponse(Long id, String cardNumberMasked, CardStatus status, BigDecimal balance, Long cardholderId) {
}
