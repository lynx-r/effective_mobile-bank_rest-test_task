package com.example.bankrest.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bankrest.dto.CardholderResponse;
import com.example.bankrest.entity.CardStatus;
import com.example.bankrest.entity.Cardholder;
import com.example.bankrest.repository.CardholderRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardholderServiceImpl implements CardholderService {

  private final CardholderRepository cardholderRepository;
  private final AuditService auditService;

  @Override
  @Transactional(readOnly = true)
  public List<CardholderResponse> findAllUsers() {
    List<CardholderResponse> cardholders = cardholderRepository.findAll().stream()
        .map(u -> new CardholderResponse(u.getId(), u.getUsername(), u.getFirstName(), u.getLastName(), u.getEnabled()))
        .toList();
    log.debug("Admin requested list of all cardholders. Total cardholders: {}", cardholders.size());
    auditService.logCardholdersListView("admin", null, "findAll");
    return cardholders;
  }

  @Override
  @Transactional
  public void blockUser(Long id) {
    Cardholder cardholder = cardholderRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));
    cardholder.setEnabled(false);
    if (cardholder.getCards() != null && !cardholder.getCards().isEmpty()) {
      cardholder.getCards().forEach(card -> card.setStatus(CardStatus.BLOCKED));
    }
    auditService.logCardholderBlocking("admin", id);
    log.debug("Cardholder blocked by admin. Cardholder ID: {}", id);
  }

  @Override
  @Transactional
  public void deleteUser(Long id) {
    if (!cardholderRepository.existsById(id)) {
      throw new EntityNotFoundException("Пользователь не найден");
    }
    auditService.logCardholderDeletion("admin", id);
    log.debug("Card deleted. Cardholder ID: {}", id);
    cardholderRepository.deleteById(id);
  }
}
