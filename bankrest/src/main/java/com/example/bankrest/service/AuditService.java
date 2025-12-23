package com.example.bankrest.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Сервис для аудита операций с банковскими картами и транзакциями
 * Обеспечивает логирование всех критических операций для соответствия
 * требованиям безопасности
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuditService {

  private final AuthenticationFacade authenticationFacade;

  /**
   * Логирует операцию создания карты
   */
  public void logCardCreation(Long cardId, String maskedCardNumber, Long cardholderId) {
    log.info("CARD AUDIT - CREATED: user={}, cardId={}, maskedCardNumber={}, cardholderId={}, timestamp={}",
        authenticationFacade.getAuthenticationName(), cardId, maskedCardNumber, cardholderId, LocalDateTime.now());
  }

  /**
   * Логирует операцию изменения статуса карты
   */
  public void logCardStatusChange(Long cardId, String fromStatus, String toStatus) {
    log.warn("CARD AUDIT - STATUS_CHANGED: user={}, cardId={}, fromStatus={}, toStatus={}, timestamp={}",
        authenticationFacade.getAuthenticationName(), cardId, fromStatus, toStatus, LocalDateTime.now());
  }

  /**
   * Логирует операцию удаления карты
   */
  public void logCardDeletion(Long cardId, String maskedCardNumber) {
    log.warn("CARD AUDIT - DELETED: user={}, cardId={}, maskedCardNumber={}, timestamp={}",
        authenticationFacade.getAuthenticationName(), cardId, maskedCardNumber, LocalDateTime.now());
  }

  /**
   * Логирует операцию удаления карты
   */
  public void logCardholderDeletion(Long cardId) {
    log.warn("CARDHOLDER AUDIT - DELETED: user={}, cardId={}, timestamp={}",
        authenticationFacade.getAuthenticationName(), cardId, LocalDateTime.now());
  }

  /**
   * Логирует операцию блокировки карты пользователем
   */
  public void logCardBlocking(Long cardId, String maskedCardNumber) {
    log.warn("CARD AUDIT - BLOCKED_BY_USER: user={}, cardId={}, maskedCardNumber={}, timestamp={}",
        authenticationFacade.getAuthenticationName(), cardId, maskedCardNumber, LocalDateTime.now());
  }

  /**
   * Логирует регистрацию держателя карт
   */
  public void logCardholderRegister(Long cardholderId, Long cardId, String maskedCardNumber) {
    log.warn("CARDHOLDER AUDIT - REGISTER: cardholderId={}, cardId={}, maskedCardNumber, timestamp={}",
        cardholderId, cardId, maskedCardNumber, LocalDateTime.now());
  }

  /**
   * Логирует операцию блокировки держателя карт
   */
  public void logCardholderBlocking(Long cardholderId) {
    log.warn("CARDHOLDER AUDIT - BLOCKED_BY_USER: user={}, cardId={}, timestamp={}",
        authenticationFacade.getAuthenticationName(), cardholderId, LocalDateTime.now());
  }

  /**
   * Логирует операцию перевода денег между картами
   */
  public void logTransfer(Long fromCardId, Long toCardId, String fromCardMasked,
      String toCardMasked, String amount, String currency) {
    log.info(
        "TRANSFER AUDIT - EXECUTED: user={}, fromCardId={}, toCardId={}, fromCardMasked={}, toCardMasked={}, amount={}, currency={}, timestamp={}",
        authenticationFacade.getAuthenticationName(), fromCardId, toCardId, fromCardMasked, toCardMasked, amount,
        currency,
        LocalDateTime.now());
  }

  /**
   * Логирует попытку несанкционированного доступа
   */
  public void logUnauthorizedAccess(String action, String resource, String reason) {
    log.error("SECURITY AUDIT - UNAUTHORIZED_ACCESS: user={}, action={}, resource={}, reason={}, timestamp={}",
        authenticationFacade.getAuthenticationName(), action, resource, reason, LocalDateTime.now());
  }

  /**
   * Логирует ошибки валидации
   */
  public void logValidationError(String action, String field, String value, String error) {
    log.warn("VALIDATION AUDIT - ERROR: user={}, action={}, field={}, value={}, error={}, timestamp={}",
        authenticationFacade.getAuthenticationName(), action, field, value, error, LocalDateTime.now());
  }

  /**
   * Логирует системные ошибки
   */
  public void logSystemError(String operation, Exception exception) {
    log.error("SYSTEM AUDIT - ERROR: user={}, operation={}, exceptionType={}, exceptionMessage={}, timestamp={}",
        authenticationFacade.getAuthenticationName(), operation, exception.getClass().getSimpleName(),
        exception.getMessage(),
        LocalDateTime.now(),
        exception);
  }

  /**
   * Логирует операцию просмотра баланса карты
   */
  public void logBalanceView(Long cardId, String maskedCardNumber, BigDecimal balance) {
    log.info("BALANCE AUDIT - VIEWED: user={}, cardId={}, maskedCardNumber={}, balance={}, timestamp={}",
        authenticationFacade.getAuthenticationName(), cardId, maskedCardNumber, balance, LocalDateTime.now());
  }

  /**
   * Логирует операцию просмотра списка карт
   */
  public void logCardsListView(Integer pageSize, String searchQuery) {
    log.info("CARDS_LIST AUDIT - VIEWED: user={}, pageSize={}, searchQuery={}, timestamp={}",
        authenticationFacade.getAuthenticationName(), pageSize, searchQuery, LocalDateTime.now());
  }

  /**
   * Логирует операцию просмотра списка держателей карт
   */
  public void logCardholdersListView(Integer pageSize, String searchQuery) {
    log.info("CARDHOLDERS_LIST AUDIT - VIEWED: user={}, pageSize={}, searchQuery={}, timestamp={}",
        authenticationFacade.getAuthenticationName(), pageSize, searchQuery, LocalDateTime.now());
  }
}
