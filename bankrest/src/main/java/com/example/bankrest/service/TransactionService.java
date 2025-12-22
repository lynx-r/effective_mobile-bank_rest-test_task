package com.example.bankrest.service;

import com.example.bankrest.dto.InternalTransferRequest;

public interface TransactionService {
  void transferBetweenOwnCards(String username, InternalTransferRequest request);
}
