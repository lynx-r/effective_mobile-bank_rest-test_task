package com.example.bankrest.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bankrest.dto.UserResponse;
import com.example.bankrest.entity.Cardholder;
import com.example.bankrest.repository.CardholderRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

  private final CardholderRepository cardholderRepository;

  @Override
  @Transactional(readOnly = true)
  public List<UserResponse> findAllUsers() {
    return cardholderRepository.findAll().stream()
        .map(u -> new UserResponse(u.getId(), u.getUsername(), true))
        .toList();
  }

  @Override
  public void blockUser(Long id) {
    Cardholder user = cardholderRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден"));
    // Здесь логика блокировки, например, установка флага enabled = false
    // Если это влияет на карты, можно заблокировать и их:
    // user.getCards().forEach(card -> card.setStatus(CardStatus.BLOCKED));
    cardholderRepository.save(user);
  }

  @Override
  public void deleteUser(Long id) {
    // Благодаря onDelete: CASCADE в Liquibase, карты удалятся автоматически в БД
    cardholderRepository.deleteById(id);
  }
}
