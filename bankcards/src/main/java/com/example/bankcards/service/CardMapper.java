package com.example.bankcards.service;

import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.entity.Card;

public class CardMapper {
  public static CardResponse mapToResponse(Card card) {
    Long ownerId = card.getOwner() != null ? card.getOwner().getId() : null;
    return new CardResponse(
        card.getId(),
        card.getOwnerName(),
        card.getCardNumberMasked(),
        card.getStatus(),
        card.getBalance(),
        ownerId);
  }
}
