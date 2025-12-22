package com.example.bankrest.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.bankrest.dto.CardResponse;
import com.example.bankrest.dto.CreateCardRequest;
import com.example.bankrest.entity.CardStatus;

public interface CardService {
  List<CardResponse> findAllCards();

  CardResponse createCard(CreateCardRequest request);

  void updateStatus(Long id, CardStatus status);

  void deleteCard(Long id);

  Page<CardResponse> findUserCards(String username, String search, Pageable pageable);

  void blockOwnCard(String username, Long cardId);

  BigDecimal getUserCardBalance(String username, Long cardId);
}
