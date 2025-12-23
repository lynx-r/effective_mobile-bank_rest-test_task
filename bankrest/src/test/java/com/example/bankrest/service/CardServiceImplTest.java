package com.example.bankrest.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;

import com.example.bankrest.config.CardConfig;
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

  @Mock
  private CardConfig cardConfig;

  @Mock
  private AuditService auditService;

  @InjectMocks
  private CardholderCardServiceImpl cardService;

  private Cardholder testOwner;
  private Card testCard1;
  private Card testCard2;

  @BeforeEach
  void setUp() {
    testOwner = new Cardholder();
    testOwner.setId(1L);
    testOwner.setUsername("user1");
    testOwner.setEmail("user1@example.com");
    testOwner.setFirstName("John");
    testOwner.setLastName("Doe");
    testOwner.setCreatedAt(LocalDateTime.now());

    testCard1 = Card.builder()
        .id(1L)
        .cardNumberEncrypted("encrypted_val_" + UUID.randomUUID())
        .cardNumberMasked("**** **** **** 1234")
        .ownerName("John Doe")
        .expiryDate(LocalDate.now().plusYears(4))
        .status(CardStatus.ACTIVE)
        .balance(BigDecimal.valueOf(1000.50))
        .owner(testOwner)
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
        .owner(testOwner)
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
    when(cardholderRepository.findById(1L)).thenReturn(Optional.of(testOwner));
    when(cardConfig.getBin()).thenReturn("123456");
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
    verify(cardConfig).getBin();
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
    doNothing().when(auditService).logCardStatusChange(anyString(), anyLong(), anyString(), anyString());

    // When
    cardService.updateStatus(1L, CardStatus.BLOCKED);

    // Then
    verify(cardRepository).findById(1L);
    verify(auditService).logCardStatusChange("admin", 1L, "ACTIVE", "BLOCKED");
    assertEquals(CardStatus.BLOCKED, testCard1.getStatus());
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
    when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard1));
    doNothing().when(auditService).logCardDeletion(anyString(), anyLong(), anyString());

    // When
    cardService.deleteCard(1L);

    // Then
    verify(cardRepository).findById(1L);
    verify(cardRepository).deleteById(1L);
  }

  @Test
  void deleteCard_ShouldThrowEntityNotFoundException_WhenCardNotFound() {
    // Given
    when(cardRepository.findById(1L)).thenReturn(Optional.empty());

    // When & Then
    EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
        () -> cardService.deleteCard(1L));

    assertEquals("Карта не найдена", exception.getMessage());
    verify(cardRepository).findById(1L);
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
      doNothing().when(auditService).logCardStatusChange(anyString(), anyLong(), anyString(), anyString());

      // When
      cardService.updateStatus(1L, CardStatus.ACTIVE);

      // Then
      verify(cardRepository).findById(1L);
      verify(auditService).logCardStatusChange("admin", 1L, "BLOCKED", "ACTIVE");
      assertEquals(CardStatus.ACTIVE, testCard1.getStatus());
    }

    @Test
    @DisplayName("Should handle expired card status transition")
    void shouldHandleExpiredCardStatusTransition() {
      // Given
      testCard1.setStatus(CardStatus.EXPIRED);
      when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard1));
      doNothing().when(auditService).logCardStatusChange(anyString(), anyLong(), anyString(), anyString());

      // When
      cardService.updateStatus(1L, CardStatus.BLOCKED);

      // Then
      verify(cardRepository).findById(1L);
      verify(auditService).logCardStatusChange("admin", 1L, "EXPIRED", "BLOCKED");
      assertEquals(CardStatus.BLOCKED, testCard1.getStatus());
    }

    @Test
    @DisplayName("Should not allow invalid status transitions")
    void shouldNotAllowInvalidStatusTransitions() {
      // Given
      testCard1.setStatus(CardStatus.ACTIVE);
      when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard1));
      doNothing().when(auditService).logCardStatusChange(anyString(), anyLong(), anyString(), anyString());

      // When - (should still work setting to same status)
      cardService.updateStatus(1L, CardStatus.ACTIVE);

      // Then
      verify(cardRepository).findById(1L);
      verify(auditService).logCardStatusChange("admin", 1L, "ACTIVE", "ACTIVE");
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
      doNothing().when(auditService).logCardStatusChange(anyString(), anyLong(), anyString(), anyString());
      when(cardRepository.save(any(Card.class))).thenReturn(testCard1);

      // When
      cardService.updateStatus(1L, CardStatus.BLOCKED);

      // Then
      verify(cardRepository).findById(1L);
      assertEquals(BigDecimal.valueOf(-100.00), testCard1.getBalance());
    }

    @Test
    @DisplayName("Should handle card with zero balance")
    void shouldHandleCardWithZeroBalance() {
      // Given
      testCard1.setBalance(BigDecimal.ZERO);
      when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard1));
      doNothing().when(auditService).logCardStatusChange(anyString(), anyLong(), anyString(), anyString());
      when(cardRepository.save(any(Card.class))).thenReturn(testCard1);

      // When
      cardService.updateStatus(1L, CardStatus.ACTIVE);

      // Then
      verify(cardRepository).findById(1L);
      assertEquals(BigDecimal.ZERO, testCard1.getBalance());
    }

    @Test
    @DisplayName("Should handle card with very large balance")
    void shouldHandleCardWithVeryLargeBalance() {
      // Given
      testCard1.setBalance(new BigDecimal("999999999.99"));
      when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard1));
      doNothing().when(auditService).logCardStatusChange(anyString(), anyLong(), anyString(), anyString());
      when(cardRepository.save(any(Card.class))).thenReturn(testCard1);

      // When
      cardService.updateStatus(1L, CardStatus.ACTIVE);

      // Then
      verify(cardRepository).findById(1L);
      assertEquals(new BigDecimal("999999999.99"), testCard1.getBalance());
    }

    @Test
    @DisplayName("Should handle card with expired date in the past")
    void shouldHandleCardWithExpiredDateInPast() {
      // Given
      testCard1.setExpiryDate(LocalDate.now().minusDays(1));
      when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard1));
      doNothing().when(auditService).logCardStatusChange(anyString(), anyLong(), anyString(), anyString());
      when(cardRepository.save(any(Card.class))).thenReturn(testCard1);

      // When
      cardService.updateStatus(1L, CardStatus.EXPIRED);

      // Then
      verify(cardRepository).findById(1L);
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
      when(cardholderRepository.findById(1L)).thenReturn(Optional.of(testOwner));
      when(cardConfig.getBin()).thenReturn("123456");
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
      when(cardRepository.findById(1L)).thenReturn(Optional.of(testCard1));
      doThrow(new RuntimeException("Concurrent modification")).when(cardRepository).deleteById(1L);

      // When & Then
      RuntimeException exception = assertThrows(RuntimeException.class,
          () -> cardService.deleteCard(1L));

      assertEquals("Concurrent modification", exception.getMessage());
      verify(cardRepository).findById(1L);
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
            .owner(testOwner)
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
      when(cardholderRepository.findById(1L)).thenReturn(Optional.of(testOwner));
      when(cardConfig.getBin()).thenReturn("123456");
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
      doNothing().when(auditService).logCardStatusChange(anyString(), anyLong(), anyString(), anyString());
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
      assertNotNull(testCard1.getOwner());
      assertNotNull(testCard1.getCreatedAt());
    }
  }

  @Nested
  @DisplayName("User Card Operations Tests")
  @Tag("user-card-operations")
  class UserCardOperationsTests {

    @Nested
    @DisplayName("Find User Cards Tests")
    class FindUserCardsTests {

      @Test
      @DisplayName("Should return all cards for user without search")
      void shouldReturnAllCardsForUserWithoutSearch() {
        // Given
        List<Card> userCards = List.of(testCard1, testCard2);
        Page<Card> cardPage = new PageImpl<>(userCards, PageRequest.of(0, userCards.size()), userCards.size());
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());

        when(cardRepository.findByOwner_Username(eq("user1"), any(Pageable.class))).thenReturn(cardPage);

        // When
        Page<CardResponse> result = cardService.findCardholderCards("user1", null, pageable);

        // Then
        assertEquals(2, result.getContent().size());
        assertEquals("**** **** **** 1234", result.getContent().get(0).cardNumberMasked());
        assertEquals("**** **** **** 5678", result.getContent().get(1).cardNumberMasked());
        verify(cardRepository).findByOwner_Username(eq("user1"), any(Pageable.class));
      }

      @Test
      @DisplayName("Should return filtered cards with search query")
      void shouldReturnFilteredCardsWithSearchQuery() {
        // Given
        List<Card> filteredCards = List.of(testCard1);
        Page<Card> cardPage = new PageImpl<>(filteredCards, PageRequest.of(0, 10), 0);
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());

        when(cardRepository.findByUserWithFilter("user1", "1234", pageable)).thenReturn(cardPage);

        // When
        Page<CardResponse> result = cardService.findCardholderCards("user1", "1234", pageable);

        // Then
        assertEquals(1, result.getContent().size());
        assertEquals("**** **** **** 1234", result.getContent().get(0).cardNumberMasked());
        verify(cardRepository).findByUserWithFilter("user1", "1234", pageable);
      }

      @Test
      @DisplayName("Should handle empty search string")
      void shouldHandleEmptySearchString() {
        // Given
        List<Card> userCards = List.of(testCard1, testCard2);
        Page<Card> cardPage = new PageImpl<>(userCards, PageRequest.of(0, userCards.size()), userCards.size());
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());

        when(cardRepository.findByOwner_Username(eq("user1"), any(Pageable.class))).thenReturn(cardPage);

        // When
        Page<CardResponse> result = cardService.findCardholderCards("user1", "", pageable);

        // Then
        assertEquals(2, result.getContent().size());
        verify(cardRepository).findByOwner_Username(eq("user1"), any(Pageable.class));
        verify(cardRepository, never()).findByUserWithFilter(anyString(), anyString(), any(Pageable.class));
      }

      @Test
      @DisplayName("Should handle whitespace search string")
      void shouldHandleWhitespaceSearchString() {
        // Given
        List<Card> userCards = List.of(testCard1, testCard2);
        Page<Card> cardPage = new PageImpl<>(userCards, PageRequest.of(0, userCards.size()), userCards.size());
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());

        when(cardRepository.findByOwner_Username(eq("user1"), any(Pageable.class))).thenReturn(cardPage);

        // When
        Page<CardResponse> result = cardService.findCardholderCards("user1", "   ", pageable);

        // Then
        assertEquals(2, result.getContent().size());
        verify(cardRepository).findByOwner_Username(eq("user1"), any(Pageable.class));
        verify(cardRepository, never()).findByUserWithFilter(anyString(), anyString(), any(Pageable.class));
      }

      @Test
      @DisplayName("Should apply default sorting when sort is unsorted")
      void shouldApplyDefaultSortingWhenSortIsUnsorted() {
        // Given
        List<Card> userCards = List.of(testCard1);
        Page<Card> cardPage = new PageImpl<>(userCards, PageRequest.of(0, userCards.size()), userCards.size());
        Pageable unsortedPageable = PageRequest.of(0, 10);
        Pageable expectedPageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());

        when(cardRepository.findByOwner_Username("user1", expectedPageable)).thenReturn(cardPage);

        // When
        Page<CardResponse> result = cardService.findCardholderCards("user1", null, unsortedPageable);

        // Then
        assertEquals(1, result.getContent().size());
        verify(cardRepository).findByOwner_Username("user1", expectedPageable);
      }

      @Test
      @DisplayName("Should preserve custom sorting when sort is provided")
      void shouldPreserveCustomSortingWhenSortIsProvided() {
        // Given
        List<Card> userCards = List.of(testCard1);
        Page<Card> cardPage = new PageImpl<>(userCards, PageRequest.of(0, userCards.size()), userCards.size());
        Pageable customSortedPageable = PageRequest.of(0, 10, Sort.by("balance").ascending());

        when(cardRepository.findByOwner_Username("user1", customSortedPageable)).thenReturn(cardPage);

        // When
        Page<CardResponse> result = cardService.findCardholderCards("user1", null, customSortedPageable);

        // Then
        assertEquals(1, result.getContent().size());
        verify(cardRepository).findByOwner_Username("user1", customSortedPageable);
      }

      @Test
      @DisplayName("Should return empty page when user has no cards")
      void shouldReturnEmptyPageWhenUserHasNoCards() {
        // Given
        Page<Card> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        Pageable pageable = PageRequest.of(0, 10);

        when(cardRepository.findByOwner_Username(eq("user1"), any(Pageable.class))).thenReturn(emptyPage);

        // When
        Page<CardResponse> result = cardService.findCardholderCards("user1", null, pageable);

        // Then
        assertEquals(0, result.getContent().size());
        assertTrue(result.isEmpty());
        verify(cardRepository).findByOwner_Username(eq("user1"), any(Pageable.class));
      }

      @Test
      @DisplayName("Should handle pagination correctly")
      void shouldHandlePaginationCorrectly() {
        // Given
        List<Card> pageContent = List.of(testCard1);
        Page<Card> cardPage = new PageImpl<>(pageContent,
            PageRequest.of(1, 5, Sort.by("createdAt").descending()), 11);

        when(cardRepository.findByOwner_Username("user1",
            PageRequest.of(1, 5, Sort.by("createdAt").descending()))).thenReturn(cardPage);

        // When
        Page<CardResponse> result = cardService.findCardholderCards("user1", null,
            PageRequest.of(1, 5, Sort.by("createdAt").descending()));

        // Then
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getNumber()); // page index
        assertEquals(5, result.getSize()); // page size
        assertEquals(3, result.getTotalPages()); // 11 items / 5 per page = 3 pages
        assertEquals(11, result.getTotalElements());
      }
    }

    @Nested
    @DisplayName("Block Own Card Tests")
    class BlockOwnCardTests {

      @Test
      @DisplayName("Should successfully block own card")
      void shouldSuccessfullyBlockOwnCard() {
        // Given
        when(cardRepository.findByIdAndOwner_Username(1L, "user1")).thenReturn(Optional.of(testCard1));

        // When
        cardService.blockOwnCard("user1", 1L);

        // Then
        assertEquals(CardStatus.BLOCKED, testCard1.getStatus());
        verify(cardRepository).findByIdAndOwner_Username(1L, "user1");
      }

      @Test
      @DisplayName("Should throw AccessDeniedException when card not found")
      void shouldThrowAccessDeniedExceptionWhenCardNotFound() {
        // Given
        when(cardRepository.findByIdAndOwner_Username(1L, "user1")).thenReturn(Optional.empty());

        // When & Then
        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
            () -> cardService.blockOwnCard("user1", 1L));

        assertEquals("Карта не найдена или не принадлежит вам", exception.getMessage());
        verify(cardRepository).findByIdAndOwner_Username(1L, "user1");
        verify(cardRepository, never()).save(any());
      }

      @Test
      @DisplayName("Should throw AccessDeniedException when trying to block another user's card")
      void shouldThrowAccessDeniedExceptionWhenTryingToBlockAnotherUsersCard() {
        // Given
        Card otherUserCard = Card.builder()
            .id(2L)
            .cardNumberEncrypted("encrypted_other")
            .cardNumberMasked("**** **** **** 9999")
            .ownerName("Other User")
            .expiryDate(LocalDate.now().plusYears(3))
            .status(CardStatus.ACTIVE)
            .balance(BigDecimal.valueOf(500.00))
            .owner(createOtherUser())
            .createdAt(LocalDateTime.now())
            .build();

        when(cardRepository.findByIdAndOwner_Username(2L, "user1")).thenReturn(Optional.empty());

        // When & Then
        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
            () -> cardService.blockOwnCard("user1", 2L));

        assertEquals("Карта не найдена или не принадлежит вам", exception.getMessage());
        verify(cardRepository).findByIdAndOwner_Username(2L, "user1");
        verify(cardRepository, never()).save(any());
      }

      @Test
      @DisplayName("Should handle blocking already blocked card")
      void shouldHandleBlockingAlreadyBlockedCard() {
        // Given
        testCard1.setStatus(CardStatus.BLOCKED);
        when(cardRepository.findByIdAndOwner_Username(1L, "user1")).thenReturn(Optional.of(testCard1));

        // When
        cardService.blockOwnCard("user1", 1L);

        // Then
        assertEquals(CardStatus.BLOCKED, testCard1.getStatus());
        verify(cardRepository).findByIdAndOwner_Username(1L, "user1");
      }

      @Test
      @DisplayName("Should preserve other card fields when blocking")
      void shouldPreserveOtherCardFieldsWhenBlocking() {
        // Given
        when(cardRepository.findByIdAndOwner_Username(1L, "user1")).thenReturn(Optional.of(testCard1));

        // When
        cardService.blockOwnCard("user1", 1L);

        // Then - verify all other fields are preserved
        assertEquals(Long.valueOf(1L), testCard1.getId());
        assertEquals("**** **** **** 1234", testCard1.getCardNumberMasked());
        assertEquals("John Doe", testCard1.getOwnerName());
        assertEquals(BigDecimal.valueOf(1000.50), testCard1.getBalance());
        assertEquals(testOwner, testCard1.getOwner());
      }
    }

    @Nested
    @DisplayName("Get User Card Balance Tests")
    class GetUserCardBalanceTests {

      @Test
      @DisplayName("Should return balance for own card")
      void shouldReturnBalanceForOwnCard() {
        // Given
        when(cardRepository.findByIdAndOwner_Username(1L, "user1")).thenReturn(Optional.of(testCard1));

        // When
        BigDecimal result = cardService.getCardholderCardBalance("user1", 1L);

        // Then
        assertEquals(BigDecimal.valueOf(1000.50), result);
        verify(cardRepository).findByIdAndOwner_Username(1L, "user1");
      }

      @Test
      @DisplayName("Should return zero balance")
      void shouldReturnZeroBalance() {
        // Given
        testCard1.setBalance(BigDecimal.ZERO);
        when(cardRepository.findByIdAndOwner_Username(1L, "user1")).thenReturn(Optional.of(testCard1));

        // When
        BigDecimal result = cardService.getCardholderCardBalance("user1", 1L);

        // Then
        assertEquals(BigDecimal.ZERO, result);
        verify(cardRepository).findByIdAndOwner_Username(1L, "user1");
      }

      @Test
      @DisplayName("Should return negative balance")
      void shouldReturnNegativeBalance() {
        // Given
        testCard1.setBalance(BigDecimal.valueOf(-250.75));
        when(cardRepository.findByIdAndOwner_Username(1L, "user1")).thenReturn(Optional.of(testCard1));

        // When
        BigDecimal result = cardService.getCardholderCardBalance("user1", 1L);

        // Then
        assertEquals(BigDecimal.valueOf(-250.75), result);
        verify(cardRepository).findByIdAndOwner_Username(1L, "user1");
      }

      @Test
      @DisplayName("Should return large balance")
      void shouldReturnLargeBalance() {
        // Given
        testCard1.setBalance(new BigDecimal("999999999.99"));
        when(cardRepository.findByIdAndOwner_Username(1L, "user1")).thenReturn(Optional.of(testCard1));

        // When
        BigDecimal result = cardService.getCardholderCardBalance("user1", 1L);

        // Then
        assertEquals(new BigDecimal("999999999.99"), result);
        verify(cardRepository).findByIdAndOwner_Username(1L, "user1");
      }

      @Test
      @DisplayName("Should throw AccessDeniedException for non-existent card")
      void shouldThrowAccessDeniedExceptionForNonExistentCard() {
        // Given
        when(cardRepository.findByIdAndOwner_Username(1L, "user1")).thenReturn(Optional.empty());

        // When & Then
        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
            () -> cardService.getCardholderCardBalance("user1", 1L));

        assertEquals("Доступ запрещен", exception.getMessage());
        verify(cardRepository).findByIdAndOwner_Username(1L, "user1");
      }

      @Test
      @DisplayName("Should throw AccessDeniedException for another user's card")
      void shouldThrowAccessDeniedExceptionForAnotherUsersCard() {
        // Given
        Card otherUserCard = Card.builder()
            .id(2L)
            .cardNumberEncrypted("encrypted_other")
            .cardNumberMasked("**** **** **** 9999")
            .ownerName("Other User")
            .expiryDate(LocalDate.now().plusYears(3))
            .status(CardStatus.ACTIVE)
            .balance(BigDecimal.valueOf(500.00))
            .owner(createOtherUser())
            .createdAt(LocalDateTime.now())
            .build();

        when(cardRepository.findByIdAndOwner_Username(2L, "user1")).thenReturn(Optional.empty());

        // When & Then
        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
            () -> cardService.getCardholderCardBalance("user1", 2L));

        assertEquals("Доступ запрещен", exception.getMessage());
        verify(cardRepository).findByIdAndOwner_Username(2L, "user1");
      }

      @Test
      @DisplayName("Should handle card with different status")
      void shouldHandleCardWithDifferentStatus() {
        // Given
        testCard1.setStatus(CardStatus.EXPIRED);
        when(cardRepository.findByIdAndOwner_Username(1L, "user1")).thenReturn(Optional.of(testCard1));

        // When
        BigDecimal result = cardService.getCardholderCardBalance("user1", 1L);

        // Then
        assertEquals(BigDecimal.valueOf(1000.50), result);
        assertEquals(CardStatus.EXPIRED, testCard1.getStatus());
        verify(cardRepository).findByIdAndOwner_Username(1L, "user1");
      }
    }
  }

  @Nested
  @DisplayName("Edge Cases and Error Handling Tests")
  @Tag("edge-cases-error-handling")
  class EdgeCasesAndErrorHandlingTests {

    @Test
    @DisplayName("Should handle null search parameter in findUserCards")
    void shouldHandleNullSearchParameterInFindUserCards() {
      // Given
      List<Card> userCards = List.of(testCard1);
      Page<Card> cardPage = new PageImpl<>(userCards, PageRequest.of(0, userCards.size()), userCards.size());
      Pageable pageable = PageRequest.of(0, 10);

      when(cardRepository.findByOwner_Username(eq("user1"), any(Pageable.class))).thenReturn(cardPage);

      // When
      Page<CardResponse> result = cardService.findCardholderCards("user1", null, pageable);

      // Then
      assertEquals(1, result.getContent().size());
      verify(cardRepository).findByOwner_Username(eq("user1"), any(Pageable.class));
    }

    @Test
    @DisplayName("Should handle very long search string")
    void shouldHandleVeryLongSearchString() {
      // Given
      String longSearch = "a".repeat(1000);
      List<Card> filteredCards = List.of();
      Page<Card> cardPage = new PageImpl<>(filteredCards, PageRequest.of(0, 10), 0);
      Pageable pageable = PageRequest.of(0, 10);

      when(cardRepository.findByUserWithFilter(eq("user1"), anyString(), any(Pageable.class))).thenReturn(cardPage);

      // When
      Page<CardResponse> result = cardService.findCardholderCards("user1", longSearch, pageable);

      // Then
      assertEquals(0, result.getContent().size());
      verify(cardRepository).findByUserWithFilter(eq("user1"), anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("Should handle special characters in search")
    void shouldHandleSpecialCharactersInSearch() {
      // Given
      String specialSearch = "!@#$%^&*()_+-={}[]|\\:;\"'<>,.?/";
      List<Card> filteredCards = List.of(testCard1);
      Page<Card> cardPage = new PageImpl<>(filteredCards, PageRequest.of(0, 10), 0);
      Pageable pageable = PageRequest.of(0, 10);

      when(cardRepository.findByUserWithFilter(eq("user1"), anyString(), any(Pageable.class))).thenReturn(cardPage);

      // When
      Page<CardResponse> result = cardService.findCardholderCards("user1", specialSearch, pageable);

      // Then
      assertEquals(1, result.getContent().size());
      verify(cardRepository).findByUserWithFilter(eq("user1"), anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("Should handle negative card ID in blockOwnCard")
    void shouldHandleNegativeCardIdInBlockOwnCard() {
      // Given
      when(cardRepository.findByIdAndOwner_Username(-1L, "user1")).thenReturn(Optional.empty());

      // When & Then
      AccessDeniedException exception = assertThrows(AccessDeniedException.class,
          () -> cardService.blockOwnCard("user1", -1L));

      assertEquals("Карта не найдена или не принадлежит вам", exception.getMessage());
      verify(cardRepository).findByIdAndOwner_Username(-1L, "user1");
    }

    @Test
    @DisplayName("Should handle negative card ID in getUserCardBalance")
    void shouldHandleNegativeCardIdInGetUserCardBalance() {
      // Given
      when(cardRepository.findByIdAndOwner_Username(-1L, "user1")).thenReturn(Optional.empty());

      // When & Then
      AccessDeniedException exception = assertThrows(AccessDeniedException.class,
          () -> cardService.getCardholderCardBalance("user1", -1L));

      assertEquals("Доступ запрещен", exception.getMessage());
      verify(cardRepository).findByIdAndOwner_Username(-1L, "user1");
    }

    @Test
    @DisplayName("Should handle empty username")
    void shouldHandleEmptyUsername() {
      // Given
      when(cardRepository.findByIdAndOwner_Username(1L, "")).thenReturn(Optional.empty());

      // When & Then
      AccessDeniedException exception = assertThrows(AccessDeniedException.class,
          () -> cardService.blockOwnCard("", 1L));

      assertEquals("Карта не найдена или не принадлежит вам", exception.getMessage());
      verify(cardRepository).findByIdAndOwner_Username(1L, "");
    }

    @Test
    @DisplayName("Should handle null username")
    void shouldHandleNullUsername() {
      // Given
      when(cardRepository.findByIdAndOwner_Username(1L, null)).thenReturn(Optional.empty());

      // When & Then
      AccessDeniedException exception = assertThrows(AccessDeniedException.class,
          () -> cardService.blockOwnCard(null, 1L));

      assertEquals("Карта не найдена или не принадлежит вам", exception.getMessage());
      verify(cardRepository).findByIdAndOwner_Username(1L, null);
    }
  }

  @Nested
  @DisplayName("Security and Access Control Tests")
  @Tag("security-access-control")
  class SecurityAndAccessControlTests {

    @Test
    @DisplayName("Should prevent access to another user's cards in findUserCards")
    void shouldPreventAccessToAnotherUsersCardsInFindUserCards() {
      // Given
      List<Card> user1Cards = List.of(testCard1);
      Page<Card> cardPage = new PageImpl<>(user1Cards, PageRequest.of(0, user1Cards.size()), user1Cards.size());
      Pageable pageable = PageRequest.of(0, 10);

      when(cardRepository.findByOwner_Username(eq("user2"), any(Pageable.class))).thenReturn(cardPage);

      // When
      Page<CardResponse> result = cardService.findCardholderCards("user2", null, pageable);

      // Then - should only return user2's cards, not user1's
      assertEquals(1, result.getContent().size());
      assertEquals("**** **** **** 1234", result.getContent().get(0).cardNumberMasked());
      verify(cardRepository).findByOwner_Username(eq("user2"), any(Pageable.class));
    }

    @Test
    @DisplayName("Should prevent cross-user card access in blockOwnCard")
    void shouldPreventCrossUserCardAccessInBlockOwnCard() {
      // Given
      Card user2Card = createUser2Card();
      when(cardRepository.findByIdAndOwner_Username(2L, "user1")).thenReturn(Optional.empty());

      // When & Then
      AccessDeniedException exception = assertThrows(AccessDeniedException.class,
          () -> cardService.blockOwnCard("user1", 2L));

      assertEquals("Карта не найдена или не принадлежит вам", exception.getMessage());
      verify(cardRepository).findByIdAndOwner_Username(2L, "user1");
    }

    @Test
    @DisplayName("Should prevent cross-user balance access")
    void shouldPreventCrossUserBalanceAccess() {
      // Given
      Card user2Card = createUser2Card();
      when(cardRepository.findByIdAndOwner_Username(2L, "user1")).thenReturn(Optional.empty());

      // When & Then
      AccessDeniedException exception = assertThrows(AccessDeniedException.class,
          () -> cardService.getCardholderCardBalance("user1", 2L));

      assertEquals("Доступ запрещен", exception.getMessage());
      verify(cardRepository).findByIdAndOwner_Username(2L, "user1");
    }
  }

  @Nested
  @DisplayName("Performance and Bulk Operations Tests")
  @Tag("performance-bulk-operations")
  class PerformanceAndBulkOperationsTests {

    @Test
    @DisplayName("Should efficiently handle large result set in findUserCards")
    void shouldEfficientlyHandleLargeResultSetInFindUserCards() {
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
            .owner(testOwner)
            .createdAt(LocalDateTime.now())
            .build();
        largeCardList.add(card);
      }

      Page<Card> cardPage = new PageImpl<>(largeCardList.subList(0, 100),
          PageRequest.of(0, 100, Sort.by("createdAt").descending()), 1000);

      when(cardRepository.findByOwner_Username("user1",
          PageRequest.of(0, 100, Sort.by("createdAt").descending()))).thenReturn(cardPage);

      // When
      Page<CardResponse> result = cardService.findCardholderCards("user1", null,
          PageRequest.of(0, 100, Sort.by("createdAt").descending()));

      // Then
      assertEquals(100, result.getContent().size());
      assertEquals(10, result.getTotalPages());
      assertEquals(1000, result.getTotalElements());
      verify(cardRepository).findByOwner_Username("user1",
          PageRequest.of(0, 100, Sort.by("createdAt").descending()));
    }

    @Test
    @DisplayName("Should handle multiple page requests efficiently")
    void shouldHandleMultiplePageRequestsEfficiently() {
      // Given
      List<Card> page1Cards = List.of(testCard1);
      List<Card> page2Cards = List.of(testCard2);

      Page<Card> page1 = new PageImpl<>(page1Cards, PageRequest.of(0, 1, Sort.by("createdAt").descending()), 2);
      Page<Card> page2 = new PageImpl<>(page2Cards, PageRequest.of(1, 1, Sort.by("createdAt").descending()), 2);

      when(cardRepository.findByOwner_Username("user1",
          PageRequest.of(0, 1, Sort.by("createdAt").descending()))).thenReturn(page1);
      when(cardRepository.findByOwner_Username("user1",
          PageRequest.of(1, 1, Sort.by("createdAt").descending()))).thenReturn(page2);

      // When
      Page<CardResponse> result1 = cardService.findCardholderCards("user1", null,
          PageRequest.of(0, 1, Sort.by("createdAt").descending()));
      Page<CardResponse> result2 = cardService.findCardholderCards("user1", null,
          PageRequest.of(1, 1, Sort.by("createdAt").descending()));

      // Then
      assertEquals(1, result1.getContent().size());
      assertEquals(1, result2.getContent().size());
      assertEquals("**** **** **** 1234", result1.getContent().get(0).cardNumberMasked());
      assertEquals("**** **** **** 5678", result2.getContent().get(0).cardNumberMasked());
    }
  }

  // Helper methods for creating test data
  private Cardholder createOtherUser() {
    Cardholder otherUser = new Cardholder();
    otherUser.setId(2L);
    otherUser.setUsername("user2");
    otherUser.setEmail("user2@example.com");
    otherUser.setFirstName("Jane");
    otherUser.setLastName("Smith");
    otherUser.setCreatedAt(LocalDateTime.now());
    return otherUser;
  }

  private Card createUser2Card() {
    return Card.builder()
        .id(2L)
        .cardNumberEncrypted("encrypted_user2")
        .cardNumberMasked("**** **** **** 9999")
        .ownerName("Jane Smith")
        .expiryDate(LocalDate.now().plusYears(3))
        .status(CardStatus.ACTIVE)
        .balance(BigDecimal.valueOf(500.00))
        .owner(createOtherUser())
        .createdAt(LocalDateTime.now())
        .build();
  }
}
