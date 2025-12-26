package com.example.bankcards.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
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
import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.service.AdminCardService;

/**
 * Unit тесты для AdminCardController
 * 
 * Тестирует REST API для управления картами администратором:
 * - Поиск карт с фильтрацией и пагинацией
 * - Создание новых карт
 * - Изменение статуса карт
 * - Удаление карт
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты AdminCardController")
class AdminCardControllerTest {

  @Mock
  private AdminCardService cardService;

  @InjectMocks
  private AdminCardController adminCardController;

  private CardResponse testCardResponse;
  private CreateCardRequest createCardRequest;
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

    createCardRequest = new CreateCardRequest(1L);
    pageable = PageRequest.of(0, 10);
  }

  // ==================== ТЕСТЫ УСПЕШНОГО ДОСТУПА ====================

  @Test
  @DisplayName("Успешный поиск всех карт без фильтра")
  void getAllCards_WithoutFilter_ReturnsPage() {
    // Arrange
    Page<CardResponse> cardPage = new PageImpl<>(Collections.singletonList(testCardResponse));
    when(cardService.findCards(eq(null), any(Pageable.class))).thenReturn(cardPage);

    // Act
    ResponseEntity<Page<CardResponse>> response = adminCardController.getCards(null, pageable);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().getContent().size());
    assertEquals("**** **** **** 1234", response.getBody().getContent().get(0).cardNumberMasked());
    assertEquals(CardStatus.ACTIVE, response.getBody().getContent().get(0).status());

    verify(cardService).findCards(eq(null), eq(pageable));
  }

  @Test
  @DisplayName("Успешный поиск карт с фильтром")
  void getAllCards_WithFilter_ReturnsFilteredPage() {
    // Arrange
    Page<CardResponse> cardPage = new PageImpl<>(Collections.singletonList(testCardResponse));
    when(cardService.findCards(eq("1234"), any(Pageable.class))).thenReturn(cardPage);

    // Act
    ResponseEntity<Page<CardResponse>> response = adminCardController.getCards("1234", pageable);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().getContent().size());
    assertEquals("**** **** **** 1234", response.getBody().getContent().get(0).cardNumberMasked());

    verify(cardService).findCards(eq("1234"), eq(pageable));
  }

  @Test
  @DisplayName("Успешное создание новой карты")
  void createCard_ValidRequest_ReturnsCreated() {
    // Arrange
    when(cardService.createCard(any(CreateCardRequest.class))).thenReturn(testCardResponse);

    // Act
    ResponseEntity<CardResponse> response = adminCardController.createCard(createCardRequest);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1L, response.getBody().id());
    assertEquals("Тест Пользователь", response.getBody().ownerName());
    assertEquals("**** **** **** 1234", response.getBody().cardNumberMasked());
    assertEquals(CardStatus.ACTIVE, response.getBody().status());
    assertEquals(BigDecimal.valueOf(1000.50), response.getBody().balance());
    assertEquals(1L, response.getBody().cardholderId());

    verify(cardService).createCard(eq(createCardRequest));
  }

  @Test
  @DisplayName("Успешное изменение статуса карты")
  void updateCardStatus_ValidData_ReturnsNoContent() {
    // Arrange
    doNothing().when(cardService).updateStatus(anyLong(), any(CardStatus.class));

    // Act
    ResponseEntity<Void> response = adminCardController.updateCardStatus(1L, CardStatus.BLOCKED);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

    verify(cardService).updateStatus(eq(1L), eq(CardStatus.BLOCKED));
  }

  @Test
  @DisplayName("Успешное удаление карты")
  void deleteCard_ValidId_ReturnsNoContent() {
    // Arrange
    doNothing().when(cardService).deleteCard(anyLong());

    // Act
    ResponseEntity<Void> response = adminCardController.deleteCard(1L);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

    verify(cardService).deleteCard(eq(1L));
  }

  // ==================== ТЕСТЫ ПАГИНАЦИИ ====================

  @Test
  @DisplayName("Успешный поиск с пагинацией")
  void getAllCards_WithPagination_ReturnsPagedResults() {
    // Arrange
    Page<CardResponse> cardPage = new PageImpl<>(Collections.singletonList(testCardResponse),
        PageRequest.of(1, 5), 15);
    when(cardService.findCards(eq(null), any(Pageable.class))).thenReturn(cardPage);

    Pageable paginatedPageable = PageRequest.of(1, 5);

    // Act
    ResponseEntity<Page<CardResponse>> response = adminCardController.getCards(null, paginatedPageable);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().getContent().size());
    assertEquals(15, response.getBody().getTotalElements());
    assertEquals(3, response.getBody().getTotalPages());
    assertEquals(1, response.getBody().getNumber());
    assertEquals(5, response.getBody().getSize());

    verify(cardService).findCards(eq(null), eq(paginatedPageable));
  }

  // ==================== ТЕСТЫ РАЗЛИЧНЫХ СТАТУСОВ ====================

  @Test
  @DisplayName("Успешное изменение статуса на ACTIVE")
  void updateCardStatus_ToActive_ReturnsNoContent() {
    // Arrange
    doNothing().when(cardService).updateStatus(anyLong(), any(CardStatus.class));

    // Act
    ResponseEntity<Void> response = adminCardController.updateCardStatus(1L, CardStatus.ACTIVE);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

    verify(cardService).updateStatus(eq(1L), eq(CardStatus.ACTIVE));
  }

  @Test
  @DisplayName("Успешное изменение статуса на BLOCKED")
  void updateCardStatus_ToBlocked_ReturnsNoContent() {
    // Arrange
    doNothing().when(cardService).updateStatus(anyLong(), any(CardStatus.class));

    // Act
    ResponseEntity<Void> response = adminCardController.updateCardStatus(1L, CardStatus.BLOCKED);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

    verify(cardService).updateStatus(eq(1L), eq(CardStatus.BLOCKED));
  }

  // ==================== ТЕСТЫ ПУСТЫХ РЕЗУЛЬТАТОВ ====================

  @Test
  @DisplayName("Пустой результат поиска")
  void getAllCards_NoResults_ReturnsEmptyPage() {
    // Arrange
    Page<CardResponse> emptyPage = new PageImpl<>(Collections.emptyList());
    when(cardService.findCards(eq("nonexistent"), any(Pageable.class))).thenReturn(emptyPage);

    // Act
    ResponseEntity<Page<CardResponse>> response = adminCardController.getCards("nonexistent", pageable);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(0, response.getBody().getContent().size());
    assertEquals(0, response.getBody().getTotalElements());

    verify(cardService).findCards(eq("nonexistent"), eq(pageable));
  }
}
