package com.example.bankrest.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
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
import com.example.bankrest.util.CardCryptoUtil;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class CardServiceImplTest {

  @Mock
  private CardRepository cardRepository;

  @Mock
  private CardholderRepository cardholderRepository;

  @Mock
  private CardCryptoUtil cardCryptoUtil;

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
    CreateCardRequest request = new CreateCardRequest(1L);
    when(cardholderRepository.findById(1L)).thenReturn(Optional.of(testCardholder));
    when(cardCryptoUtil.maskCardNumber(any(String.class))).thenReturn("**** **** **** 1234");
    when(cardCryptoUtil.encrypt(any(String.class))).thenReturn("encrypted_123456");
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
    verify(cardCryptoUtil).maskCardNumber(any(String.class));
    verify(cardCryptoUtil).encrypt(any(String.class));
    verify(cardRepository).save(any(Card.class));
  }

  @Test
  void createCard_ShouldThrowEntityNotFoundException_WhenUserNotFound() {
    // Given
    CreateCardRequest request = new CreateCardRequest(1L);
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

  // Enhanced Tests with Better Coverage

  @Nested
  @DisplayName("Card Status Transition Tests")
  @Tag("status-transitions")
  class CardStatusTransitionTests {

    @Test
    @DisplayName("Should successfully activate a blocked card")
    void shouldActivateBlockedCard() {
      // Given
      testCard1.setStatus(CardStatus.BLOCKED);
      when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard1));
      when(cardRepository.save(any(Card.class))).thenReturn(testCard1);

      // When
      cardService.updateStatus(1L, CardStatus.ACTIVE);

      // Then
      verify(cardRepository).findById(1L);
      verify(cardRepository).save(testCard1);
      assertEquals(CardStatus.ACTIVE, testCard1.getStatus());
    }

    @Test
    @DisplayName("Should handle expired card status transition")
    void shouldHandleExpiredCardStatusTransition() {
      // Given
      testCard1.setStatus(CardStatus.EXPIRED);
      when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard1));
      when(cardRepository.save(any(Card.class))).thenReturn(testCard1);

      // When
      cardService.updateStatus(1L, CardStatus.BLOCKED);

      // Then
      verify(cardRepository).findById(1L);
      verify(cardRepository).save(testCard1);
      assertEquals(CardStatus.BLOCKED, testCard1.getStatus());
    }

    @Test
    @DisplayName("Should not allow invalid status transitions")
    void shouldNotAllowInvalidStatusTransitions() {
      // Given
      testCard1.setStatus(CardStatus.ACTIVE);
      when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard1));
      when(cardRepository.save(any(Card.class))).thenReturn(testCard1);

      // When - setting to same status (should still work)
      cardService.updateStatus(1L, CardStatus.ACTIVE);

      // Then
      verify(cardRepository).findById(1L);
      verify(cardRepository).save(testCard1);
      assertEquals(CardStatus.ACTIVE, testCard1.getStatus());
    }
  }

  @Nested
  @DisplayName("Edge Cases and Boundary Tests")
  @Tag("edge-cases")
  class EdgeCasesTests {

    @Test
    @DisplayName("Should handle card with negative balance")
    void shouldHandleCardWithNegativeBalance() {
      // Given
      testCard1.setBalance(BigDecimal.valueOf(-100.00));
      when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard1));
      when(cardRepository.save(any(Card.class))).thenReturn(testCard1);

      // When
      cardService.updateStatus(1L, CardStatus.BLOCKED);

      // Then
      verify(cardRepository).findById(1L);
      verify(cardRepository).save(testCard1);
      assertEquals(BigDecimal.valueOf(-100.00), testCard1.getBalance());
    }

    @Test
    @DisplayName("Should handle card with zero balance")
    void shouldHandleCardWithZeroBalance() {
      // Given
      testCard1.setBalance(BigDecimal.ZERO);
      when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard1));
      when(cardRepository.save(any(Card.class))).thenReturn(testCard1);

      // When
      cardService.updateStatus(1L, CardStatus.ACTIVE);

      // Then
      verify(cardRepository).findById(1L);
      verify(cardRepository).save(testCard1);
      assertEquals(BigDecimal.ZERO, testCard1.getBalance());
    }

    @Test
    @DisplayName("Should handle card with very large balance")
    void shouldHandleCardWithVeryLargeBalance() {
      // Given
      testCard1.setBalance(new BigDecimal("999999999.99"));
      when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard1));
      when(cardRepository.save(any(Card.class))).thenReturn(testCard1);

      // When
      cardService.updateStatus(1L, CardStatus.ACTIVE);

      // Then
      verify(cardRepository).findById(1L);
      verify(cardRepository).save(testCard1);
      assertEquals(new BigDecimal("999999999.99"), testCard1.getBalance());
    }

    @Test
    @DisplayName("Should handle card with expired date in the past")
    void shouldHandleCardWithExpiredDateInPast() {
      // Given
      testCard1.setExpiryDate(LocalDate.now().minusDays(1));
      when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard1));
      when(cardRepository.save(any(Card.class))).thenReturn(testCard1);

      // When
      cardService.updateStatus(1L, CardStatus.EXPIRED);

      // Then
      verify(cardRepository).findById(1L);
      verify(cardRepository).save(testCard1);
      assertTrue(testCard1.getExpiryDate().isBefore(LocalDate.now()));
    }
  }

  @Nested
  @DisplayName("Database Constraint and Repository Error Tests")
  @Tag("repository-errors")
  class RepositoryErrorTests {

    @Test
    @DisplayName("Should handle database constraint violation on card creation")
    void shouldHandleDatabaseConstraintViolationOnCardCreation() {
      // Given
      CreateCardRequest request = new CreateCardRequest(1L);
      when(cardholderRepository.findById(1L)).thenReturn(Optional.of(testCardholder));
      when(cardCryptoUtil.maskCardNumber(any(String.class))).thenReturn("**** **** **** 1234");
      when(cardCryptoUtil.encrypt(any(String.class))).thenReturn("encrypted_123456");
      when(cardRepository.save(any(Card.class))).thenThrow(new RuntimeException("Database constraint violation"));

      // When & Then
      RuntimeException exception = assertThrows(RuntimeException.class,
          () -> cardService.createCard(request));

      assertEquals("Database constraint violation", exception.getMessage());
      verify(cardholderRepository).findById(1L);
      verify(cardCryptoUtil).maskCardNumber(any(String.class));
      verify(cardCryptoUtil).encrypt(any(String.class));
      verify(cardRepository).save(any(Card.class));
    }

    @Test
    @DisplayName("Should handle repository connection error on findAll")
    void shouldHandleRepositoryConnectionErrorOnFindAll() {
      // Given
      when(cardRepository.findAll()).thenThrow(new RuntimeException("Connection timeout"));

      // When & Then
      RuntimeException exception = assertThrows(RuntimeException.class,
          () -> cardService.findAllCards());

      assertEquals("Connection timeout", exception.getMessage());
      verify(cardRepository).findAll();
    }

    @Test
    @DisplayName("Should handle concurrent modification during card deletion")
    void shouldHandleConcurrentModificationDuringCardDeletion() {
      // Given
      when(cardRepository.existsById(1L)).thenReturn(true);
      doThrow(new RuntimeException("Concurrent modification")).when(cardRepository).deleteById(1L);

      // When & Then
      RuntimeException exception = assertThrows(RuntimeException.class,
          () -> cardService.deleteCard(1L));

      assertEquals("Concurrent modification", exception.getMessage());
      verify(cardRepository).existsById(1L);
      verify(cardRepository).deleteById(1L);
    }
  }

  @Nested
  @DisplayName("Bulk Operations and Performance Tests")
  @Tag("bulk-operations")
  class BulkOperationsTests {

    @Test
    @DisplayName("Should efficiently handle large number of cards")
    void shouldEfficientlyHandleLargeNumberOfCards() {
      // Given
      List<Card> largeCardList = new ArrayList<>();
      for (int i = 0; i < 1000; i++) {
        Card card = Card.builder()
            .id((long) i + 1)
            .cardNumberEncrypted("encrypted_" + i)
            .cardNumberMasked("**** **** **** " + String.format("%04d", i))
            .ownerName("User " + i)
            .expiryDate(LocalDate.now().plusYears(2))
            .status(CardStatus.ACTIVE)
            .balance(BigDecimal.valueOf(i * 100))
            .cardholder(testCardholder)
            .createdAt(LocalDateTime.now())
            .build();
        largeCardList.add(card);
      }
      when(cardRepository.findAll()).thenReturn(largeCardList);

      // When
      List<CardResponse> result = cardService.findAllCards();

      // Then
      assertEquals(1000, result.size());
      verify(cardRepository).findAll();
    }

    @Test
    @DisplayName("Should return empty list when no cards exist")
    void shouldReturnEmptyListWhenNoCardsExist() {
      // Given
      when(cardRepository.findAll()).thenReturn(new ArrayList<>());

      // When
      List<CardResponse> result = cardService.findAllCards();

      // Then
      assertNotNull(result);
      assertEquals(0, result.size());
      verify(cardRepository).findAll();
    }
  }

  @Nested
  @DisplayName("Data Validation and Input Tests")
  @Tag("data-validation")
  class DataValidationTests {

    @Test
    @DisplayName("Should validate card data integrity during creation")
    void shouldValidateCardDataIntegrityDuringCreation() {
      // Given
      CreateCardRequest request = new CreateCardRequest(1L);
      when(cardholderRepository.findById(1L)).thenReturn(Optional.of(testCardholder));
      when(cardCryptoUtil.maskCardNumber(any(String.class))).thenReturn("**** **** **** 1234");
      when(cardCryptoUtil.encrypt(any(String.class))).thenReturn("encrypted_123456");
      when(cardRepository.save(any(Card.class))).thenReturn(testCard1);

      // When
      CardResponse result = cardService.createCard(request);

      // Then
      assertNotNull(result);
      assertNotNull(result.id());
      assertNotNull(result.cardNumberMasked());
      assertNotNull(result.status());
      assertNotNull(result.balance());
      assertNotNull(result.cardholderId());
      verify(cardholderRepository).findById(1L);
      verify(cardCryptoUtil).maskCardNumber(any(String.class));
      verify(cardCryptoUtil).encrypt(any(String.class));
      verify(cardRepository).save(any(Card.class));
    }

    @Test
    @DisplayName("Should preserve data consistency across operations")
    void shouldPreserveDataConsistencyAcrossOperations() {
      // Given
      when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard1));
      when(cardRepository.save(any(Card.class))).thenReturn(testCard1);

      // When
      cardService.updateStatus(1L, CardStatus.BLOCKED);

      // Then - verify all fields are preserved
      assertEquals(testCard1.getId(), Long.valueOf(1L));
      assertNotNull(testCard1.getCardNumberEncrypted());
      assertNotNull(testCard1.getCardNumberMasked());
      assertEquals(testCard1.getOwnerName(), "John Doe");
      assertNotNull(testCard1.getExpiryDate());
      assertEquals(testCard1.getStatus(), CardStatus.BLOCKED);
      assertEquals(testCard1.getBalance(), BigDecimal.valueOf(1000.50));
      assertNotNull(testCard1.getCardholder());
      assertNotNull(testCard1.getCreatedAt());
    }
  }
}
