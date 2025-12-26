package com.example.bankcards.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

import com.example.bankcards.dto.InternalTransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Cardholder;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransactionRepository;

import jakarta.persistence.EntityNotFoundException;

/**
 * Тесты для TransactionServiceImpl
 * 
 * Тестирует бизнес-логику переводов между картами:
 * - Успешные переводы
 * - Переводы на ту же карту
 * - Недостаток средств
 * - Заблокированные карты
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты TransactionServiceImpl")
class TransactionServiceImplTest {

  @Mock
  private AuthenticationFacade authenticationFacade;

  @Mock
  private CardRepository cardRepository;

  @Mock
  private TransactionRepository transactionRepository;

  @Mock
  private AuditService auditService;

  @InjectMocks
  private TransactionServiceImpl transactionService;

  private Cardholder testCardholder;
  private Card fromCard;
  private Card toCard;
  private Transaction testTransaction;
  private InternalTransferRequest transferRequest;

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

    fromCard = Card.builder()
        .id(1L)
        .cardNumberMasked("1234-****-****-5678")
        .ownerName("Test User")
        .expiryDate(LocalDate.now().plusYears(3))
        .status(CardStatus.ACTIVE)
        .balance(new BigDecimal("1000.00"))
        .owner(testCardholder)
        .build();

    toCard = Card.builder()
        .id(2L)
        .cardNumberMasked("8765-****-****-4321")
        .ownerName("Test User")
        .expiryDate(LocalDate.now().plusYears(3))
        .status(CardStatus.ACTIVE)
        .balance(new BigDecimal("500.00"))
        .owner(testCardholder)
        .build();

    testTransaction = Transaction.builder()
        .id(1L)
        .fromCard(fromCard)
        .toCard(toCard)
        .amount(new BigDecimal("200.00"))
        .description("Перевод между своими картами")
        .status("COMPLETED")
        .build();

    transferRequest = new InternalTransferRequest(1L, 2L, new BigDecimal("200.00"));
  }

  @Test
  @DisplayName("Успешный перевод между картами")
  void transferBetweenOwnCards_ValidRequest_Success() {
    // Arrange
    when(authenticationFacade.getAuthenticationName()).thenReturn("testuser");
    when(cardRepository.findByIdAndOwner_Username(1L, "testuser")).thenReturn(java.util.Optional.of(fromCard));
    when(cardRepository.findByIdAndOwner_Username(2L, "testuser")).thenReturn(java.util.Optional.of(toCard));
    when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

    // Act
    assertDoesNotThrow(() -> transactionService.transferBetweenOwnCards(transferRequest));

    // Assert
    verify(cardRepository).findByIdAndOwner_Username(1L, "testuser");
    verify(cardRepository).findByIdAndOwner_Username(2L, "testuser");
    verify(transactionRepository).save(any(Transaction.class));
    verify(auditService).logTransfer(eq(1L), eq(2L), eq("1234-****-****-5678"), eq("8765-****-****-4321"), eq("200.00"),
        eq("RUB"));

    // Проверяем, что баланс изменился корректно
    assertEquals(new BigDecimal("800.00"), fromCard.getBalance());
    assertEquals(new BigDecimal("700.00"), toCard.getBalance());
  }

  @Test
  @DisplayName("Попытка перевода на ту же карту")
  void transferBetweenOwnCards_SameCard_ThrowsException() {
    // Arrange
    InternalTransferRequest sameCardRequest = new InternalTransferRequest(1L, 1L, new BigDecimal("100.00"));

    // Act & Assert
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> transactionService.transferBetweenOwnCards(sameCardRequest));

    assertEquals("Нельзя переводить средства на ту же карту", exception.getMessage());

    verify(cardRepository, never()).findByIdAndOwner_Username(anyLong(), anyString());
    verify(transactionRepository, never()).save(any());
    verify(auditService, never()).logTransfer(any(), any(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("Перевод с карты отправителя не найдена")
  void transferBetweenOwnCards_FromCardNotFound_ThrowsException() {
    // Arrange
    when(authenticationFacade.getAuthenticationName()).thenReturn("testuser");
    when(cardRepository.findByIdAndOwner_Username(1L, "testuser")).thenReturn(java.util.Optional.empty());

    // Act & Assert
    EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
        () -> transactionService.transferBetweenOwnCards(transferRequest));

    assertEquals("Карта списания не найдена", exception.getMessage());

    verify(cardRepository).findByIdAndOwner_Username(1L, "testuser");
    verify(cardRepository, never()).findByIdAndOwner_Username(2L, "testuser");
    verify(transactionRepository, never()).save(any());
  }

  @Test
  @DisplayName("Перевод на карту получателя не найдена")
  void transferBetweenOwnCards_ToCardNotFound_ThrowsException() {
    // Arrange
    when(authenticationFacade.getAuthenticationName()).thenReturn("testuser");
    when(cardRepository.findByIdAndOwner_Username(1L, "testuser")).thenReturn(java.util.Optional.of(fromCard));
    when(cardRepository.findByIdAndOwner_Username(2L, "testuser")).thenReturn(java.util.Optional.empty());

    // Act & Assert
    EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
        () -> transactionService.transferBetweenOwnCards(transferRequest));

    assertEquals("Карта зачисления не найдена", exception.getMessage());

    verify(cardRepository).findByIdAndOwner_Username(1L, "testuser");
    verify(cardRepository).findByIdAndOwner_Username(2L, "testuser");
    verify(transactionRepository, never()).save(any());
  }

  @Test
  @DisplayName("Перевод с заблокированной карты отправителя")
  void transferBetweenOwnCards_FromCardBlocked_ThrowsException() {
    // Arrange
    Card blockedFromCard = Card.builder()
        .id(1L)
        .cardNumberMasked("1234-****-****-5678")
        .ownerName("Test User")
        .expiryDate(LocalDate.now().plusYears(3))
        .status(CardStatus.BLOCKED)
        .balance(new BigDecimal("1000.00"))
        .owner(testCardholder)
        .build();

    when(authenticationFacade.getAuthenticationName()).thenReturn("testuser");
    when(cardRepository.findByIdAndOwner_Username(1L, "testuser")).thenReturn(java.util.Optional.of(blockedFromCard));
    when(cardRepository.findByIdAndOwner_Username(2L, "testuser")).thenReturn(java.util.Optional.of(toCard));

    // Act & Assert
    IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> transactionService.transferBetweenOwnCards(transferRequest));

    assertEquals("Карта списания заблокирована", exception.getMessage());

    verify(cardRepository).findByIdAndOwner_Username(1L, "testuser");
    verify(cardRepository).findByIdAndOwner_Username(2L, "testuser");
    verify(transactionRepository, never()).save(any());
  }

  @Test
  @DisplayName("Перевод на заблокированную карту получателя")
  void transferBetweenOwnCards_ToCardBlocked_ThrowsException() {
    // Arrange
    Card blockedToCard = Card.builder()
        .id(2L)
        .cardNumberMasked("8765-****-****-4321")
        .ownerName("Test User")
        .expiryDate(LocalDate.now().plusYears(3))
        .status(CardStatus.BLOCKED)
        .balance(new BigDecimal("500.00"))
        .owner(testCardholder)
        .build();

    when(authenticationFacade.getAuthenticationName()).thenReturn("testuser");
    when(cardRepository.findByIdAndOwner_Username(1L, "testuser")).thenReturn(java.util.Optional.of(fromCard));
    when(cardRepository.findByIdAndOwner_Username(2L, "testuser")).thenReturn(java.util.Optional.of(blockedToCard));

    // Act & Assert
    IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> transactionService.transferBetweenOwnCards(transferRequest));

    assertEquals("Карта зачисления заблокирована", exception.getMessage());

    verify(cardRepository).findByIdAndOwner_Username(1L, "testuser");
    verify(cardRepository).findByIdAndOwner_Username(2L, "testuser");
    verify(transactionRepository, never()).save(any());
  }

  @Test
  @DisplayName("Перевод с недостаточным балансом")
  void transferBetweenOwnCards_InsufficientFunds_ThrowsException() {
    // Arrange
    Card lowBalanceCard = Card.builder()
        .id(1L)
        .cardNumberMasked("1234-****-****-5678")
        .ownerName("Test User")
        .expiryDate(LocalDate.now().plusYears(3))
        .status(CardStatus.ACTIVE)
        .balance(new BigDecimal("50.00"))
        .owner(testCardholder)
        .build();

    when(authenticationFacade.getAuthenticationName()).thenReturn("testuser");
    when(cardRepository.findByIdAndOwner_Username(1L, "testuser")).thenReturn(java.util.Optional.of(lowBalanceCard));
    when(cardRepository.findByIdAndOwner_Username(2L, "testuser")).thenReturn(java.util.Optional.of(toCard));

    // Act & Assert
    InsufficientFundsException exception = assertThrows(InsufficientFundsException.class,
        () -> transactionService.transferBetweenOwnCards(transferRequest));

    assertEquals("Недостаточно средств", exception.getMessage());

    verify(cardRepository).findByIdAndOwner_Username(1L, "testuser");
    verify(cardRepository).findByIdAndOwner_Username(2L, "testuser");
    verify(transactionRepository, never()).save(any());
  }
}
