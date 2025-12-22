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

@Service
@RequiredArgsConstructor
@Transactional
public class CardServiceImpl implements CardService {

  // TODO: брать из конфига
  private static final String BIN = "444455";

  private final CardRepository cardRepository;
  private final CardholderRepository cardholderRepository;
  private final CardCryptoUtil cardCryptoUtil;

  @Override
  @Transactional(readOnly = true)
  public List<CardResponse> findAllCards() {
    return cardRepository.findAll().stream()
        .map(this::mapToResponse)
        .toList();
  }

  @Override
  public CardResponse createCard(CreateCardRequest request) {
    Cardholder owner = cardholderRepository.findById(request.cardholderId())
        .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));
    String rawCardNumber = CardGenerator.generate(BIN);

    Card card = Card.builder()
        .owner(owner)
        .ownerName(owner.getCardOwnerName())
        .cardNumberMasked(cardCryptoUtil.maskCardNumber(rawCardNumber))
        .cardNumberEncrypted(cardCryptoUtil.encrypt(rawCardNumber))
        .expiryDate(LocalDate.now().plusYears(4))
        .balance(BigDecimal.ZERO)
        .status(CardStatus.ACTIVE)
        .build();

    return mapToResponse(cardRepository.save(card));
  }

  @Override
  public void updateStatus(Long id, CardStatus status) {
    Card card = cardRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Карта не найдена"));
    card.setStatus(status);
    cardRepository.save(card);
  }

  @Override
  public void deleteCard(Long id) {
    if (!cardRepository.existsById(id)) {
      throw new EntityNotFoundException("Карта не найдена");
    }
    cardRepository.deleteById(id);
  }

  private CardResponse mapToResponse(Card card) {
    return new CardResponse(
        card.getId(),
        card.getCardNumberMasked(),
        card.getStatus(),
        card.getBalance(),
        card.getOwner().getId());
  }

  @Override
  @Transactional(readOnly = true)
  public Page<CardResponse> findUserCards(String username, String search, Pageable pageable) {
    Pageable sortedPageable = pageable.isPaged() && pageable.getSort().isUnsorted()
        ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("createdAt").descending())
        : pageable;

    String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;

    if (cleanSearch == null || cleanSearch.isEmpty()) {
      return cardRepository.findByOwner_Username(username,
          sortedPageable).map(this::mapToResponse);
    }

    return cardRepository.findByUserWithFilter(username, cleanSearch, sortedPageable).map(this::mapToResponse);
  }

  @Override
  public void blockOwnCard(String username, Long cardId) {
    Card card = cardRepository.findByIdAndOwner_Username(cardId, username)
        .orElseThrow(() -> new AccessDeniedException("Карта не найдена или не принадлежит вам"));
    card.setStatus(CardStatus.BLOCKED);
    cardRepository.save(card);
  }

  @Override
  public BigDecimal getUserCardBalance(String username, Long cardId) {
    return cardRepository.findByIdAndOwner_Username(cardId, username)
        .map(Card::getBalance)
        .orElseThrow(() -> new AccessDeniedException("Доступ запрещен"));
  }

}
