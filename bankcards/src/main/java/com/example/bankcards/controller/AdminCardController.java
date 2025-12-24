package com.example.bankcards.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.service.AdminCardService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/cards")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // Доступ только для администраторов
public class AdminCardController {

  private final AdminCardService cardService;

  @GetMapping
  public ResponseEntity<Page<CardResponse>> getAllCards(
      @RequestParam(required = false) String search,
      @ParameterObject Pageable pageable) {
    return ResponseEntity.ok(cardService.findCards(search, pageable));
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
