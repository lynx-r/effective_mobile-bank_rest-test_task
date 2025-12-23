package com.example.bankrest.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
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
public class CardServiceImpl implements CardService {

  private final CardRepository cardRepository;
  private final CardholderRepository cardholderRepository;
  private final CardCryptoUtil cardCryptoUtil;
  private final CardConfig cardConfig;
  private final AuditService auditService;

  @Override
  @Transactional(readOnly = true)
  public List<CardResponse> findAllCards() {
    List<CardResponse> cards = cardRepository.findAll().stream()
        .map(CardMapper::mapToResponse)
        .toList();

    auditService.logCardsListView("admin", null, "findAll");
    log.info("Admin requested list of all cards. Total cards: {}", cards.size());
    return cards;
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
    auditService.logCardCreation("admin", card.getId(), card.getCardNumberMasked(), owner.getId());
    log.info("Card created successfully. Card ID: {}, Masked Number: {}, Owner ID: {}",
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
    // cardRepository.save(card);

    // Аудит изменения статуса карты
    auditService.logCardStatusChange("admin", card.getId(), previousStatus.name(), status.name());
    log.warn("Card status updated. Card ID: {}, Previous Status: {}, New Status: {}",
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
    auditService.logCardDeletion("admin", id, card.getCardNumberMasked());
    log.warn("Card deleted. Card ID: {}, maskedCardNumber: {}", id, card.getCardNumberMasked());
    cardRepository.deleteById(id);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<CardResponse> findUserCards(String username, String search, Pageable pageable) {
    Pageable sortedPageable = pageable.isPaged() && pageable.getSort().isUnsorted()
        ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("createdAt").descending())
        : pageable;

    String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;

    if (cleanSearch == null || cleanSearch.isEmpty()) {
      auditService.logCardsListView("admin", sortedPageable.getPageSize(), "findByOwner_Username");
      log.info("User requested list of cards. Page size: {}", sortedPageable.getPageSize());
      return cardRepository.findByOwner_Username(username,
          sortedPageable).map(CardMapper::mapToResponse);
    }

    auditService.logCardsListView("admin", sortedPageable.getPageSize(), "findByUserWithFilter");
    log.info("User requested list of cards with filter. Search: {}, Page size: {}", cleanSearch,
        sortedPageable.getPageSize());
    return cardRepository.findByUserWithFilter(username, cleanSearch, sortedPageable).map(CardMapper::mapToResponse);
  }

  @Override
  @Transactional
  public void blockOwnCard(String username, Long cardId) {
    Card card = cardRepository.findByIdAndOwner_Username(cardId, username)
        .orElseThrow(() -> new AccessDeniedException("Карта не найдена или не принадлежит вам"));

    if (card.getStatus() == CardStatus.BLOCKED) {
      log.info("User {} attempted to block already blocked card {}", username, cardId);
      return;
    }

    CardStatus previousStatus = card.getStatus();
    card.setStatus(CardStatus.BLOCKED);
    // cardRepository.save(card);

    // Аудит блокировки карты пользователем
    auditService.logCardBlocking(username, card.getId(), card.getCardNumberMasked());
    log.warn("Card blocked by user. Username: {}, Card ID: {}, Masked Number: {}, Previous Status: {}",
        username, card.getId(), card.getCardNumberMasked(), previousStatus);
  }

  @Override
  public BigDecimal getUserCardBalance(String username, Long cardId) {
    return cardRepository.findByIdAndOwner_Username(cardId, username)
        .map((card) -> {
          BigDecimal balance = card.getBalance();
          auditService.logBalanceView("user", cardId, card.getCardNumberMasked(), card.getBalance());
          log.info("User requested balance. Username: {}, Card ID: {}, Masked Number: {}, Balance: {}",
              username, card.getId(), card.getCardNumberMasked(), balance);
          return balance;
        })
        .orElseThrow(() -> new AccessDeniedException("Доступ запрещен"));
  }

}
