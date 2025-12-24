package com.example.bankcards.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.repository.CardRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardholderCardServiceImpl implements CardholderCardService {

  private final CardRepository cardRepository;
  private final AuditService auditService;
  private final AuthenticationFacade authenticationFacade;

  @Override
  @Transactional(readOnly = true)
  public Page<CardResponse> findCardholderCards(String search, Pageable pageable) {
    Pageable sortedPageable = pageable.isPaged() && pageable.getSort().isUnsorted()
        ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("createdAt").descending())
        : pageable;

    String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;

    if (cleanSearch == null || cleanSearch.isEmpty()) {
      auditService.logCardsListView(sortedPageable.getPageSize(), "findByOwner_Username");
      log.debug("User requested list of cards. Page size: {}", sortedPageable.getPageSize());
      return cardRepository.findByOwner_Username(authenticationFacade.getAuthenticationName(),
          sortedPageable).map(CardMapper::mapToResponse);
    }

    auditService.logCardsListView(sortedPageable.getPageSize(), "findByUserWithFilter");
    log.debug("User requested list of cards with filter. Search: {}, Page size: {}", cleanSearch,
        sortedPageable.getPageSize());
    return cardRepository
        .findByOwner_UsernameAndCardNumberMasked(
            authenticationFacade.getAuthenticationName(), cleanSearch, sortedPageable)
        .map(CardMapper::mapToResponse);
  }

  @Override
  @Transactional
  public void blockOwnCard(Long cardId) {
    Card card = cardRepository
        .findByIdAndOwner_Username(cardId, authenticationFacade
            .getAuthenticationName())
        .orElseThrow(() -> new AccessDeniedException("Карта не найдена или не принадлежит вам"));

    if (card.getStatus() == CardStatus.BLOCKED) {
      log.debug("User {} attempted to block already blocked card {}", cardId);
      return;
    }

    CardStatus previousStatus = card.getStatus();
    card.setIsBlockRequested(true);
    card.setBlockRequestedAt(LocalDateTime.now());
    cardRepository.save(card);

    // Аудит блокировки карты пользователем
    auditService.logCardBlocking(card.getId(), card.getCardNumberMasked());
    log.debug("Card blocked by user. Card ID: {}, Masked Number: {}, Previous Status: {}",
        card.getId(), card.getCardNumberMasked(), previousStatus);
  }

  @Override
  public BigDecimal getCardholderCardBalance(Long cardId) {
    return cardRepository.findByIdAndOwner_Username(cardId, authenticationFacade
        .getAuthenticationName())
        .map((card) -> {
          BigDecimal balance = card.getBalance();
          auditService.logBalanceView(cardId, card.getCardNumberMasked(), card.getBalance());
          log.debug("User requested balance. Card ID: {}, Masked Number: {}, Balance: {}",
              card.getId(), card.getCardNumberMasked(), balance);
          return balance;
        })
        .orElseThrow(() -> new AccessDeniedException("Доступ запрещен"));
  }

}
