package com.example.bankrest.service;

import com.example.bankrest.dto.CardholderResponse;
import com.example.bankrest.entity.Cardholder;

public class CardholderMapper {
  public static CardholderResponse mapToResponse(Cardholder cardholder) {
    return new CardholderResponse(cardholder.getId(), cardholder.getUsername(), cardholder.getEmail(),
        cardholder.getFirstName(),
        cardholder.getLastName(), cardholder.getEnabled());
  }
}
