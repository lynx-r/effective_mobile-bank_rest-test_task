package com.example.bankrest.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.bankrest.dto.CardResponse;
import com.example.bankrest.dto.CreateCardRequest;
import com.example.bankrest.entity.CardStatus;
import com.example.bankrest.service.CardService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/cards")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // Доступ только для администраторов
public class AdminCardController {

  private final CardService cardService;

  @GetMapping
  public ResponseEntity<List<CardResponse>> getAllCards() {
    return ResponseEntity.ok(cardService.findAllCards());
  }

  @PostMapping
  public ResponseEntity<CardResponse> createCard(@Valid @RequestBody CreateCardRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(cardService.createCard(request));
  }

  @PatchMapping("/{id}/status")
  public ResponseEntity<Void> updateCardStatus(@PathVariable Long id, @RequestParam CardStatus status) {
    cardService.updateStatus(id, status);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
    cardService.deleteCard(id);
    return ResponseEntity.noContent().build();
  }
}
