package com.example.bankcards.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.InternalTransferRequest;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.service.CardholderCardService;
import com.example.bankcards.service.TransactionService;

/**
 * Unit тесты для CardholderCardController
 * 
 * Тестирует REST API для операций держателей карт:
 * - Просмотр своих карт с поиском и пагинацией
 * - Запрос блокировки карты
 * - Переводы между своими картами
 * - Просмотр баланса карты
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты CardholderCardController")
class CardholderCardControllerTest {

  @Mock
  private CardholderCardService cardService;

  @Mock
  private TransactionService transactionService;

  @InjectMocks
  private CardholderCardController cardholderCardController;

  private CardResponse testCardResponse;
  private InternalTransferRequest transferRequest;
  private Pageable pageable;

  @BeforeEach
  void setUp() {
    // Настройка тестовых данных
    testCardResponse = new CardResponse(
        1L,
        "Тест Пользователь",
        "**** **** **** 1234",
        CardStatus.ACTIVE,
        BigDecimal.valueOf(1000.50),
        false,
        null,
        1L);

    transferRequest = new InternalTransferRequest(1L, 2L, BigDecimal.valueOf(100.0));
    pageable = PageRequest.of(0, 10);
  }

  // ==================== ТЕСТЫ УСПЕШНОГО ДОСТУПА ====================

  @Test
  @DisplayName("Успешный просмотр своих карт без поиска")
  void getCardholderCards_WithoutSearch_ReturnsPage() {
    // Arrange
    Page<CardResponse> cardPage = new PageImpl<>(Collections.singletonList(testCardResponse));
    when(cardService.findCardholderCards(eq(null), any(Pageable.class))).thenReturn(cardPage);

    // Act
    ResponseEntity<Page<CardResponse>> response = cardholderCardController.getCardholderCards(null, pageable);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().getContent().size());
    assertEquals("**** **** **** 1234", response.getBody().getContent().get(0).cardNumberMasked());
    assertEquals(CardStatus.ACTIVE, response.getBody().getContent().get(0).status());
    assertEquals(BigDecimal.valueOf(1000.50), response.getBody().getContent().get(0).balance());

    verify(cardService).findCardholderCards(eq(null), any(Pageable.class));
  }

  @Test
  @DisplayName("Успешный просмотр своих карт с поиском")
  void getCardholderCards_WithSearch_ReturnsFilteredPage() {
    // Arrange
    Page<CardResponse> cardPage = new PageImpl<>(Collections.singletonList(testCardResponse));
    when(cardService.findCardholderCards(eq("1234"), any(Pageable.class))).thenReturn(cardPage);

    // Act
    ResponseEntity<Page<CardResponse>> response = cardholderCardController.getCardholderCards("1234", pageable);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().getContent().size());
    assertEquals("**** **** **** 1234", response.getBody().getContent().get(0).cardNumberMasked());

    verify(cardService).findCardholderCards(eq("1234"), any(Pageable.class));
  }

  @Test
  @DisplayName("Успешный просмотр своих карт с пустым поиском")
  void getCardholderCards_WithEmptySearch_ReturnsAllCards() {
    // Arrange
    Page<CardResponse> cardPage = new PageImpl<>(Collections.singletonList(testCardResponse));
    when(cardService.findCardholderCards(eq(""), any(Pageable.class))).thenReturn(cardPage);

    // Act
    ResponseEntity<Page<CardResponse>> response = cardholderCardController.getCardholderCards("", pageable);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().getContent().size());

    verify(cardService).findCardholderCards(eq(""), any(Pageable.class));
  }

  @Test
  @DisplayName("Успешный запрос блокировки карты")
  void requestBlockCard_ValidCardId_ReturnsNoContent() {
    // Arrange
    doNothing().when(cardService).requestBlockCard(anyLong());

    // Act
    ResponseEntity<Void> response = cardholderCardController.requestBlockCard(1L);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

    verify(cardService).requestBlockCard(eq(1L));
  }

  @Test
  @DisplayName("Успешный перевод между своими картами")
  void transfer_ValidRequest_ReturnsNoContent() {
    // Arrange
    doNothing().when(transactionService).transferBetweenOwnCards(any(InternalTransferRequest.class));

    // Act
    ResponseEntity<Void> response = cardholderCardController.transfer(transferRequest);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

    verify(transactionService).transferBetweenOwnCards(eq(transferRequest));
  }

  @Test
  @DisplayName("Успешный просмотр баланса карты")
  void getCardholderCardBalance_ValidCardId_ReturnsBalance() {
    // Arrange
    BigDecimal expectedBalance = BigDecimal.valueOf(1500.75);
    when(cardService.getCardholderCardBalance(anyLong())).thenReturn(expectedBalance);

    // Act
    ResponseEntity<BigDecimal> response = cardholderCardController.getCardholderCardBalance(1L);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(expectedBalance, response.getBody());

    verify(cardService).getCardholderCardBalance(eq(1L));
  }

  // ==================== ТЕСТЫ ПАГИНАЦИИ ====================

  @Test
  @DisplayName("Успешный просмотр карт с пагинацией")
  void getCardholderCards_WithPagination_ReturnsPagedResults() {
    // Arrange
    Page<CardResponse> cardPage = new PageImpl<>(Collections.singletonList(testCardResponse),
        PageRequest.of(1, 5), 15);
    when(cardService.findCardholderCards(eq(null), any(Pageable.class))).thenReturn(cardPage);

    Pageable paginatedPageable = PageRequest.of(1, 5);

    // Act
    ResponseEntity<Page<CardResponse>> response = cardholderCardController.getCardholderCards(null, paginatedPageable);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().getContent().size());
    assertEquals(15, response.getBody().getTotalElements());
    assertEquals(3, response.getBody().getTotalPages());
    assertEquals(1, response.getBody().getNumber());
    assertEquals(5, response.getBody().getSize());

    verify(cardService).findCardholderCards(eq(null), any(Pageable.class));
  }

  // ==================== ТЕСТЫ ВЫЗОВА СЕРВИСОВ ====================

  @Test
  @DisplayName("Просмотр карт вызывает правильные методы сервиса")
  void getCardholderCards_CallsServiceWithCorrectParameters() {
    // Arrange
    Page<CardResponse> cardPage = new PageImpl<>(Collections.emptyList());
    when(cardService.findCardholderCards(eq("masked"), any(Pageable.class))).thenReturn(cardPage);

    // Act
    cardholderCardController.getCardholderCards("masked", pageable);

    // Assert
    verify(cardService).findCardholderCards(eq("masked"), eq(pageable));
  }

  @Test
  @DisplayName("Запрос блокировки передает ID в сервис")
  void requestBlockCardCard_PassesCardIdToService() {
    // Arrange
    doNothing().when(cardService).requestBlockCard(anyLong());

    // Act
    cardholderCardController.requestBlockCard(999L);

    // Assert
    verify(cardService).requestBlockCard(eq(999L));
  }

  @Test
  @DisplayName("Перевод передает запрос в сервис")
  void transfer_PassesRequestToService() {
    // Arrange
    doNothing().when(transactionService).transferBetweenOwnCards(any(InternalTransferRequest.class));

    // Act
    cardholderCardController.transfer(transferRequest);

    // Assert
    verify(transactionService).transferBetweenOwnCards(eq(transferRequest));
  }

  @Test
  @DisplayName("Просмотр баланса передает ID в сервис")
  void getCardholderCardBalance_PassesCardIdToService() {
    // Arrange
    when(cardService.getCardholderCardBalance(anyLong())).thenReturn(BigDecimal.ZERO);

    // Act
    cardholderCardController.getCardholderCardBalance(999L);

    // Assert
    verify(cardService).getCardholderCardBalance(eq(999L));
  }

  // ==================== ТЕСТЫ ГРАНИЧНЫХ СЛУЧАЕВ ====================

  @Test
  @DisplayName("Просмотр карт с null параметрами")
  void getCardholderCards_NullParameters_HandlesCorrectly() {
    // Arrange
    Page<CardResponse> cardPage = new PageImpl<>(Collections.emptyList());
    doReturn(cardPage).when(cardService).findCardholderCards(isNull(), isNull());

    // Act
    ResponseEntity<Page<CardResponse>> response = cardholderCardController.getCardholderCards(null, null);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    verify(cardService).findCardholderCards(isNull(), isNull());
  }

  @Test
  @DisplayName("Пустой результат поиска карт")
  void getCardholderCards_NoResults_ReturnsEmptyPage() {
    // Arrange
    Page<CardResponse> emptyPage = new PageImpl<>(Collections.emptyList());
    when(cardService.findCardholderCards(eq("nonexistent"), any(Pageable.class))).thenReturn(emptyPage);

    // Act
    ResponseEntity<Page<CardResponse>> response = cardholderCardController.getCardholderCards("nonexistent", pageable);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(0, response.getBody().getContent().size());
    assertEquals(0, response.getBody().getTotalElements());

    verify(cardService).findCardholderCards(eq("nonexistent"), any(Pageable.class));
  }

  @Test
  @DisplayName("Перевод с различными суммами")
  void transfer_VariousAmounts_HandlesCorrectly() {
    // Arrange
    doNothing().when(transactionService).transferBetweenOwnCards(any(InternalTransferRequest.class));

    // Тест с минимальной суммой
    InternalTransferRequest smallTransfer = new InternalTransferRequest(1L, 2L, BigDecimal.valueOf(0.01));
    cardholderCardController.transfer(smallTransfer);
    verify(transactionService).transferBetweenOwnCards(eq(smallTransfer));

    // Тест с большой суммой
    InternalTransferRequest largeTransfer = new InternalTransferRequest(1L, 2L, BigDecimal.valueOf(999999.99));
    cardholderCardController.transfer(largeTransfer);
    verify(transactionService).transferBetweenOwnCards(eq(largeTransfer));
  }

  @Test
  @DisplayName("Блокировка различных карт")
  void requestBlockCard_VariousCardIds_HandlesCorrectly() {
    // Arrange
    doNothing().when(cardService).requestBlockCard(anyLong());

    // Тест с ID = 1
    cardholderCardController.requestBlockCard(1L);
    verify(cardService).requestBlockCard(eq(1L));

    // Тест с большим ID
    cardholderCardController.requestBlockCard(999999L);
    verify(cardService).requestBlockCard(eq(999999L));
  }

  @Test
  @DisplayName("Просмотр баланса различных карт")
  void getCardholderCardBalance_VariousCardIds_HandlesCorrectly() {
    // Arrange
    BigDecimal balance1 = BigDecimal.valueOf(100.50);
    BigDecimal balance2 = BigDecimal.valueOf(999999.99);

    when(cardService.getCardholderCardBalance(1L)).thenReturn(balance1);
    when(cardService.getCardholderCardBalance(999999L)).thenReturn(balance2);

    // Тест баланса карты 1
    ResponseEntity<BigDecimal> response1 = cardholderCardController.getCardholderCardBalance(1L);
    assertNotNull(response1);
    assertEquals(balance1, response1.getBody());
    verify(cardService).getCardholderCardBalance(eq(1L));

    // Тест баланса карты 999999
    ResponseEntity<BigDecimal> response2 = cardholderCardController.getCardholderCardBalance(999999L);
    assertNotNull(response2);
    assertEquals(balance2, response2.getBody());
    verify(cardService).getCardholderCardBalance(eq(999999L));
  }

  // ==================== ТЕСТЫ РАЗЛИЧНЫХ ТИПОВ КАРТ ====================

  @Test
  @DisplayName("Просмотр активных карт")
  void getCardholderCards_ActiveCards_ReturnsActiveStatus() {
    // Arrange
    CardResponse activeCard = new CardResponse(2L, "Тест Пользователь", "**** **** **** 5678",
        CardStatus.ACTIVE, BigDecimal.valueOf(500.0), false, null, 1L);
    Page<CardResponse> cardPage = new PageImpl<>(Collections.singletonList(activeCard));
    when(cardService.findCardholderCards(eq(null), any(Pageable.class))).thenReturn(cardPage);

    // Act
    ResponseEntity<Page<CardResponse>> response = cardholderCardController.getCardholderCards(null, pageable);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(CardStatus.ACTIVE, response.getBody().getContent().get(0).status());

    verify(cardService).findCardholderCards(eq(null), any(Pageable.class));
  }

  @Test
  @DisplayName("Просмотр заблокированных карт")
  void getCardholderCards_BlockedCards_ReturnsBlockedStatus() {
    // Arrange
    CardResponse blockedCard = new CardResponse(3L, "Тест Пользователь", "**** **** **** 9999",
        CardStatus.BLOCKED, BigDecimal.valueOf(0.0), true, null, 1L);
    Page<CardResponse> cardPage = new PageImpl<>(Collections.singletonList(blockedCard));
    when(cardService.findCardholderCards(eq(null), any(Pageable.class))).thenReturn(cardPage);

    // Act
    ResponseEntity<Page<CardResponse>> response = cardholderCardController.getCardholderCards(null, pageable);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(CardStatus.BLOCKED, response.getBody().getContent().get(0).status());
    assertEquals(true, response.getBody().getContent().get(0).isBlockRequested());

    verify(cardService).findCardholderCards(eq(null), any(Pageable.class));
  }

  // ==================== ТЕСТЫ РАЗЛИЧНЫХ СУММ ПЕРЕВОДА ====================

  @Test
  @DisplayName("Перевод с нулевой суммой")
  void transfer_ZeroAmount_HandlesCorrectly() {
    // Arrange
    InternalTransferRequest zeroTransfer = new InternalTransferRequest(1L, 2L, BigDecimal.ZERO);
    doNothing().when(transactionService).transferBetweenOwnCards(any(InternalTransferRequest.class));

    // Act
    ResponseEntity<Void> response = cardholderCardController.transfer(zeroTransfer);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

    verify(transactionService).transferBetweenOwnCards(eq(zeroTransfer));
  }

  @Test
  @DisplayName("Перевод с максимальной суммой")
  void transfer_MaxAmount_HandlesCorrectly() {
    // Arrange
    InternalTransferRequest maxTransfer = new InternalTransferRequest(1L, 2L, BigDecimal.valueOf(1000000000));
    doNothing().when(transactionService).transferBetweenOwnCards(any(InternalTransferRequest.class));

    // Act
    ResponseEntity<Void> response = cardholderCardController.transfer(maxTransfer);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

    verify(transactionService).transferBetweenOwnCards(eq(maxTransfer));
  }

  @Test
  @DisplayName("Перевод с отрицательной суммой")
  void transfer_NegativeAmount_HandlesCorrectly() {
    // Arrange
    InternalTransferRequest negativeTransfer = new InternalTransferRequest(1L, 2L, BigDecimal.valueOf(-100));
    doNothing().when(transactionService).transferBetweenOwnCards(any(InternalTransferRequest.class));

    // Act
    ResponseEntity<Void> response = cardholderCardController.transfer(negativeTransfer);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

    verify(transactionService).transferBetweenOwnCards(eq(negativeTransfer));
  }
}
