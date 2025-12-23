package com.example.bankrest.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bankrest.dto.CardholderResponse;
import com.example.bankrest.dto.CreateCardRequest;
import com.example.bankrest.entity.CardStatus;
import com.example.bankrest.entity.Cardholder;
import com.example.bankrest.repository.CardholderRepository;
import com.example.common.auth.event.UserCreatedEvent;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminCardholderServiceImpl implements AdminCardholderService {

  private final AdminCardService cardService;
  private final CardholderRepository cardholderRepository;
  private final AuditService auditService;

  @Override
  @Transactional(readOnly = true)
  public Page<CardholderResponse> findCardholders(String search, Pageable pageable) {
    log.debug("Admin requested list of all cardholders. Page size: {}", pageable.getPageSize());
    auditService.logCardholdersListView(null, "findAll");
    return cardholderRepository.findByUserInfo(search, pageable)
        .map(CardholderMapper::mapToResponse);
  }

  @Override
  @Transactional
  public void registerCardholder(UserCreatedEvent event) {
    var cardholder = Cardholder.builder().username(event.username()).email(event.email()).firstName(event.firstName())
        .lastName(event.lastName()).enabled(true)
        .createdAt(event.createdAt()).build();

    cardholder = cardholderRepository.save(cardholder);
    var cardResponse = cardService.createCard(new CreateCardRequest(cardholder.getId()));

    log.debug("Cardholder created with default card. Cardholder ID: {}, Card ID: {}, Card Masked Number: {}",
        cardholder.getId(), cardResponse.id(), cardResponse.cardNumberMasked());
    auditService.logCardholderRegister(cardholder.getId(), cardResponse.id(), cardResponse.cardNumberMasked());
  }

  @Override
  @Transactional
  public void blockCardholder(Long id) {
    Cardholder cardholder = cardholderRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));
    cardholder.setEnabled(false);
    if (cardholder.getCards() != null && !cardholder.getCards().isEmpty()) {
      cardholder.getCards().forEach(card -> card.setStatus(CardStatus.BLOCKED));
    }
    auditService.logCardholderBlocking(id);
    log.debug("Cardholder blocked. Cardholder ID: {}", id);
    cardholderRepository.save(cardholder);
  }

  @Override
  @Transactional
  public void deleteCardholder(Long id) {
    if (!cardholderRepository.existsById(id)) {
      throw new EntityNotFoundException("Пользователь не найден");
    }
    auditService.logCardholderDeletion(id);
    log.debug("Card deleted. Cardholder ID: {}", id);
    cardholderRepository.deleteById(id);
  }
}
