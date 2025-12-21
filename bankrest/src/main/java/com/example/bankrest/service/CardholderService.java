package com.example.bankrest.service;

import java.util.List;

import com.example.bankrest.dto.CardholderResponse;

public interface CardholderService {
  List<CardholderResponse> findAllUsers();

  void blockUser(Long id);

  void deleteUser(Long id);
}
