package com.example.bankrest.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Сервис для аудита операций с банковскими картами и транзакциями
 * Обеспечивает логирование всех критических операций для соответствия
 * требованиям безопасности
 */
@Service
@Slf4j
public class AuditService {

  /**
   * Логирует операцию создания карты
   */
  public void logCardCreation(String username, Long cardId, String maskedCardNumber, Long cardholderId) {
    log.info("CARD AUDIT - CREATED: user={}, cardId={}, maskedCardNumber={}, cardholderId={}, timestamp={}",
        username, cardId, maskedCardNumber, cardholderId, LocalDateTime.now());
  }

  /**
   * Логирует операцию изменения статуса карты
   */
  public void logCardStatusChange(String username, Long cardId, String fromStatus, String toStatus) {
    log.warn("CARD AUDIT - STATUS_CHANGED: user={}, cardId={}, fromStatus={}, toStatus={}, timestamp={}",
        username, cardId, fromStatus, toStatus, LocalDateTime.now());
  }

  /**
   * Логирует операцию удаления карты
   */
  public void logCardDeletion(String username, Long cardId, String maskedCardNumber) {
    log.warn("CARD AUDIT - DELETED: user={}, cardId={}, maskedCardNumber={}, timestamp={}",
        username, cardId, maskedCardNumber, LocalDateTime.now());
  }

  /**
   * Логирует операцию удаления карты
   */
  public void logCardholderDeletion(String username, Long cardId) {
    log.warn("CARD AUDIT - DELETED: user={}, cardId={}, timestamp={}",
        username, cardId, LocalDateTime.now());
  }

  /**
   * Логирует операцию блокировки карты пользователем
   */
  public void logCardBlocking(String username, Long cardId, String maskedCardNumber) {
    log.warn("CARD AUDIT - BLOCKED_BY_USER: user={}, cardId={}, maskedCardNumber={}, timestamp={}",
        username, cardId, maskedCardNumber, LocalDateTime.now());
  }

  /**
   * Логирует операцию блокировки держателя карт
   */
  public void logCardholderBlocking(String username, Long cardholderId) {
    log.warn("CARD AUDIT - BLOCKED_BY_USER: user={}, cardId={}, timestamp={}",
        username, cardholderId, LocalDateTime.now());
  }

  /**
   * Логирует операцию перевода денег между картами
   */
  public void logTransfer(String username, Long fromCardId, Long toCardId, String fromCardMasked,
      String toCardMasked, String amount, String currency) {
    log.info(
        "TRANSFER AUDIT - EXECUTED: user={}, fromCardId={}, toCardId={}, fromCardMasked={}, toCardMasked={}, amount={}, currency={}, timestamp={}",
        username, fromCardId, toCardId, fromCardMasked, toCardMasked, amount, currency, LocalDateTime.now());
  }

  /**
   * Логирует попытку несанкционированного доступа
   */
  public void logUnauthorizedAccess(String username, String action, String resource, String reason) {
    log.error("SECURITY AUDIT - UNAUTHORIZED_ACCESS: user={}, action={}, resource={}, reason={}, timestamp={}",
        username, action, resource, reason, LocalDateTime.now());
  }

  /**
   * Логирует ошибки валидации
   */
  public void logValidationError(String username, String action, String field, String value, String error) {
    log.warn("VALIDATION AUDIT - ERROR: user={}, action={}, field={}, value={}, error={}, timestamp={}",
        username, action, field, value, error, LocalDateTime.now());
  }

  /**
   * Логирует системные ошибки
   */
  public void logSystemError(String username, String operation, Exception exception) {
    log.error("SYSTEM AUDIT - ERROR: user={}, operation={}, exceptionType={}, exceptionMessage={}, timestamp={}",
        username, operation, exception.getClass().getSimpleName(), exception.getMessage(), LocalDateTime.now(),
        exception);
  }

  /**
   * Логирует операцию просмотра баланса карты
   */
  public void logBalanceView(String username, Long cardId, String maskedCardNumber, BigDecimal balance) {
    log.info("BALANCE AUDIT - VIEWED: user={}, cardId={}, maskedCardNumber={}, balance={}, timestamp={}",
        username, cardId, maskedCardNumber, balance, LocalDateTime.now());
  }

  /**
   * Логирует операцию просмотра списка карт
   */
  public void logCardsListView(String username, Integer pageSize, String searchQuery) {
    log.info("CARDS_LIST AUDIT - VIEWED: user={}, pageSize={}, searchQuery={}, timestamp={}",
        username, pageSize, searchQuery, LocalDateTime.now());
  }

  /**
   * Логирует операцию просмотра списка держателей карт
   */
  public void logCardholdersListView(String username, Integer pageSize, String searchQuery) {
    log.info("CARDHOLDERS_LIST AUDIT - VIEWED: user={}, pageSize={}, searchQuery={}, timestamp={}",
        username, pageSize, searchQuery, LocalDateTime.now());
  }
}
