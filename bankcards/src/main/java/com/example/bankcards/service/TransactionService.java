package com.example.bankcards.service;

import com.example.bankcards.dto.InternalTransferRequest;

public interface TransactionService {
  void transferBetweenOwnCards(InternalTransferRequest request);
}
