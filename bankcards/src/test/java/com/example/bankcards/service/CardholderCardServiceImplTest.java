package com.example.bankcards.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Cardholder;
import com.example.bankcards.repository.CardRepository;

/**
 * Тесты для CardholderCardServiceImpl
 * 
 * Тестирует бизнес-логику управления картами держателя:
 * - Поиск карт пользователя с пагинацией и фильтрацией
 * - Блокировка собственной карты пользователем
 * - Получение баланса карты
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты CardholderCardServiceImpl")
class CardholderCardServiceImplTest {

  @Mock
  private CardRepository cardRepository;

  @Mock
  private AuditService auditService;

  @Mock
  private AuthenticationFacade authenticationFacade;

  @InjectMocks
  private CardholderCardServiceImpl cardholderCardService;

  private Cardholder testCardholder;
  private Card activeCard;
  private Card blockedCard;
  private Pageable pageable;

  @BeforeEach
  void setUp() {
    // Настройка тестовых данных
    testCardholder = Cardholder.builder()
        .id(1L)
        .username("testuser")
        .email("test@example.com")
        .firstName("Тест")
        .lastName("Пользователь")
        .enabled(true)
        .build();

    activeCard = Card.builder()
        .id(1L)
        .cardNumberMasked("**** **** **** 1234")
        .ownerName("Test User")
        .expiryDate(LocalDate.now().plusYears(3))
        .status(CardStatus.ACTIVE)
        .balance(new BigDecimal("1000.00"))
        .isBlockRequested(false)
        .blockRequestedAt(null)
        .owner(testCardholder)
        .build();

    blockedCard = Card.builder()
        .id(2L)
        .cardNumberMasked("**** **** **** 4321")
        .ownerName("Test User")
        .expiryDate(LocalDate.now().plusYears(2))
        .status(CardStatus.BLOCKED)
        .balance(new BigDecimal("500.00"))
        .isBlockRequested(true)
        .blockRequestedAt(LocalDateTime.now().minusDays(1))
        .owner(testCardholder)
        .build();

    pageable = PageRequest.of(0, 10);
  }

  @Test
  @DisplayName("Успешный поиск карт пользователя без фильтра")
  void findCardholderCards_WithoutFilter_ReturnsPage() {
    // Arrange
    Page<Card> cardPage = new PageImpl<>(java.util.Collections.singletonList(activeCard));
    when(authenticationFacade.getAuthenticationName()).thenReturn("testuser");
    when(cardRepository.findByOwner_Username(eq("testuser"), any(Pageable.class))).thenReturn(cardPage);

    // Act
    Page<CardResponse> result = cardholderCardService.findCardholderCards(null, pageable);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getContent().size());
    assertEquals("**** **** **** 1234", result.getContent().get(0).cardNumberMasked());
    assertEquals(CardStatus.ACTIVE, result.getContent().get(0).status());

    verify(cardRepository).findByOwner_Username(eq("testuser"), any(Pageable.class));
    verify(auditService).logCardsListView(10, "findByOwner_Username");
  }

  @Test
  @DisplayName("Успешный поиск карт пользователя с фильтром")
  void findCardholderCards_WithFilter_ReturnsPage() {
    // Arrange
    Page<Card> cardPage = new PageImpl<>(java.util.Collections.singletonList(activeCard));
    when(authenticationFacade.getAuthenticationName()).thenReturn("testuser");
    when(cardRepository.findByOwner_UsernameAndCardNumberMasked(eq("testuser"), eq("1234"), any(Pageable.class)))
        .thenReturn(cardPage);

    // Act
    Page<CardResponse> result = cardholderCardService.findCardholderCards("1234", pageable);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getContent().size());
    assertEquals("**** **** **** 1234", result.getContent().get(0).cardNumberMasked());

    verify(cardRepository).findByOwner_UsernameAndCardNumberMasked(eq("testuser"), eq("1234"), any(Pageable.class));
    verify(auditService).logCardsListView(10, "findByUserWithFilter");
  }

  @Test
  @DisplayName("Поиск карт с пустым результатом")
  void findCardholderCards_NoResults_ReturnsEmptyPage() {
    // Arrange
    Page<Card> emptyPage = new PageImpl<>(java.util.Collections.emptyList());
    when(authenticationFacade.getAuthenticationName()).thenReturn("testuser");
    when(cardRepository.findByOwner_Username(eq("testuser"), any(Pageable.class))).thenReturn(emptyPage);

    // Act
    Page<CardResponse> result = cardholderCardService.findCardholderCards("", pageable);

    // Assert
    assertNotNull(result);
    assertTrue(result.getContent().isEmpty());
    assertEquals(0, result.getTotalElements());

    verify(cardRepository).findByOwner_Username(eq("testuser"), any(Pageable.class));
  }

  @Test
  @DisplayName("Успешная блокировка активной карты")
  void blockOwnCard_ActiveCard_Success() {
    // Arrange
    when(authenticationFacade.getAuthenticationName()).thenReturn("testuser");
    when(cardRepository.findByIdAndOwner_Username(1L, "testuser")).thenReturn(java.util.Optional.of(activeCard));

    // Act
    assertDoesNotThrow(() -> cardholderCardService.requestBlockCard(1L));

    // Assert
    verify(cardRepository).findByIdAndOwner_Username(1L, "testuser");
    verify(cardRepository).save(activeCard);
    verify(auditService).logCardBlocking(1L, "**** **** **** 1234");

    // Проверяем, что карта помечена как заблокированная пользователем
    assertTrue(activeCard.getIsBlockRequested());
    assertNotNull(activeCard.getBlockRequestedAt());
  }

  @Test
  @DisplayName("Попытка блокировки уже заблокированной карты")
  void blockOwnCard_AlreadyBlocked_SkipsBlocking() {
    // Arrange
    when(authenticationFacade.getAuthenticationName()).thenReturn("testuser");
    when(cardRepository.findByIdAndOwner_Username(2L, "testuser")).thenReturn(java.util.Optional.of(blockedCard));

    // Act
    assertDoesNotThrow(() -> cardholderCardService.requestBlockCard(2L));

    // Assert
    verify(cardRepository).findByIdAndOwner_Username(2L, "testuser");
    verify(cardRepository, never()).save(any());
    verify(auditService, never()).logCardBlocking(anyLong(), anyString());
  }

  @Test
  @DisplayName("Попытка блокировки чужой карты")
  void blockOwnCard_ForeignCard_ThrowsAccessDeniedException() {
    // Arrange
    when(authenticationFacade.getAuthenticationName()).thenReturn("testuser");
    when(cardRepository.findByIdAndOwner_Username(1L, "testuser")).thenReturn(java.util.Optional.empty());

    // Act & Assert
    AccessDeniedException exception = assertThrows(AccessDeniedException.class,
        () -> cardholderCardService.requestBlockCard(1L));

    assertEquals("Карта не найдена или не принадлежит вам", exception.getMessage());

    verify(cardRepository).findByIdAndOwner_Username(1L, "testuser");
    verify(cardRepository, never()).save(any());
    verify(auditService, never()).logCardBlocking(anyLong(), anyString());
  }

  @Test
  @DisplayName("Успешное получение баланса карты")
  void getCardholderCardBalance_ValidCard_ReturnsBalance() {
    // Arrange
    when(authenticationFacade.getAuthenticationName()).thenReturn("testuser");
    when(cardRepository.findByIdAndOwner_Username(1L, "testuser")).thenReturn(java.util.Optional.of(activeCard));

    // Act
    BigDecimal result = cardholderCardService.getCardholderCardBalance(1L);

    // Assert
    assertNotNull(result);
    assertEquals(new BigDecimal("1000.00"), result);

    verify(cardRepository).findByIdAndOwner_Username(1L, "testuser");
    verify(auditService).logBalanceView(1L, "**** **** **** 1234", new BigDecimal("1000.00"));
  }

  @Test
  @DisplayName("Получение баланса несуществующей карты")
  void getCardholderCardBalance_InvalidCard_ThrowsAccessDeniedException() {
    // Arrange
    when(authenticationFacade.getAuthenticationName()).thenReturn("testuser");
    when(cardRepository.findByIdAndOwner_Username(1L, "testuser")).thenReturn(java.util.Optional.empty());

    // Act & Assert
    AccessDeniedException exception = assertThrows(AccessDeniedException.class,
        () -> cardholderCardService.getCardholderCardBalance(1L));

    assertEquals("Доступ запрещен", exception.getMessage());

    verify(cardRepository).findByIdAndOwner_Username(1L, "testuser");
    verify(auditService, never()).logBalanceView(anyLong(), anyString(), any());
  }

  @Test
  @DisplayName("Получение баланса заблокированной карты")
  void getCardholderCardBalance_BlockedCard_ReturnsBalance() {
    // Arrange
    when(authenticationFacade.getAuthenticationName()).thenReturn("testuser");
    when(cardRepository.findByIdAndOwner_Username(2L, "testuser")).thenReturn(java.util.Optional.of(blockedCard));

    // Act
    BigDecimal result = cardholderCardService.getCardholderCardBalance(2L);

    // Assert
    assertNotNull(result);
    assertEquals(new BigDecimal("500.00"), result);

    verify(cardRepository).findByIdAndOwner_Username(2L, "testuser");
    verify(auditService).logBalanceView(2L, "**** **** **** 4321", new BigDecimal("500.00"));
  }
}
