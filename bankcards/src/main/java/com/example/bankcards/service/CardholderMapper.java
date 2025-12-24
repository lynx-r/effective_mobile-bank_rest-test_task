package com.example.bankcards.service;

import com.example.bankcards.dto.CardholderResponse;
import com.example.bankcards.entity.Cardholder;

public class CardholderMapper {
  public static CardholderResponse mapToResponse(Cardholder cardholder) {
    return new CardholderResponse(cardholder.getId(), cardholder.getUsername(), cardholder.getEmail(),
        cardholder.getFirstName(),
        cardholder.getLastName(), cardholder.getEnabled());
  }
}
