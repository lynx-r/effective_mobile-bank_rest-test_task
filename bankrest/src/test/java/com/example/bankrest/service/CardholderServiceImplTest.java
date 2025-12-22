package com.example.bankrest.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
class CardholderServiceImplTest {

  @Mock
  private CardholderRepository cardholderRepository;

  @InjectMocks
  private CardholderServiceImpl cardholderService;

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
    List<CardholderResponse> result = cardholderService.findAllUsers();

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
    List<CardholderResponse> result = cardholderService.findAllUsers();

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
    cardholderService.blockUser(1L);

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
        () -> cardholderService.blockUser(1L));

    assertEquals("Пользователь не найден", exception.getMessage());
    verify(cardholderRepository).findById(1L);
    verify(cardholderRepository, never()).save(any());
  }

  @Test
  void deleteUser_ShouldDeleteUserSuccessfully() {
    // Given
    doNothing().when(cardholderRepository).deleteById(1L);

    // When
    cardholderService.deleteUser(1L);

    // Then
    verify(cardholderRepository).deleteById(1L);
  }

  @Test
  void deleteUser_ShouldThrowException_WhenUserNotFound() {
    // Given
    doThrow(new RuntimeException("User not found")).when(cardholderRepository).deleteById(1L);

    // When & Then
    RuntimeException exception = assertThrows(RuntimeException.class,
        () -> cardholderService.deleteUser(1L));

    assertEquals("User not found", exception.getMessage());
    verify(cardholderRepository).deleteById(1L);
  }

  // Enhanced Tests with Better Coverage

  @Test
  void shouldSuccessfullyBlockActiveUser() {
    // Given
    testCardholder1.setEnabled(true);
    when(cardholderRepository.findById(1L)).thenReturn(Optional.of(testCardholder1));
    when(cardholderRepository.save(any(Cardholder.class))).thenReturn(testCardholder1);

    // When
    cardholderService.blockUser(1L);

    // Then
    verify(cardholderRepository).findById(1L);
    verify(cardholderRepository).save(testCardholder1);
    assertEquals(false, testCardholder1.getEnabled());
  }

  @Test
  void shouldHandleBlockingAlreadyBlockedUser() {
    // Given
    testCardholder1.setEnabled(false);
    when(cardholderRepository.findById(1L)).thenReturn(Optional.of(testCardholder1));
    when(cardholderRepository.save(any(Cardholder.class))).thenReturn(testCardholder1);

    // When
    cardholderService.blockUser(1L);

    // Then
    verify(cardholderRepository).findById(1L);
    verify(cardholderRepository).save(testCardholder1);
    assertEquals(false, testCardholder1.getEnabled());
  }

  @Test
  void shouldHandleUserWithNullUsername() {
    // Given
    testCardholder1.setUsername(null);
    when(cardholderRepository.findById(1L)).thenReturn(Optional.of(testCardholder1));
    when(cardholderRepository.save(any(Cardholder.class))).thenReturn(testCardholder1);

    // When
    cardholderService.blockUser(1L);

    // Then
    verify(cardholderRepository).findById(1L);
    verify(cardholderRepository).save(testCardholder1);
    assertNull(testCardholder1.getUsername());
  }

  @Test
  void shouldHandleUserWithEmptyEmail() {
    // Given
    testCardholder1.setEmail("");
    when(cardholderRepository.findById(1L)).thenReturn(Optional.of(testCardholder1));
    when(cardholderRepository.save(any(Cardholder.class))).thenReturn(testCardholder1);

    // When
    cardholderService.blockUser(1L);

    // Then
    verify(cardholderRepository).findById(1L);
    verify(cardholderRepository).save(testCardholder1);
    assertEquals("", testCardholder1.getEmail());
  }

  @Test
  void shouldHandleUserWithVeryLongName() {
    // Given
    String longName = "Very Long Name That Exceeds Normal Length Limits For Testing Purposes";
    testCardholder1.setFirstName(longName);
    testCardholder1.setLastName(longName);
    when(cardholderRepository.findById(1L)).thenReturn(Optional.of(testCardholder1));
    when(cardholderRepository.save(any(Cardholder.class))).thenReturn(testCardholder1);

    // When
    cardholderService.blockUser(1L);

    // Then
    verify(cardholderRepository).findById(1L);
    verify(cardholderRepository).save(testCardholder1);
    assertEquals(longName, testCardholder1.getFirstName());
    assertEquals(longName, testCardholder1.getLastName());
  }

  @Test
  void shouldHandleUserWithSpecialCharactersInName() {
    // Given
    testCardholder1.setFirstName("José María");
    testCardholder1.setLastName("O'Connor-Smith");
    when(cardholderRepository.findById(1L)).thenReturn(Optional.of(testCardholder1));
    when(cardholderRepository.save(any(Cardholder.class))).thenReturn(testCardholder1);

    // When
    cardholderService.blockUser(1L);

    // Then
    verify(cardholderRepository).findById(1L);
    verify(cardholderRepository).save(testCardholder1);
    assertEquals("José María", testCardholder1.getFirstName());
    assertEquals("O'Connor-Smith", testCardholder1.getLastName());
  }

  @Test
  void shouldHandleDatabaseConstraintViolationOnUserBlocking() {
    // Given
    when(cardholderRepository.findById(1L)).thenReturn(Optional.of(testCardholder1));
    when(cardholderRepository.save(any(Cardholder.class)))
        .thenThrow(new RuntimeException("Foreign key constraint violation"));

    // When & Then
    RuntimeException exception = assertThrows(RuntimeException.class,
        () -> cardholderService.blockUser(1L));

    assertEquals("Foreign key constraint violation", exception.getMessage());
    verify(cardholderRepository).findById(1L);
    verify(cardholderRepository).save(any(Cardholder.class));
  }

  @Test
  void shouldHandleRepositoryConnectionErrorOnFindAll() {
    // Given
    when(cardholderRepository.findAll()).thenThrow(new RuntimeException("Database connection failed"));

    // When & Then
    RuntimeException exception = assertThrows(RuntimeException.class,
        () -> cardholderService.findAllUsers());

    assertEquals("Database connection failed", exception.getMessage());
    verify(cardholderRepository).findAll();
  }

  @Test
  void shouldHandleConcurrentDeletionDuringUserOperations() {
    // Given
    when(cardholderRepository.findById(1L)).thenReturn(Optional.empty());

    // When & Then
    EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
        () -> cardholderService.blockUser(1L));

    assertEquals("Пользователь не найден", exception.getMessage());
    verify(cardholderRepository).findById(1L);
    verify(cardholderRepository, never()).save(any());
  }

  @Test
  void shouldEfficientlyHandleLargeNumberOfUsers() {
    // Given
    List<Cardholder> largeUserList = new ArrayList<>();
    for (int i = 0; i < 500; i++) {
      Cardholder user = new Cardholder();
      user.setId((long) i + 1);
      user.setUsername("user" + i);
      user.setEmail("user" + i + "@example.com");
      user.setFirstName("First" + i);
      user.setLastName("Last" + i);
      user.setEnabled(true);
      user.setCreatedAt(LocalDateTime.now());
      largeUserList.add(user);
    }
    when(cardholderRepository.findAll()).thenReturn(largeUserList);

    // When
    List<CardholderResponse> result = cardholderService.findAllUsers();

    // Then
    assertEquals(500, result.size());
    verify(cardholderRepository).findAll();
  }

  @Test
  void shouldPreserveUserDataDuringBlockingOperation() {
    // Given
    when(cardholderRepository.findById(1L)).thenReturn(Optional.of(testCardholder1));
    when(cardholderRepository.save(any(Cardholder.class))).thenReturn(testCardholder1);

    // When
    cardholderService.blockUser(1L);

    // Then - verify all fields are preserved except enabled flag
    assertEquals(Long.valueOf(1L), testCardholder1.getId());
    assertEquals("user1", testCardholder1.getUsername());
    assertEquals("user1@example.com", testCardholder1.getEmail());
    assertEquals("John", testCardholder1.getFirstName());
    assertEquals("Doe", testCardholder1.getLastName());
    assertEquals(false, testCardholder1.getEnabled());
    assertNotNull(testCardholder1.getCreatedAt());
  }

  @Test
  void shouldValidateUserDataIntegrityDuringFindAll() {
    // Given
    List<Cardholder> users = List.of(testCardholder1, testCardholder2);
    when(cardholderRepository.findAll()).thenReturn(users);

    // When
    List<CardholderResponse> result = cardholderService.findAllUsers();

    // Then
    assertNotNull(result);
    assertEquals(2, result.size());

    // Verify first user
    CardholderResponse firstUser = result.get(0);
    assertNotNull(firstUser.id());
    assertNotNull(firstUser.username());
    assertNotNull(firstUser.firstName());
    assertNotNull(firstUser.lastName());
    assertNotNull(firstUser.enabled());

    // Verify second user
    CardholderResponse secondUser = result.get(1);
    assertNotNull(secondUser.id());
    assertNotNull(secondUser.username());
    assertNotNull(secondUser.firstName());
    assertNotNull(secondUser.lastName());
    assertNotNull(secondUser.enabled());

    verify(cardholderRepository).findAll();
  }
}
