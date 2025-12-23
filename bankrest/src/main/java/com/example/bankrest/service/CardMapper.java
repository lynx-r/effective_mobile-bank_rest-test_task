package com.example.bankrest.service;

import com.example.bankrest.dto.CardResponse;
import com.example.bankrest.entity.Card;

public class CardMapper {
  public static CardResponse mapToResponse(Card card) {
    Long ownerId = card.getOwner() != null ? card.getOwner().getId() : null;
    return new CardResponse(
        card.getId(),
        card.getCardNumberMasked(),
        card.getStatus(),
        card.getBalance(),
        ownerId);
  }
}
