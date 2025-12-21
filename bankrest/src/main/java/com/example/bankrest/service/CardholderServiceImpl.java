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

@Service
@RequiredArgsConstructor
@Transactional
public class CardholderServiceImpl implements CardholderService {

  private final CardholderRepository cardholderRepository;

  @Override
  @Transactional(readOnly = true)
  public List<CardholderResponse> findAllUsers() {
    return cardholderRepository.findAll().stream()
        .map(u -> new CardholderResponse(u.getId(), u.getUsername(), u.getFirstName(), u.getLastName(), u.getEnabled()))
        .toList();
  }

  @Override
  public void blockUser(Long id) {
    Cardholder cardholder = cardholderRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));
    // Здесь логика блокировки, например, установка флага enabled = false
    // Если это влияет на карты, можно заблокировать и их:
    cardholder.setEnabled(false);
    cardholder.getCards().forEach(card -> card.setStatus(CardStatus.BLOCKED));
    cardholderRepository.save(cardholder);
  }

  @Override
  public void deleteUser(Long id) {
    // Благодаря onDelete: CASCADE в Liquibase, карты удалятся автоматически в БД
    cardholderRepository.deleteById(id);
  }
}
