package com.example.bankrest.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bankrest.config.CardConfig;
import com.example.bankrest.dto.CardResponse;
import com.example.bankrest.dto.CreateCardRequest;
import com.example.bankrest.entity.Card;
import com.example.bankrest.entity.CardStatus;
import com.example.bankrest.entity.Cardholder;
import com.example.bankrest.repository.CardRepository;
import com.example.bankrest.repository.CardholderRepository;
import com.example.bankrest.util.CardCryptoUtil;
import com.example.bankrest.util.CardGenerator;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminCardServiceImpl implements AdminCardService {

  private final CardRepository cardRepository;
  private final CardholderRepository cardholderRepository;
  private final CardCryptoUtil cardCryptoUtil;
  private final CardConfig cardConfig;
  private final AuditService auditService;

  @Override
  @Transactional(readOnly = true)
  public Page<CardResponse> findCards(String search, Pageable pageable) {
    auditService.logCardsListView(pageable.getPageSize(), "findAll");
    log.debug("Admin requested list of all cards. Page size: {}", pageable.getPageSize());
    return cardRepository.findByOwnerNameAndCardNumberMasked(search, pageable).map(CardMapper::mapToResponse);
  }

  @Override
  @Transactional
  public CardResponse createCard(CreateCardRequest request) {
    Cardholder owner = cardholderRepository.findById(request.cardholderId())
        .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));
    String rawCardNumber = CardGenerator.generate(cardConfig.getBin());

    Card card = Card.builder()
        .owner(owner)
        .ownerName(owner.getCardOwnerName())
        .cardNumberMasked(cardCryptoUtil.maskCardNumber(rawCardNumber))
        .cardNumberEncrypted(cardCryptoUtil.encrypt(rawCardNumber))
        .expiryDate(LocalDate.now().plusYears(4))
        .balance(BigDecimal.ZERO)
        .status(CardStatus.ACTIVE)
        .build();

    CardResponse response = CardMapper.mapToResponse(cardRepository.save(card));

    // Аудит создания карты
    auditService.logCardCreation(card.getId(), card.getCardNumberMasked(), owner.getId());
    log.debug("Card created successfully. Card ID: {}, Masked Number: {}, Owner ID: {}",
        card.getId(), card.getCardNumberMasked(), owner.getId());

    return response;
  }

  @Override
  @Transactional
  public void updateStatus(Long id, CardStatus status) {
    Card card = cardRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Карта не найдена"));

    CardStatus previousStatus = card.getStatus();
    card.setStatus(status);
    cardRepository.save(card);

    // Аудит изменения статуса карты
    auditService.logCardStatusChange(card.getId(), previousStatus.name(), status.name());
    log.debug("Card status updated. Card ID: {}, Previous Status: {}, New Status: {}",
        card.getId(), previousStatus, status);
  }

  @Override
  @Transactional
  public void deleteCard(Long id) {
    var cardOptional = cardRepository.findById(id);
    if (cardOptional.isEmpty()) {
      throw new EntityNotFoundException("Карта не найдена");
    }
    var card = cardOptional.get();
    auditService.logCardDeletion(id, card.getCardNumberMasked());
    log.debug("Card deleted. Card ID: {}, maskedCardNumber: {}", id, card.getCardNumberMasked());
    cardRepository.deleteById(id);
  }
}
