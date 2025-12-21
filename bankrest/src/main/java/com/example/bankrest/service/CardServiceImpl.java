package com.example.bankrest.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
import com.example.bankrest.util.CardUtils;

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
    String rawCardNumber = CardUtils.generateCardNumber(BIN);

    Card card = Card.builder()
        .cardholder(owner)
        .ownerName(CardUtils.createOwnerName(owner))
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
        card.getCardholder().getId());
  }
}
