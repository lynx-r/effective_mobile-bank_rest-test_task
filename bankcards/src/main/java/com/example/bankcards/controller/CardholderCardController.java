package com.example.bankcards.controller;

import java.math.BigDecimal;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.InternalTransferRequest;
import com.example.bankcards.service.CardholderCardService;
import com.example.bankcards.service.TransactionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/cardholder")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class CardholderCardController {

  private final CardholderCardService cardService;
  private final TransactionService transactionService;

  @GetMapping("/cards")
  public ResponseEntity<Page<CardResponse>> getCardholderCards(

      @RequestParam(required = false) String search,
      @ParameterObject Pageable pageable) {
    return ResponseEntity.ok(cardService.findCardholderCards(search, pageable));
  }

  @PatchMapping("/cards/{cardId}/block")
  public ResponseEntity<Void> requestBlockCard(@PathVariable Long cardId) {
    cardService.requestBlockCard(cardId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/transfer")
  public ResponseEntity<Void> transfer(
      @Valid @RequestBody InternalTransferRequest request) {
    transactionService.transferBetweenOwnCards(request);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/cards/{cardId}/balance")
  public ResponseEntity<BigDecimal> getCardholderCardBalance(@PathVariable Long cardId) {
    return ResponseEntity.ok(cardService.getCardholderCardBalance(cardId));
  }
}
