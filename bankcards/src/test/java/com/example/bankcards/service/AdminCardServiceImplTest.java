package com.example.bankcards.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;

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

import com.example.bankcards.config.CardConfig;
import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Cardholder;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.CardholderRepository;
import com.example.bankcards.util.CardCryptoUtil;
import com.example.bankcards.util.CardGenerator;

import jakarta.persistence.EntityNotFoundException;

/**
 * Тесты для AdminCardServiceImpl
 * 
 * Тестирует бизнес-логику управления картами администратором:
 * - Поиск всех карт с фильтрацией
 * - Создание новых карт
 * - Изменение статуса карт
 * - Удаление карт
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты AdminCardServiceImpl")
class AdminCardServiceImplTest {

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

  @Mock
  private CardGenerator cardGenerator;

  @InjectMocks
  private AdminCardServiceImpl adminCardService;

  private Cardholder testCardholder;
  private Card testCard;
  private CreateCardRequest createCardRequest;
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

    testCard = Card.builder()
        .id(1L)
        .cardNumberEncrypted("encrypted123")
        .cardNumberMasked("1234-****-****-5678")
        .ownerName("Test User")
        .expiryDate(LocalDate.now().plusYears(4))
        .status(CardStatus.ACTIVE)
        .balance(BigDecimal.ZERO)
        .owner(testCardholder)
        .build();

    createCardRequest = new CreateCardRequest(1L);
    pageable = PageRequest.of(0, 10);
  }

  @Test
  @DisplayName("Успешный поиск всех карт без фильтра")
  void findCards_WithoutFilter_ReturnsPage() {
    // Arrange
    Page<Card> cardPage = new PageImpl<>(java.util.Collections.singletonList(testCard));
    when(cardRepository.findByOwnerNameAndCardNumberMasked("", pageable)).thenReturn(cardPage);

    // Act
    Page<CardResponse> result = adminCardService.findCards(null, pageable);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getContent().size());
    assertEquals("1234-****-****-5678", result.getContent().get(0).cardNumberMasked());
    assertEquals(CardStatus.ACTIVE, result.getContent().get(0).status());

    verify(cardRepository).findByOwnerNameAndCardNumberMasked("", pageable);
    verify(auditService).logCardsListView(10, "findAll");
  }

  @Test
  @DisplayName("Успешный поиск карт с фильтром")
  void findCards_WithFilter_ReturnsPage() {
    // Arrange
    Page<Card> cardPage = new PageImpl<>(java.util.Collections.singletonList(testCard));
    when(cardRepository.findByOwnerNameAndCardNumberMasked("1234", pageable)).thenReturn(cardPage);

    // Act
    Page<CardResponse> result = adminCardService.findCards("1234", pageable);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getContent().size());
    assertEquals("1234-****-****-5678", result.getContent().get(0).cardNumberMasked());

    verify(cardRepository).findByOwnerNameAndCardNumberMasked("1234", pageable);
  }

  @Test
  @DisplayName("Поиск карт с пустым результатом")
  void findCards_NoResults_ReturnsEmptyPage() {
    // Arrange
    Page<Card> emptyPage = new PageImpl<>(java.util.Collections.emptyList());
    when(cardRepository.findByOwnerNameAndCardNumberMasked("", pageable)).thenReturn(emptyPage);

    // Act
    Page<CardResponse> result = adminCardService.findCards("", pageable);

    // Assert
    assertNotNull(result);
    assertTrue(result.getContent().isEmpty());
    assertEquals(0, result.getTotalElements());

    verify(cardRepository).findByOwnerNameAndCardNumberMasked("", pageable);
  }

  @Test
  @DisplayName("Успешное создание новой карты")
  void createCard_ValidRequest_ReturnsCardResponse() {
    // Arrange
    String generatedCardNumber = "1234567890123456";
    String maskedNumber = "1234-****-****-3456";
    String encryptedNumber = "encrypted_card_number";

    when(cardholderRepository.findById(1L)).thenReturn(java.util.Optional.of(testCardholder));
    when(CardGenerator.generate(cardConfig.getBin())).thenReturn(generatedCardNumber);
    when(cardCryptoUtil.maskCardNumber(generatedCardNumber)).thenReturn(maskedNumber);
    when(cardCryptoUtil.encrypt(generatedCardNumber)).thenReturn(encryptedNumber);
    when(cardRepository.save(any(Card.class))).thenReturn(testCard);

    // Act
    CardResponse result = adminCardService.createCard(createCardRequest);

    // Assert
    assertNotNull(result);
    assertEquals("1234-****-****-5678", result.cardNumberMasked());
    assertEquals(CardStatus.ACTIVE, result.status());
    assertEquals(BigDecimal.ZERO, result.balance());

    verify(cardholderRepository).findById(1L);
    verify(cardGenerator).generate(cardConfig.getBin());
    verify(cardCryptoUtil).maskCardNumber(generatedCardNumber);
    verify(cardCryptoUtil).encrypt(generatedCardNumber);
    verify(cardRepository).save(any(Card.class));
    verify(auditService).logCardCreation(1L, maskedNumber, 1L);
  }

  @Test
  @DisplayName("Создание карты для несуществующего держателя")
  void createCard_NonExistentCardholder_ThrowsException() {
    // Arrange
    when(cardholderRepository.findById(1L)).thenReturn(java.util.Optional.empty());

    // Act & Assert
    EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
        () -> adminCardService.createCard(createCardRequest));

    assertEquals("Пользователь не найден", exception.getMessage());

    verify(cardholderRepository).findById(1L);
    verify(cardRepository, never()).save(any());
    verify(auditService, never()).logCardCreation(any(), any(), any());
  }

  @Test
  @DisplayName("Успешное изменение статуса карты")
  void updateStatus_ValidData_Success() {
    // Arrange
    Card card = Card.builder()
        .id(1L)
        .cardNumberMasked("1234-****-****-5678")
        .status(CardStatus.ACTIVE)
        .owner(testCardholder)
        .build();

    when(cardRepository.findById(1L)).thenReturn(java.util.Optional.of(card));

    // Act
    assertDoesNotThrow(() -> adminCardService.updateStatus(1L, CardStatus.BLOCKED));

    // Assert
    verify(cardRepository).findById(1L);
    verify(cardRepository).save(card);
    verify(auditService).logCardStatusChange(1L, "ACTIVE", "BLOCKED");

    // Проверяем, что статус изменился
    assertEquals(CardStatus.BLOCKED, card.getStatus());
  }

  @Test
  @DisplayName("Изменение статуса несуществующей карты")
  void updateStatus_NonExistentCard_ThrowsException() {
    // Arrange
    when(cardRepository.findById(1L)).thenReturn(java.util.Optional.empty());

    // Act & Assert
    EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
        () -> adminCardService.updateStatus(1L, CardStatus.BLOCKED));

    assertEquals("Карта не найдена", exception.getMessage());

    verify(cardRepository).findById(1L);
    verify(cardRepository, never()).save(any());
    verify(auditService, never()).logCardStatusChange(any(), any(), any());
  }

  @Test
  @DisplayName("Успешное удаление карты")
  void deleteCard_ValidId_Success() {
    // Arrange
    Card card = Card.builder()
        .id(1L)
        .cardNumberMasked("1234-****-****-5678")
        .build();

    when(cardRepository.findById(1L)).thenReturn(java.util.Optional.of(card));

    // Act
    assertDoesNotThrow(() -> adminCardService.deleteCard(1L));

    // Assert
    verify(cardRepository).findById(1L);
    verify(cardRepository).deleteById(1L);
    verify(auditService).logCardDeletion(1L, "1234-****-****-5678");
  }

  @Test
  @DisplayName("Удаление несуществующей карты")
  void deleteCard_NonExistentCard_ThrowsException() {
    // Arrange
    when(cardRepository.findById(1L)).thenReturn(java.util.Optional.empty());

    // Act & Assert
    EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
        () -> adminCardService.deleteCard(1L));

    assertEquals("Карта не найдена", exception.getMessage());

    verify(cardRepository).findById(1L);
    verify(cardRepository, never()).deleteById(any());
    verify(auditService, never()).logCardDeletion(any(), any());
  }

  @Test
  @DisplayName("Создание карты с пустым поисковым запросом")
  void findCards_EmptySearch_QueriesAllCards() {
    // Arrange
    Page<Card> cardPage = new PageImpl<>(java.util.Collections.singletonList(testCard));
    when(cardRepository.findByOwnerNameAndCardNumberMasked("", pageable)).thenReturn(cardPage);

    // Act
    Page<CardResponse> result = adminCardService.findCards("   ", pageable);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.getContent().size());

    verify(cardRepository).findByOwnerNameAndCardNumberMasked("", pageable);
  }
}
