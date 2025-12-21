package com.example.bankrest.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.bankrest.dto.CardResponse;
import com.example.bankrest.dto.CreateCardRequest;
import com.example.bankrest.entity.Card;
import com.example.bankrest.entity.CardStatus;
import com.example.bankrest.entity.Cardholder;
import com.example.bankrest.repository.CardRepository;
import com.example.bankrest.repository.CardholderRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class CardServiceImplTest {

  @Mock
  private CardRepository cardRepository;

  @Mock
  private CardholderRepository cardholderRepository;

  @InjectMocks
  private CardServiceImpl cardService;

  private Cardholder testCardholder;
  private Card testCard1;
  private Card testCard2;

  @BeforeEach
  void setUp() {
    testCardholder = new Cardholder();
    testCardholder.setId(1L);
    testCardholder.setUsername("user1");
    testCardholder.setEmail("user1@example.com");
    testCardholder.setFirstName("John");
    testCardholder.setLastName("Doe");
    testCardholder.setCreatedAt(LocalDateTime.now());

    testCard1 = Card.builder()
        .id(1L)
        .cardNumberEncrypted("encrypted_val_" + UUID.randomUUID())
        .cardNumberMasked("**** **** **** 1234")
        .ownerName("John Doe")
        .expiryDate(LocalDate.now().plusYears(4))
        .status(CardStatus.ACTIVE)
        .balance(BigDecimal.valueOf(1000.50))
        .cardholder(testCardholder)
        .createdAt(LocalDateTime.now())
        .build();

    testCard2 = Card.builder()
        .id(2L)
        .cardNumberEncrypted("encrypted_val_" + UUID.randomUUID())
        .cardNumberMasked("**** **** **** 5678")
        .ownerName("Jane Smith")
        .expiryDate(LocalDate.now().plusYears(3))
        .status(CardStatus.ACTIVE)
        .balance(BigDecimal.valueOf(2500.00))
        .cardholder(testCardholder)
        .createdAt(LocalDateTime.now())
        .build();
  }

  @Test
  void findAllCards_ShouldReturnAllCards() {
    // Given
    List<Card> cards = List.of(testCard1, testCard2);
    when(cardRepository.findAll()).thenReturn(cards);

    // When
    List<CardResponse> result = cardService.findAllCards();

    // Then
    assertEquals(2, result.size());
    assertEquals("**** **** **** 1234", result.get(0).cardNumberMasked());
    assertEquals("**** **** **** 5678", result.get(1).cardNumberMasked());
    assertEquals(CardStatus.ACTIVE, result.get(0).status());
    assertEquals(CardStatus.ACTIVE, result.get(1).status());
    assertEquals(BigDecimal.valueOf(1000.50), result.get(0).balance());
    assertEquals(BigDecimal.valueOf(2500.00), result.get(1).balance());
    assertEquals(1L, result.get(0).cardholderId());
    assertEquals(1L, result.get(1).cardholderId());
    verify(cardRepository).findAll();
  }

  @Test
  void findAllCards_ShouldReturnEmptyList() {
    // Given
    when(cardRepository.findAll()).thenReturn(new ArrayList<>());

    // When
    List<CardResponse> result = cardService.findAllCards();

    // Then
    assertEquals(0, result.size());
    verify(cardRepository).findAll();
  }

  @Test
  void createCard_ShouldCreateCardSuccessfully() {
    // Given
    CreateCardRequest request = new CreateCardRequest(1L, "John Doe");
    when(cardholderRepository.findById(1L)).thenReturn(Optional.of(testCardholder));
    when(cardRepository.save(any(Card.class))).thenReturn(testCard1);

    // When
    CardResponse result = cardService.createCard(request);

    // Then
    assertEquals(1L, result.id());
    assertEquals("**** **** **** 1234", result.cardNumberMasked());
    assertEquals(CardStatus.ACTIVE, result.status());
    assertEquals(BigDecimal.valueOf(1000.50), result.balance());
    assertEquals(1L, result.cardholderId());

    verify(cardholderRepository).findById(1L);
    verify(cardRepository).save(any(Card.class));
  }

  @Test
  void createCard_ShouldThrowEntityNotFoundException_WhenUserNotFound() {
    // Given
    CreateCardRequest request = new CreateCardRequest(1L, "John Doe");
    when(cardholderRepository.findById(1L)).thenReturn(Optional.empty());

    // When & Then
    EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
        () -> cardService.createCard(request));

    assertEquals("Пользователь не найден", exception.getMessage());
    verify(cardholderRepository).findById(1L);
    verify(cardRepository, never()).save(any());
  }

  @Test
  void updateStatus_ShouldUpdateCardStatusSuccessfully() {
    // Given
    when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard1));
    when(cardRepository.save(any(Card.class))).thenReturn(testCard1);

    // When
    cardService.updateStatus(1L, CardStatus.BLOCKED);

    // Then
    verify(cardRepository).findById(1L);
    verify(cardRepository).save(testCard1);
  }

  @Test
  void updateStatus_ShouldThrowEntityNotFoundException_WhenCardNotFound() {
    // Given
    when(cardRepository.findById(1L)).thenReturn(Optional.empty());

    // When & Then
    EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
        () -> cardService.updateStatus(1L, CardStatus.BLOCKED));

    assertEquals("Карта не найдена", exception.getMessage());
    verify(cardRepository).findById(1L);
    verify(cardRepository, never()).save(any());
  }

  @Test
  void deleteCard_ShouldDeleteCardSuccessfully() {
    // Given
    when(cardRepository.existsById(1L)).thenReturn(true);
    doNothing().when(cardRepository).deleteById(1L);

    // When
    cardService.deleteCard(1L);

    // Then
    verify(cardRepository).existsById(1L);
    verify(cardRepository).deleteById(1L);
  }

  @Test
  void deleteCard_ShouldThrowEntityNotFoundException_WhenCardNotFound() {
    // Given
    when(cardRepository.existsById(1L)).thenReturn(false);

    // When & Then
    EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
        () -> cardService.deleteCard(1L));

    assertEquals("Карта не найдена", exception.getMessage());
    verify(cardRepository).existsById(1L);
    verify(cardRepository, never()).deleteById(anyLong());
  }
}
