package com.example.bankcards.service;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.bankcards.dto.CardResponse;

public interface CardholderCardService {
  Page<CardResponse> findCardholderCards(String search, Pageable pageable);

  void blockOwnCard(Long cardId);

  BigDecimal getCardholderCardBalance(Long cardId);
}
