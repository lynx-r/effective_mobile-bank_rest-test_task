package com.example.bankrest.service;

import java.util.List;

import com.example.bankrest.dto.CardResponse;
import com.example.bankrest.dto.CreateCardRequest;
import com.example.bankrest.entity.CardStatus;

public interface CardService {
  List<CardResponse> findAllCards();

  CardResponse createCard(CreateCardRequest request);

  void updateStatus(Long id, CardStatus status);

  void deleteCard(Long id);
}
