package com.example.bankcards.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

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

import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.CardholderResponse;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Cardholder;
import com.example.bankcards.repository.CardholderRepository;
import com.example.common.auth.event.UserCreatedEvent;

/**
 * Тесты для AdminCardholderServiceImpl
 * 
 * Тестирует бизнес-логику управления держателями карт:
 * - Поиск держателей с пагинацией
 * - Регистрация нового держателя при создании пользователя
 * - Блокировка держателя
 * - Удаление держателя
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты AdminCardholderServiceImpl")
class AdminCardholderServiceImplTest {

  @Mock
  private CardholderRepository cardholderRepository;

  @Mock
  private AdminCardService cardService;

  @Mock
  private AuditService auditService;

  @InjectMocks
  private AdminCardholderServiceImpl adminCardholderService;

  private Cardholder testCardholder;
  private CardholderResponse testCardholderResponse;
  private CardResponse testCardResponse;
  private UserCreatedEvent testUserEvent;
  private Pageable pageable;

  @BeforeEach
  void setUp() {
    // Настройка тестовых данных
    testCardholder = Cardholder.builder()
        .id(1L)
        .username("user123")
        .email("ivan@example.com")
        .firstName("John")
        .lastName("Doe")
        .enabled(true)
        .createdAt(LocalDateTime.now())
        .build();

    testCardholderResponse = new CardholderResponse(
        1L,
        "user123",
        "ivan@example.com",
        "John",
        "Doe",
        true);

    testCardResponse = new CardResponse(
        1L,
        "John Doe",
        "**** **** **** 1234",
        CardStatus.ACTIVE,
        BigDecimal.valueOf(1000.00),
        false,
        null,
        1L);

    testUserEvent = new UserCreatedEvent("user123", "ivan@example.com", "John", "Doe", LocalDateTime.now());
    pageable = PageRequest.of(0, 10);
  }

  @Test
  @DisplayName("Успешный поиск держателей с пагинацией")
  void findCardholders_WithValidParameters_ReturnsPage() {
    // Arrange
    Page<Cardholder> cardholderPage = new PageImpl<>(java.util.Collections.singletonList(testCardholder));
    when(cardholderRepository.findByUserInfo(anyString(), eq(pageable))).thenReturn(cardholderPage);

    // Act
    Page<CardholderResponse> result = adminCardholderService.findCardholders("search", pageable);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getContent().size());
    assertEquals("John", result.getContent().get(0).firstName());
    assertEquals("Doe", result.getContent().get(0).lastName());

    verify(cardholderRepository).findByUserInfo("search", pageable);
  }

  @Test
  @DisplayName("Поиск держателей с пустым результатом")
  void findCardholders_WithNoResults_ReturnsEmptyPage() {
    // Arrange
    Page<Cardholder> emptyPage = new PageImpl<>(java.util.Collections.emptyList());
    when(cardholderRepository.findByUserInfo(anyString(), eq(pageable))).thenReturn(emptyPage);

    // Act
    Page<CardholderResponse> result = adminCardholderService.findCardholders("nonexistent", pageable);

    // Assert
    assertNotNull(result);
    assertTrue(result.getContent().isEmpty());
    assertEquals(0, result.getTotalElements());

    verify(cardholderRepository).findByUserInfo("nonexistent", pageable);
  }

  @Test
  @DisplayName("Успешная регистрация нового держателя")
  void registerCardholder_WithValidUserEvent_SavesCardholder() {
    // Arrange
    when(cardholderRepository.save(any(Cardholder.class))).thenReturn(testCardholder);
    when(cardService.createCard(any())).thenReturn(testCardResponse);

    // Act & Assert
    assertDoesNotThrow(() -> adminCardholderService.registerCardholder(testUserEvent));
  }

  @Test
  @DisplayName("Попытка регистрации уже существующего держателя")
  void registerCardholder_WithExistingEmail_SkipsRegistration() {
    // Arrange
    when(cardholderRepository.findByEmail("ivan@example.com")).thenReturn(Optional.of(testCardholder));

    // Act
    adminCardholderService.registerCardholder(testUserEvent);

    // Assert
    verify(cardholderRepository).findByEmail("ivan@example.com");
    verify(cardholderRepository, never()).save(any());
    verify(cardService, never()).createCard(any());
    verify(auditService, never()).logCardholderRegister(any(), any(), any());
  }

  @Test
  @DisplayName("Успешная блокировка держателя")
  void blockCardholder_WithValidId_BlocksCardholder() {
    // Arrange
    when(cardholderRepository.findById(1L)).thenReturn(Optional.of(testCardholder));

    // Act
    adminCardholderService.blockCardholder(1L);

    // Assert
    verify(cardholderRepository).findById(1L);
    verify(cardholderRepository).save(testCardholder);
    verify(auditService).logCardholderBlocking(1L);
  }

  @Test
  @DisplayName("Попытка блокировки несуществующего держателя")
  void blockCardholder_WithNonExistentId_DoesNotThrowException() {
    // Arrange
    when(cardholderRepository.findById(1L)).thenReturn(Optional.empty());

    // Act & Assert
    assertDoesNotThrow(() -> adminCardholderService.blockCardholder(1L));

    verify(cardholderRepository).findById(1L);
    verify(cardholderRepository, never()).save(any());
    verify(auditService, never()).logCardholderBlocking(any());
  }

  @Test
  @DisplayName("Успешное удаление держателя")
  void deleteCardholder_WithValidId_DeletesCardholder() {
    // Arrange
    when(cardholderRepository.existsById(1L)).thenReturn(true);

    // Act
    adminCardholderService.deleteCardholder(1L);

    // Assert
    verify(cardholderRepository).existsById(1L);
    verify(cardholderRepository).deleteById(1L);
    verify(auditService).logCardholderDeletion(1L);
  }

  @Test
  @DisplayName("Попытка удаления несуществующего держателя")
  void deleteCardholder_WithNonExistentId_DoesNotThrowException() {
    // Arrange
    when(cardholderRepository.existsById(1L)).thenReturn(false);

    // Act & Assert
    assertDoesNotThrow(() -> adminCardholderService.deleteCardholder(1L));

    verify(cardholderRepository).existsById(1L);
    verify(cardholderRepository, never()).deleteById(any());
    verify(auditService, never()).logCardholderDeletion(any());
  }
}
