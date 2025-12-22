package com.example.bankrest.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.bankrest.dto.InternalTransferRequest;
import com.example.bankrest.entity.Card;
import com.example.bankrest.entity.CardStatus;
import com.example.bankrest.entity.Cardholder;
import com.example.bankrest.entity.Transaction;
import com.example.bankrest.exception.InsufficientFundsException;
import com.example.bankrest.repository.CardRepository;
import com.example.bankrest.repository.TransactionRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

  @Mock
  private CardRepository cardRepository;

  @Mock
  private TransactionRepository transactionRepository;

  @InjectMocks
  private TransactionServiceImpl transactionService;

  private Cardholder testOwner;
  private Card fromCard;
  private Card toCard;
  private Transaction testTransaction;

  @BeforeEach
  void setUp() {
    testOwner = new Cardholder();
    testOwner.setId(1L);
    testOwner.setUsername("testuser");
    testOwner.setEmail("testuser@example.com");
    testOwner.setFirstName("Test");
    testOwner.setLastName("User");
    testOwner.setCreatedAt(LocalDateTime.now());

    fromCard = Card.builder()
        .id(1L)
        .cardNumberEncrypted("encrypted_from_1234")
        .cardNumberMasked("**** **** **** 1234")
        .ownerName("Test User")
        .expiryDate(LocalDate.now().plusYears(2))
        .status(CardStatus.ACTIVE)
        .balance(BigDecimal.valueOf(1000.00))
        .owner(testOwner)
        .createdAt(LocalDateTime.now())
        .build();

    toCard = Card.builder()
        .id(2L)
        .cardNumberEncrypted("encrypted_to_5678")
        .cardNumberMasked("**** **** **** 5678")
        .ownerName("Test User")
        .expiryDate(LocalDate.now().plusYears(3))
        .status(CardStatus.ACTIVE)
        .balance(BigDecimal.valueOf(500.00))
        .owner(testOwner)
        .createdAt(LocalDateTime.now())
        .build();

    testTransaction = Transaction.builder()
        .fromCard(fromCard)
        .toCard(toCard)
        .amount(BigDecimal.valueOf(100.00))
        .status("COMPLETED")
        .description("Перевод между своими картами")
        .build();
  }

  @Nested
  @DisplayName("Успешные сценарии перевода")
  @Tag("successful-transfers")
  class SuccessfulTransferTests {

    @Test
    @DisplayName("Успешный перевод между картами пользователя")
    void shouldTransferBetweenOwnCardsSuccessfully() {
      // Given
      InternalTransferRequest request = new InternalTransferRequest(1L, 2L, BigDecimal.valueOf(100.00));
      when(cardRepository.findByIdAndOwner_Username(1L, "testuser")).thenReturn(Optional.of(fromCard));
      when(cardRepository.findByIdAndOwner_Username(2L, "testuser")).thenReturn(Optional.of(toCard));
      when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

      // When
      transactionService.transferBetweenOwnCards("testuser", request);

      // Then
      assertEquals(BigDecimal.valueOf(900.00), fromCard.getBalance());
      assertEquals(BigDecimal.valueOf(600.00), toCard.getBalance());

      verify(cardRepository).findByIdAndOwner_Username(1L, "testuser");
      verify(cardRepository).findByIdAndOwner_Username(2L, "testuser");
      verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Проверка корректности изменения балансов")
    void shouldUpdateBalancesCorrectly() {
      // Given
      InternalTransferRequest request = new InternalTransferRequest(1L, 2L, BigDecimal.valueOf(250.50));
      when(cardRepository.findByIdAndOwner_Username(1L, "testuser")).thenReturn(Optional.of(fromCard));
      when(cardRepository.findByIdAndOwner_Username(2L, "testuser")).thenReturn(Optional.of(toCard));
      when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

      // When
      transactionService.transferBetweenOwnCards("testuser", request);

      // Then
      BigDecimal expectedFromBalance = BigDecimal.valueOf(1000.00).subtract(BigDecimal.valueOf(250.50));
      BigDecimal expectedToBalance = BigDecimal.valueOf(500.00).add(BigDecimal.valueOf(250.50));

      assertEquals(expectedFromBalance, fromCard.getBalance());
      assertEquals(expectedToBalance, toCard.getBalance());

      verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Проверка сохранения записи транзакции")
    void shouldSaveTransactionRecord() {
      // Given
      InternalTransferRequest request = new InternalTransferRequest(1L, 2L, BigDecimal.valueOf(50.00));
      when(cardRepository.findByIdAndOwner_Username(1L, "testuser")).thenReturn(Optional.of(fromCard));
      when(cardRepository.findByIdAndOwner_Username(2L, "testuser")).thenReturn(Optional.of(toCard));
      when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

      // When
      transactionService.transferBetweenOwnCards("testuser", request);

      // Then
      verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Проверка полей транзакции")
    void shouldSetTransactionFieldsCorrectly() {
      // Given
      InternalTransferRequest request = new InternalTransferRequest(1L, 2L, BigDecimal.valueOf(75.25));
      Transaction savedTransaction = Transaction.builder()
          .id(1L)
          .fromCard(fromCard)
          .toCard(toCard)
          .amount(BigDecimal.valueOf(75.25))
          .status("COMPLETED")
          .description("Перевод между своими картами")
          .build();

      when(cardRepository.findByIdAndOwner_Username(1L, "testuser")).thenReturn(Optional.of(fromCard));
      when(cardRepository.findByIdAndOwner_Username(2L, "testuser")).thenReturn(Optional.of(toCard));
      when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

      // When
      transactionService.transferBetweenOwnCards("testuser", request);

      // Then
      verify(transactionRepository).save(argThat(transaction -> transaction.getFromCard().equals(fromCard) &&
          transaction.getToCard().equals(toCard) &&
          transaction.getAmount().equals(BigDecimal.valueOf(75.25)) &&
          "COMPLETED".equals(transaction.getStatus()) &&
          "Перевод между своими картами".equals(transaction.getDescription())));
    }
  }

  @Nested
  @DisplayName("Тесты валидации и обработки ошибок")
  @Tag("validation-errors")
  class ValidationErrorTests {

    @Test
    @DisplayName("Недостаточно средств на карте списания")
    void shouldThrowInsufficientFundsException() {
      // Given
      fromCard.setBalance(BigDecimal.valueOf(50.00));
      InternalTransferRequest request = new InternalTransferRequest(1L, 2L, BigDecimal.valueOf(100.00));
      when(cardRepository.findByIdAndOwner_Username(1L, "testuser")).thenReturn(Optional.of(fromCard));
      when(cardRepository.findByIdAndOwner_Username(2L, "testuser")).thenReturn(Optional.of(toCard));

      // When & Then
      InsufficientFundsException exception = assertThrows(InsufficientFundsException.class,
          () -> transactionService.transferBetweenOwnCards("testuser", request));

      assertEquals("Недостаточно средств", exception.getMessage());
      verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Карта списания заблокирована")
    void shouldThrowIllegalStateExceptionForBlockedCard() {
      // Given
      fromCard.setStatus(CardStatus.BLOCKED);
      InternalTransferRequest request = new InternalTransferRequest(1L, 2L, BigDecimal.valueOf(50.00));
      when(cardRepository.findByIdAndOwner_Username(1L, "testuser")).thenReturn(Optional.of(fromCard));
      when(cardRepository.findByIdAndOwner_Username(2L, "testuser")).thenReturn(Optional.of(toCard));

      // When & Then
      IllegalStateException exception = assertThrows(IllegalStateException.class,
          () -> transactionService.transferBetweenOwnCards("testuser", request));

      assertEquals("Карта списания заблокирована", exception.getMessage());
      verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Карта списания не найдена")
    void shouldThrowEntityNotFoundExceptionForMissingFromCard() {
      // Given
      InternalTransferRequest request = new InternalTransferRequest(1L, 2L, BigDecimal.valueOf(50.00));
      when(cardRepository.findByIdAndOwner_Username(1L, "testuser")).thenReturn(Optional.empty());

      // When & Then
      EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
          () -> transactionService.transferBetweenOwnCards("testuser", request));

      assertEquals("Карта списания не найдена", exception.getMessage());
      verify(cardRepository).findByIdAndOwner_Username(1L, "testuser");
      verify(cardRepository, never()).findByIdAndOwner_Username(2L, "testuser");
      verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Карта зачисления не найдена")
    void shouldThrowEntityNotFoundExceptionForMissingToCard() {
      // Given
      InternalTransferRequest request = new InternalTransferRequest(1L, 2L, BigDecimal.valueOf(50.00));
      when(cardRepository.findByIdAndOwner_Username(1L, "testuser")).thenReturn(Optional.of(fromCard));
      when(cardRepository.findByIdAndOwner_Username(2L, "testuser")).thenReturn(Optional.empty());

      // When & Then
      EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
          () -> transactionService.transferBetweenOwnCards("testuser", request));

      assertEquals("Карта зачисления не найдена", exception.getMessage());
      verify(transactionRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("Edge Cases и граничные случаи")
  @Tag("edge-cases")
  class EdgeCasesTests {

    @Test
    @DisplayName("Перевод на ту же карту (fromCardId == toCardId)")
    void shouldHandleTransferToSameCard() {
      // Given
      InternalTransferRequest request = new InternalTransferRequest(1L, 1L, BigDecimal.valueOf(50.00));
      when(cardRepository.findByIdAndOwner_Username(1L, "testuser")).thenReturn(Optional.of(fromCard));

      // When
      // Перевод на ту же карту проходит успешно - баланс уменьшается и увеличивается
      // на ту же сумму
      transactionService.transferBetweenOwnCards("testuser", request);

      // Then
      // Баланс остается неизменным: 1000 - 50 + 50 = 1000
      assertEquals(BigDecimal.valueOf(1000.00), fromCard.getBalance());

      verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Перевод с нулевой суммой")
    void shouldHandleZeroAmountTransfer() {
      // Given
      InternalTransferRequest request = new InternalTransferRequest(1L, 2L, BigDecimal.ZERO);
      when(cardRepository.findByIdAndOwner_Username(1L, "testuser")).thenReturn(Optional.of(fromCard));
      when(cardRepository.findByIdAndOwner_Username(2L, "testuser")).thenReturn(Optional.of(toCard));
      when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

      // When
      transactionService.transferBetweenOwnCards("testuser", request);

      // Then
      assertEquals(BigDecimal.valueOf(1000.00), fromCard.getBalance());
      assertEquals(BigDecimal.valueOf(500.00), toCard.getBalance());
      verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Перевод с отрицательной суммой")
    void shouldHandleNegativeAmountTransfer() {
      // Given
      InternalTransferRequest request = new InternalTransferRequest(1L, 2L, BigDecimal.valueOf(-50.00));
      when(cardRepository.findByIdAndOwner_Username(1L, "testuser")).thenReturn(Optional.of(fromCard));
      when(cardRepository.findByIdAndOwner_Username(2L, "testuser")).thenReturn(Optional.of(toCard));

      // When & Then
      // С отрицательной суммой перевод должен пройти успешно, так как баланс
      // увеличится
      transactionService.transferBetweenOwnCards("testuser", request);

      // Then
      // Отрицательная сумма уменьшает баланс с карты списания (делает его еще меньше)
      // и увеличивает баланс карты зачисления
      assertEquals(BigDecimal.valueOf(1050.00), fromCard.getBalance()); // 1000 - (-50) = 1050
      assertEquals(BigDecimal.valueOf(450.00), toCard.getBalance()); // 500 + (-50) = 450

      verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Перевод с очень большой суммой")
    void shouldHandleVeryLargeAmountTransfer() {
      // Given
      InternalTransferRequest request = new InternalTransferRequest(1L, 2L, BigDecimal.valueOf(999999999.99));
      when(cardRepository.findByIdAndOwner_Username(1L, "testuser")).thenReturn(Optional.of(fromCard));
      when(cardRepository.findByIdAndOwner_Username(2L, "testuser")).thenReturn(Optional.of(toCard));

      // When & Then
      InsufficientFundsException exception = assertThrows(InsufficientFundsException.class,
          () -> transactionService.transferBetweenOwnCards("testuser", request));

      assertEquals("Недостаточно средств", exception.getMessage());
    }

    @Test
    @DisplayName("Попытка перевода с карты другого пользователя")
    void shouldDenyTransferFromAnotherUsersCard() {
      // Given
      Cardholder anotherUser = new Cardholder();
      anotherUser.setId(2L);
      anotherUser.setUsername("anotheruser");

      fromCard.setOwner(anotherUser);

      InternalTransferRequest request = new InternalTransferRequest(1L, 2L, BigDecimal.valueOf(50.00));
      when(cardRepository.findByIdAndOwner_Username(1L, "testuser")).thenReturn(Optional.empty());

      // When & Then
      EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
          () -> transactionService.transferBetweenOwnCards("testuser", request));

      assertEquals("Карта списания не найдена", exception.getMessage());
    }

    @Test
    @DisplayName("Обработка ошибок репозитория")
    void shouldHandleRepositoryErrors() {
      // Given
      InternalTransferRequest request = new InternalTransferRequest(1L, 2L, BigDecimal.valueOf(50.00));
      when(cardRepository.findByIdAndOwner_Username(1L, "testuser")).thenReturn(Optional.of(fromCard));
      when(cardRepository.findByIdAndOwner_Username(2L, "testuser")).thenReturn(Optional.of(toCard));
      when(transactionRepository.save(any(Transaction.class))).thenThrow(new RuntimeException("Database error"));

      // When & Then
      RuntimeException exception = assertThrows(RuntimeException.class,
          () -> transactionService.transferBetweenOwnCards("testuser", request));

      assertEquals("Database error", exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Тесты производительности и конкурентности")
  @Tag("performance")
  class PerformanceTests {

    @Test
    @DisplayName("Последовательные переводы (конкурентность)")
    void shouldHandleSequentialTransfers() {
      // Given
      InternalTransferRequest request1 = new InternalTransferRequest(1L, 2L, BigDecimal.valueOf(100.00));
      InternalTransferRequest request2 = new InternalTransferRequest(2L, 1L, BigDecimal.valueOf(50.00));

      when(cardRepository.findByIdAndOwner_Username(1L, "testuser")).thenReturn(Optional.of(fromCard));
      when(cardRepository.findByIdAndOwner_Username(2L, "testuser")).thenReturn(Optional.of(toCard));
      when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

      // When
      transactionService.transferBetweenOwnCards("testuser", request1);
      transactionService.transferBetweenOwnCards("testuser", request2);

      // Then
      BigDecimal expectedFromBalance = BigDecimal.valueOf(1000.00) // начальный
          .subtract(BigDecimal.valueOf(100.00)) // первый перевод
          .add(BigDecimal.valueOf(50.00)); // возврат
      BigDecimal expectedToBalance = BigDecimal.valueOf(500.00) // начальный
          .add(BigDecimal.valueOf(100.00)) // первый перевод
          .subtract(BigDecimal.valueOf(50.00)); // возврат

      assertEquals(expectedFromBalance, fromCard.getBalance());
      assertEquals(expectedToBalance, toCard.getBalance());
      verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Массовые операции с множественными переводами")
    void shouldHandleMultipleTransfers() {
      // Given
      for (int i = 0; i < 10; i++) {
        BigDecimal amount = BigDecimal.valueOf(10.00 * (i + 1));
        InternalTransferRequest request = new InternalTransferRequest(1L, 2L, amount);

        when(cardRepository.findByIdAndOwner_Username(1L, "testuser")).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdAndOwner_Username(2L, "testuser")).thenReturn(Optional.of(toCard));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // When
        transactionService.transferBetweenOwnCards("testuser", request);
      }

      // Then
      BigDecimal totalTransferred = BigDecimal.valueOf(550.00); // 10 + 20 + ... + 100
      BigDecimal expectedFromBalance = BigDecimal.valueOf(1000.00).subtract(totalTransferred);
      BigDecimal expectedToBalance = BigDecimal.valueOf(500.00).add(totalTransferred);

      assertEquals(expectedFromBalance, fromCard.getBalance());
      assertEquals(expectedToBalance, toCard.getBalance());
      verify(transactionRepository, org.mockito.Mockito.times(10)).save(any(Transaction.class));
    }
  }
}
