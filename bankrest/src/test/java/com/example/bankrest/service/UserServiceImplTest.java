package com.example.bankrest.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.bankrest.dto.CardholderResponse;
import com.example.bankrest.entity.Cardholder;
import com.example.bankrest.repository.CardholderRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

  @Mock
  private CardholderRepository cardholderRepository;

  @InjectMocks
  private CardholderServiceImpl userService;

  private Cardholder testCardholder1;
  private Cardholder testCardholder2;

  @BeforeEach
  void setUp() {
    testCardholder1 = new Cardholder();
    testCardholder1.setId(1L);
    testCardholder1.setUsername("user1");
    testCardholder1.setEmail("user1@example.com");
    testCardholder1.setFirstName("John");
    testCardholder1.setLastName("Doe");
    testCardholder1.setCreatedAt(LocalDateTime.now());

    testCardholder2 = new Cardholder();
    testCardholder2.setId(2L);
    testCardholder2.setUsername("user2");
    testCardholder2.setEmail("user2@example.com");
    testCardholder2.setFirstName("Jane");
    testCardholder2.setLastName("Smith");
    testCardholder2.setCreatedAt(LocalDateTime.now());
  }

  @Test
  void findAllUsers_ShouldReturnAllUsers() {
    // Given
    List<Cardholder> cardholders = List.of(testCardholder1, testCardholder2);
    when(cardholderRepository.findAll()).thenReturn(cardholders);

    // When
    List<CardholderResponse> result = userService.findAllUsers();

    // Then
    assertEquals(2, result.size());
    assertEquals("user1", result.get(0).username());
    assertEquals("user2", result.get(1).username());
    assertEquals(true, result.get(0).enabled());
    assertEquals(true, result.get(1).enabled());
    verify(cardholderRepository).findAll();
  }

  @Test
  void findAllUsers_ShouldReturnEmptyList() {
    // Given
    when(cardholderRepository.findAll()).thenReturn(new ArrayList<>());

    // When
    List<CardholderResponse> result = userService.findAllUsers();

    // Then
    assertEquals(0, result.size());
    verify(cardholderRepository).findAll();
  }

  @Test
  void blockUser_ShouldBlockUserSuccessfully() {
    // Given
    when(cardholderRepository.findById(1L)).thenReturn(Optional.of(testCardholder1));
    when(cardholderRepository.save(any(Cardholder.class))).thenReturn(testCardholder1);

    // When
    userService.blockUser(1L);

    // Then
    verify(cardholderRepository).findById(1L);
    verify(cardholderRepository).save(testCardholder1);
  }

  @Test
  void blockUser_ShouldThrowEntityNotFoundException_WhenUserNotFound() {
    // Given
    when(cardholderRepository.findById(1L)).thenReturn(Optional.empty());

    // When & Then
    EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
        () -> userService.blockUser(1L));

    assertEquals("Пользователь не найден", exception.getMessage());
    verify(cardholderRepository).findById(1L);
    verify(cardholderRepository, never()).save(any());
  }

  @Test
  void deleteUser_ShouldDeleteUserSuccessfully() {
    // Given
    doNothing().when(cardholderRepository).deleteById(1L);

    // When
    userService.deleteUser(1L);

    // Then
    verify(cardholderRepository).deleteById(1L);
  }

  @Test
  void deleteUser_ShouldThrowException_WhenUserNotFound() {
    // Given
    doThrow(new RuntimeException("User not found")).when(cardholderRepository).deleteById(1L);

    // When & Then
    RuntimeException exception = assertThrows(RuntimeException.class,
        () -> userService.deleteUser(1L));

    assertEquals("User not found", exception.getMessage());
    verify(cardholderRepository).deleteById(1L);
  }
}
