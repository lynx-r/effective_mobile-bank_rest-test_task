package com.example.bankrest.controller;

import java.math.BigDecimal;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.bankrest.dto.CardResponse;
import com.example.bankrest.dto.InternalTransferRequest;
import com.example.bankrest.service.CardService;
import com.example.bankrest.service.TransactionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class UserCardController {

  private final CardService cardService;
  private final TransactionService transactionService;

  // 1. Просмотр своих карт с пагинацией и поиском по маске/номеру
  @GetMapping("/cards")
  public ResponseEntity<Page<CardResponse>> getMyCards(
      @AuthenticationPrincipal OidcUser principal,
      @RequestParam(required = false) String search,
      @ParameterObject Pageable pageable) {
    return ResponseEntity.ok(cardService.findUserCards(principal.getName(), search, pageable));
  }

  // 2. Запрос на блокировку карты
  @PatchMapping("/cards/{cardId}/block")
  public ResponseEntity<Void> requestBlock(@AuthenticationPrincipal OidcUser principal, @PathVariable Long cardId) {
    cardService.blockOwnCard(principal.getName(), cardId);
    return ResponseEntity.noContent().build();
  }

  // 3. Перевод между своими картами
  @PostMapping("/transfer")
  public ResponseEntity<Void> transfer(@AuthenticationPrincipal OidcUser principal,
      @RequestBody InternalTransferRequest request) {
    transactionService.transferBetweenOwnCards(principal.getName(), request);
    return ResponseEntity.ok().build();
  }

  // 4. Просмотр баланса конкретной карты
  @GetMapping("/cards/{cardId}/balance")
  public ResponseEntity<BigDecimal> getBalance(@AuthenticationPrincipal OidcUser principal, @PathVariable Long cardId) {
    return ResponseEntity.ok(cardService.getUserCardBalance(principal.getName(), cardId));
  }
}
