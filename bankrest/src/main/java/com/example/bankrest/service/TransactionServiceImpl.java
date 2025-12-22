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

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionServiceImpl implements TransactionService {

  private final CardRepository cardRepository;
  private final TransactionRepository transactionRepository;

  @Override
  public void transferBetweenOwnCards(String username, InternalTransferRequest request) {
    // 1. Загружаем карты и проверяем владельца
    Card fromCard = cardRepository.findByIdAndOwner_Username(request.fromCardId(), username)
        .orElseThrow(() -> new EntityNotFoundException("Карта списания не найдена"));

    Card toCard = cardRepository.findByIdAndOwner_Username(request.toCardId(), username)
        .orElseThrow(() -> new EntityNotFoundException("Карта зачисления не найдена"));

    // 2. Проверки
    if (fromCard.getBalance().compareTo(request.amount()) < 0) {
      throw new InsufficientFundsException("Недостаточно средств");
    }
    if (fromCard.getStatus() != CardStatus.ACTIVE) {
      throw new IllegalStateException("Карта списания заблокирована");
    }

    // 3. Изменение балансов
    fromCard.setBalance(fromCard.getBalance().subtract(request.amount()));
    toCard.setBalance(toCard.getBalance().add(request.amount()));

    // 4. Сохранение истории транзакций
    Transaction tx = Transaction.builder()
        .fromCard(fromCard)
        .toCard(toCard)
        .amount(request.amount())
        .status("COMPLETED")
        .description("Перевод между своими картами")
        .build();

    transactionRepository.save(tx);
  }
}
