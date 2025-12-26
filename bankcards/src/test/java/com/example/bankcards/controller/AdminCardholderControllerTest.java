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

import com.example.bankcards.dto.CardholderResponse;
import com.example.bankcards.service.AdminCardholderService;

/**
 * Unit тесты для AdminCardholderController
 * 
 * Тестирует REST API для управления держателями карт администратором:
 * - Поиск держателей карт с фильтрацией и пагинацией
 * - Блокировка пользователей
 * - Удаление пользователей
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты AdminCardholderController")
class AdminCardholderControllerTest {

  @Mock
  private AdminCardholderService cardholderService;

  @InjectMocks
  private AdminCardholderController adminCardholderController;

  private CardholderResponse testCardholderResponse;
  private Pageable pageable;

  @BeforeEach
  void setUp() {
    // Настройка тестовых данных
    testCardholderResponse = new CardholderResponse(
        1L,
        "testuser",
        "test@example.com",
        "Тест",
        "Пользователь",
        true);

    pageable = PageRequest.of(0, 10);
  }

  // ==================== ТЕСТЫ УСПЕШНОГО ДОСТУПА ====================

  @Test
  @DisplayName("Успешный поиск всех держателей без фильтра")
  void getAllUsers_WithoutFilter_ReturnsPage() {
    // Arrange
    Page<CardholderResponse> cardholderPage = new PageImpl<>(Collections.singletonList(testCardholderResponse));
    when(cardholderService.findCardholders(eq(null), any(Pageable.class))).thenReturn(cardholderPage);

    // Act
    ResponseEntity<Page<CardholderResponse>> response = adminCardholderController.getCardholders(null, pageable);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().getContent().size());
    assertEquals("testuser", response.getBody().getContent().get(0).username());
    assertEquals("test@example.com", response.getBody().getContent().get(0).email());
    assertEquals("Тест", response.getBody().getContent().get(0).firstName());
    assertEquals("Пользователь", response.getBody().getContent().get(0).lastName());
    assertEquals(true, response.getBody().getContent().get(0).enabled());

    verify(cardholderService).findCardholders(eq(null), any(Pageable.class));
  }

  @Test
  @DisplayName("Успешный поиск держателей с фильтром")
  void getAllUsers_WithFilter_ReturnsFilteredPage() {
    // Arrange
    Page<CardholderResponse> cardholderPage = new PageImpl<>(Collections.singletonList(testCardholderResponse));
    when(cardholderService.findCardholders(eq("testuser"), any(Pageable.class))).thenReturn(cardholderPage);

    // Act
    ResponseEntity<Page<CardholderResponse>> response = adminCardholderController.getCardholders("testuser", pageable);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().getContent().size());
    assertEquals("testuser", response.getBody().getContent().get(0).username());

    verify(cardholderService).findCardholders(eq("testuser"), any(Pageable.class));
  }

  @Test
  @DisplayName("Успешный поиск держателей с пустой строкой фильтра")
  void getAllUsers_WithEmptyFilter_ReturnsAllCardholders() {
    // Arrange
    Page<CardholderResponse> cardholderPage = new PageImpl<>(Collections.singletonList(testCardholderResponse));
    when(cardholderService.findCardholders(eq(""), any(Pageable.class))).thenReturn(cardholderPage);

    // Act
    ResponseEntity<Page<CardholderResponse>> response = adminCardholderController.getCardholders("", pageable);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().getContent().size());

    verify(cardholderService).findCardholders(eq(""), any(Pageable.class));
  }

  @Test
  @DisplayName("Успешная блокировка пользователя")
  void blockUser_ValidId_ReturnsNoContent() {
    // Arrange
    doNothing().when(cardholderService).blockCardholder(anyLong());

    // Act
    ResponseEntity<Void> response = adminCardholderController.blockCardholder(1L);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

    verify(cardholderService).blockCardholder(eq(1L));
  }

  @Test
  @DisplayName("Успешное удаление пользователя")
  void deleteUser_ValidId_ReturnsNoContent() {
    // Arrange
    doNothing().when(cardholderService).deleteCardholder(anyLong());

    // Act
    ResponseEntity<Void> response = adminCardholderController.deleteCardholder(1L);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

    verify(cardholderService).deleteCardholder(eq(1L));
  }

  // ==================== ТЕСТЫ ПАГИНАЦИИ ====================

  @Test
  @DisplayName("Успешный поиск с пагинацией")
  void getAllUsers_WithPagination_ReturnsPagedResults() {
    // Arrange
    Page<CardholderResponse> cardholderPage = new PageImpl<>(Collections.singletonList(testCardholderResponse),
        PageRequest.of(1, 5), 15);
    when(cardholderService.findCardholders(eq(null), any(Pageable.class))).thenReturn(cardholderPage);

    Pageable paginatedPageable = PageRequest.of(1, 5);

    // Act
    ResponseEntity<Page<CardholderResponse>> response = adminCardholderController.getCardholders(null,
        paginatedPageable);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().getContent().size());
    assertEquals(15, response.getBody().getTotalElements());
    assertEquals(3, response.getBody().getTotalPages());
    assertEquals(1, response.getBody().getNumber());
    assertEquals(5, response.getBody().getSize());

    verify(cardholderService).findCardholders(eq(null), any(Pageable.class));
  }

  // ==================== ТЕСТЫ ВЫЗОВА СЕРВИСОВ ====================

  @Test
  @DisplayName("Поиск держателей вызывает правильные методы сервиса")
  void getAllUsers_CallsServiceWithCorrectParameters() {
    // Arrange
    Page<CardholderResponse> cardholderPage = new PageImpl<>(Collections.emptyList());
    when(cardholderService.findCardholders(eq("email@example.com"), any(Pageable.class))).thenReturn(cardholderPage);

    // Act
    adminCardholderController.getCardholders("email@example.com", pageable);

    // Assert
    verify(cardholderService).findCardholders(eq("email@example.com"), eq(pageable));
  }

  @Test
  @DisplayName("Блокировка пользователя передает ID в сервис")
  void blockUser_PassesIdToService() {
    // Arrange
    doNothing().when(cardholderService).blockCardholder(anyLong());

    // Act
    adminCardholderController.blockCardholder(999L);

    // Assert
    verify(cardholderService).blockCardholder(eq(999L));
  }

  @Test
  @DisplayName("Удаление пользователя передает ID в сервис")
  void deleteUser_PassesIdToService() {
    // Arrange
    doNothing().when(cardholderService).deleteCardholder(anyLong());

    // Act
    adminCardholderController.deleteCardholder(999L);

    // Assert
    verify(cardholderService).deleteCardholder(eq(999L));
  }

  // ==================== ТЕСТЫ ГРАНИЧНЫХ СЛУЧАЕВ ====================

  @Test
  @DisplayName("Поиск держателей с null параметрами")
  void getCardholders_NullParameters_HandlesCorrectly() {
    // Arrange
    Page<CardholderResponse> cardholderPage = new PageImpl<>(Collections.emptyList());
    doReturn(cardholderPage).when(cardholderService).findCardholders(isNull(), isNull());

    // Act
    ResponseEntity<Page<CardholderResponse>> response = adminCardholderController.getCardholders(null, null);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());

    verify(cardholderService).findCardholders(isNull(), isNull());
  }

  @Test
  @DisplayName("Пустой результат поиска")
  void getAllUsers_NoResults_ReturnsEmptyPage() {
    // Arrange
    Page<CardholderResponse> emptyPage = new PageImpl<>(Collections.emptyList());
    when(cardholderService.findCardholders(eq("nonexistent"), any(Pageable.class))).thenReturn(emptyPage);

    // Act
    ResponseEntity<Page<CardholderResponse>> response = adminCardholderController.getCardholders("nonexistent",
        pageable);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(0, response.getBody().getContent().size());
    assertEquals(0, response.getBody().getTotalElements());

    verify(cardholderService).findCardholders(eq("nonexistent"), any(Pageable.class));
  }

  // ==================== ТЕСТЫ РАЗЛИЧНЫХ ПОЛЬЗОВАТЕЛЕЙ ====================

  @Test
  @DisplayName("Поиск активного пользователя")
  void getAllUsers_ActiveUser_ReturnsEnabledTrue() {
    // Arrange
    CardholderResponse activeUser = new CardholderResponse(2L, "activeuser", "active@example.com", "Активный",
        "Пользователь", true);
    Page<CardholderResponse> cardholderPage = new PageImpl<>(Collections.singletonList(activeUser));
    when(cardholderService.findCardholders(eq(null), any(Pageable.class))).thenReturn(cardholderPage);

    // Act
    ResponseEntity<Page<CardholderResponse>> response = adminCardholderController.getCardholders(null, pageable);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(true, response.getBody().getContent().get(0).enabled());

    verify(cardholderService).findCardholders(eq(null), any(Pageable.class));
  }

  @Test
  @DisplayName("Поиск заблокированного пользователя")
  void getAllUsers_BlockedUser_ReturnsEnabledFalse() {
    // Arrange
    CardholderResponse blockedUser = new CardholderResponse(3L, "blockeduser", "blocked@example.com", "Заблокированный",
        "Пользователь", false);
    Page<CardholderResponse> cardholderPage = new PageImpl<>(Collections.singletonList(blockedUser));
    when(cardholderService.findCardholders(eq(null), any(Pageable.class))).thenReturn(cardholderPage);

    // Act
    ResponseEntity<Page<CardholderResponse>> response = adminCardholderController.getCardholders(null, pageable);

    // Assert
    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(false, response.getBody().getContent().get(0).enabled());

    verify(cardholderService).findCardholders(eq(null), any(Pageable.class));
  }

  // ==================== ТЕСТЫ РАЗЛИЧНЫХ ИД ====================

  @Test
  @DisplayName("Блокировка пользователя с ID = 1")
  void blockUser_Id1_CorrectlyPassed() {
    // Arrange
    doNothing().when(cardholderService).blockCardholder(anyLong());

    // Act
    adminCardholderController.blockCardholder(1L);

    // Assert
    verify(cardholderService).blockCardholder(eq(1L));
  }

  @Test
  @DisplayName("Блокировка пользователя с большим ID")
  void blockUser_LargeId_CorrectlyPassed() {
    // Arrange
    doNothing().when(cardholderService).blockCardholder(anyLong());

    // Act
    adminCardholderController.blockCardholder(999999L);

    // Assert
    verify(cardholderService).blockCardholder(eq(999999L));
  }

  @Test
  @DisplayName("Удаление пользователя с ID = 1")
  void deleteUser_Id1_CorrectlyPassed() {
    // Arrange
    doNothing().when(cardholderService).deleteCardholder(anyLong());

    // Act
    adminCardholderController.deleteCardholder(1L);

    // Assert
    verify(cardholderService).deleteCardholder(eq(1L));
  }

  @Test
  @DisplayName("Удаление пользователя с большим ID")
  void deleteUser_LargeId_CorrectlyPassed() {
    // Arrange
    doNothing().when(cardholderService).deleteCardholder(anyLong());

    // Act
    adminCardholderController.deleteCardholder(999999L);

    // Assert
    verify(cardholderService).deleteCardholder(eq(999999L));
  }
}
