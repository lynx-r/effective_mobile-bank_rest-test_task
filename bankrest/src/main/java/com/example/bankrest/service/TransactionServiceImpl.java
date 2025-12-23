package com.example.bankrest.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bankrest.dto.InternalTransferRequest;
import com.example.bankrest.entity.Card;
import com.example.bankrest.entity.CardStatus;
import com.example.bankrest.entity.Transaction;
import com.example.bankrest.exception.InsufficientFundsException;
import com.example.bankrest.repository.CardRepository;
import com.example.bankrest.repository.TransactionRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TransactionServiceImpl implements TransactionService {

  private final CardRepository cardRepository;
  private final TransactionRepository transactionRepository;
  private final AuditService auditService;

  @Override
  public void transferBetweenOwnCards(String username, InternalTransferRequest request) {
    if (request.fromCardId().equals(request.toCardId())) {
      throw new IllegalArgumentException("Нельзя переводить средства на ту же карту");
    }

    Card fromCard = cardRepository.findByIdAndOwner_Username(request.fromCardId(), username)
        .orElseThrow(() -> new EntityNotFoundException("Карта списания не найдена"));

    Card toCard = cardRepository.findByIdAndOwner_Username(request.toCardId(), username)
        .orElseThrow(() -> new EntityNotFoundException("Карта зачисления не найдена"));

    if (fromCard.getStatus() != CardStatus.ACTIVE) {
      throw new IllegalStateException("Карта списания заблокирована");
    }
    if (toCard.getStatus() != CardStatus.ACTIVE) {
      throw new IllegalStateException("Карта зачисления заблокирована");
    }
    if (fromCard.getBalance().compareTo(request.amount()) < 0) {
      throw new InsufficientFundsException("Недостаточно средств");
    }

    fromCard.setBalance(fromCard.getBalance().subtract(request.amount()));
    toCard.setBalance(toCard.getBalance().add(request.amount()));

    Transaction tx = Transaction.builder()
        .fromCard(fromCard)
        .toCard(toCard)
        .amount(request.amount())
        .status("COMPLETED")
        .description("Перевод между своими картами")
        .build();

    Transaction savedTx = transactionRepository.save(tx);

    // Аудит перевода денег
    auditService.logTransfer(username, fromCard.getId(), toCard.getId(),
        fromCard.getCardNumberMasked(), toCard.getCardNumberMasked(),
        request.amount().toString(), "RUB");
    log.debug("Transfer completed successfully. User: {}, From Card: {}, To Card: {}, Amount: {}, Transaction ID: {}",
        username, fromCard.getCardNumberMasked(), toCard.getCardNumberMasked(),
        request.amount(), savedTx.getId());
  }
}
