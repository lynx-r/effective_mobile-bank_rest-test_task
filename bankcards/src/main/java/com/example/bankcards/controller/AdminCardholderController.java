package com.example.bankcards.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.bankcards.dto.CardholderResponse;
import com.example.bankcards.service.AdminCardholderService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/cardholders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCardholderController {

  private final AdminCardholderService cardholderService;

  @GetMapping
  public ResponseEntity<Page<CardholderResponse>> getAllUsers(
      @RequestParam(required = false) String search,
      @ParameterObject Pageable pageable) {
    return ResponseEntity.ok(cardholderService.findCardholders(search, pageable));
  }

  @PutMapping("/{id}/block")
  public ResponseEntity<Void> blockUser(@PathVariable Long id) {
    cardholderService.blockCardholder(id);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    cardholderService.deleteCardholder(id);
    return ResponseEntity.noContent().build();
  }
}
