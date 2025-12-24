package com.example.bankcards.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.entity.CardStatus;

public interface AdminCardService {
  Page<CardResponse> findCards(String search, Pageable pageable);

  CardResponse createCard(CreateCardRequest request);

  void updateStatus(Long id, CardStatus status);

  void deleteCard(Long id);
}
