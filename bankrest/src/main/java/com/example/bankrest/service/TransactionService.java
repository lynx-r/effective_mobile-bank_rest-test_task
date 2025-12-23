package com.example.bankrest.service;

import com.example.bankrest.dto.InternalTransferRequest;

public interface TransactionService {
  void transferBetweenOwnCards(InternalTransferRequest request);
}
