package com.example.bankrest.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.bankrest.dto.CardResponse;
import com.example.bankrest.dto.CreateCardRequest;
import com.example.bankrest.entity.CardStatus;

public interface AdminCardService {
  Page<CardResponse> findCards(String search, Pageable pageable);

  CardResponse createCard(CreateCardRequest request);

  void updateStatus(Long id, CardStatus status);

  void deleteCard(Long id);
}
