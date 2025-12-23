package com.example.bankrest.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.bankrest.dto.CardholderResponse;
import com.example.common.auth.event.UserCreatedEvent;

public interface AdminCardholderService {
  Page<CardholderResponse> findCardholders(String search, Pageable pageable);

  void registerCardholder(UserCreatedEvent event);

  void blockCardholder(Long id);

  void deleteCardholder(Long id);

}
