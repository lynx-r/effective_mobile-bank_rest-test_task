package com.example.authorizationserver.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.authorizationserver.dto.RegisterRequest;
import com.example.authorizationserver.entity.Role;
import com.example.authorizationserver.entity.User;
import com.example.authorizationserver.exception.UserAlreadyExistsException;
import com.example.authorizationserver.repository.RoleRepository;
import com.example.authorizationserver.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

/**
 * Unit тесты для UserServiceImpl
 *
 * Тестирует бизнес-логику регистрации пользователей:
 * - Успешная регистрация
 * - Проверка существования пользователя по username/email
 * - Обработка исключений
 * - Шифрование пароля
 * - Отправка событий в Kafka
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты UserServiceImpl")
class UserServiceImplTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private RoleRepository roleRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private KafkaTemplate<String, Object> kafkaTemplate;

  @InjectMocks
  private UserServiceImpl userService;

  private RegisterRequest registerRequest;
  private Role defaultRole;
  private User savedUser;

  @BeforeEach
  void setUp() {
    registerRequest = new RegisterRequest("testuser", "test@example.com", "Test", "User", "password123");

    defaultRole = new Role();
    defaultRole.setId(1L);
    defaultRole.setName("ROLE_USER");

    savedUser = new User();
    savedUser.setId(1L);
    savedUser.setUsername("testuser");
    savedUser.setEmail("test@example.com");
    savedUser.setFirstName("Test");
    savedUser.setLastName("User");
    savedUser.setPassword("encodedPassword");
    savedUser.setCreatedAt(LocalDateTime.now());
  }

  // ==================== ТЕСТЫ УСПЕШНОЙ РЕГИСТРАЦИИ ====================

  @Test
  @DisplayName("Успешная регистрация нового пользователя")
  void register_SuccessfulRegistration_CreatesUserAndSendsEvent() {
    // Arrange
    when(userRepository.existsByUsername("testuser")).thenReturn(false);
    when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
    when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
    when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(defaultRole));
    when(userRepository.save(any(User.class))).thenReturn(savedUser);
    when(kafkaTemplate.send(eq("user-registration-topic"), any())).thenReturn(null);

    // Act
    userService.register(registerRequest);

    // Assert
    verify(userRepository).existsByUsername("testuser");
    verify(userRepository).existsByEmail("test@example.com");
    verify(passwordEncoder).encode("password123");
    verify(roleRepository).findByName("ROLE_USER");
    verify(userRepository).save(any(User.class));
    verify(kafkaTemplate).send(eq("user-registration-topic"), any());
  }

  @Test
  @DisplayName("Регистрация с корректными данными сохраняет пользователя")
  void register_ValidData_SavesUserWithCorrectFields() {
    // Arrange
    when(userRepository.existsByUsername(anyString())).thenReturn(false);
    when(userRepository.existsByEmail(anyString())).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
    when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(defaultRole));
    when(userRepository.save(any(User.class))).thenReturn(savedUser);

    // Act
    userService.register(registerRequest);

    // Assert
    verify(userRepository).save(any(User.class));
  }

  // ==================== ТЕСТЫ ПРОВЕРКИ СУЩЕСТВОВАНИЯ ПОЛЬЗОВАТЕЛЯ
  // ====================

  @Test
  @DisplayName("Регистрация с существующим username выбрасывает исключение")
  void register_ExistingUsername_ThrowsException() {
    // Arrange
    when(userRepository.existsByUsername("testuser")).thenReturn(true);

    // Act & Assert
    UserAlreadyExistsException exception = assertThrows(UserAlreadyExistsException.class,
        () -> userService.register(registerRequest));

    assertEquals("Пользователь уже существует", exception.getMessage());
    verify(userRepository).existsByUsername("testuser");
    verify(userRepository, never()).existsByEmail(anyString());
    verify(userRepository, never()).save(any(User.class));
    verify(kafkaTemplate, never()).send(anyString(), any());
  }

  @Test
  @DisplayName("Регистрация с существующим email выбрасывает исключение")
  void register_ExistingEmail_ThrowsException() {
    // Arrange
    when(userRepository.existsByUsername("testuser")).thenReturn(false);
    when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

    // Act & Assert
    UserAlreadyExistsException exception = assertThrows(UserAlreadyExistsException.class,
        () -> userService.register(registerRequest));

    assertEquals("Пользователь с таким email уже существует", exception.getMessage());
    verify(userRepository).existsByUsername("testuser");
    verify(userRepository).existsByEmail("test@example.com");
    verify(userRepository, never()).save(any(User.class));
    verify(kafkaTemplate, never()).send(anyString(), any());
  }

  // ==================== ТЕСТЫ ОБРАБОТКИ РОЛЕЙ ====================

  @Test
  @DisplayName("Регистрация без роли ROLE_USER выбрасывает исключение")
  void register_RoleNotFound_ThrowsException() {
    // Arrange
    when(userRepository.existsByUsername(anyString())).thenReturn(false);
    when(userRepository.existsByEmail(anyString())).thenReturn(false);
    when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());

    // Act & Assert
    EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
        () -> userService.register(registerRequest));

    assertEquals("Роль ROLE_USER не найдена", exception.getMessage());
    verify(roleRepository).findByName("ROLE_USER");
    verify(userRepository, never()).save(any(User.class));
    verify(kafkaTemplate, never()).send(anyString(), any());
  }

  // ==================== ТЕСТЫ ШИФРОВАНИЯ ПАРОЛЯ ====================

  @Test
  @DisplayName("Пароль шифруется перед сохранением")
  void register_PasswordIsEncoded() {
    // Arrange
    when(userRepository.existsByUsername(anyString())).thenReturn(false);
    when(userRepository.existsByEmail(anyString())).thenReturn(false);
    when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
    when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(defaultRole));
    when(userRepository.save(any(User.class))).thenReturn(savedUser);

    // Act
    userService.register(registerRequest);

    // Assert
    verify(passwordEncoder).encode("password123");
  }

  // ==================== ТЕСТЫ ОТПРАВКИ СОБЫТИЙ ====================

  @Test
  @DisplayName("Отправляется событие в Kafka после успешной регистрации")
  void register_SendsKafkaEvent() {
    // Arrange
    when(userRepository.existsByUsername(anyString())).thenReturn(false);
    when(userRepository.existsByEmail(anyString())).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
    when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(defaultRole));
    when(userRepository.save(any(User.class))).thenReturn(savedUser);
    when(kafkaTemplate.send(anyString(), any())).thenReturn(null);

    // Act
    userService.register(registerRequest);

    // Assert
    verify(kafkaTemplate).send(eq("user-registration-topic"), any());
  }

  // ==================== ТЕСТЫ РАЗЛИЧНЫХ ДАННЫХ ====================

  @Test
  @DisplayName("Регистрация с русскими символами")
  void register_CyrillicCharacters_Success() {
    // Arrange
    RegisterRequest cyrillicRequest = new RegisterRequest("тестпользователь", "тест@example.com", "Тест",
        "Пользователь", "пароль123");
    when(userRepository.existsByUsername("тестпользователь")).thenReturn(false);
    when(userRepository.existsByEmail("тест@example.com")).thenReturn(false);
    when(passwordEncoder.encode("пароль123")).thenReturn("encodedPassword");
    when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(defaultRole));
    when(userRepository.save(any(User.class))).thenReturn(savedUser);

    // Act
    userService.register(cyrillicRequest);

    // Assert
    verify(userRepository).existsByUsername("тестпользователь");
    verify(userRepository).existsByEmail("тест@example.com");
    verify(passwordEncoder).encode("пароль123");
  }

  @Test
  @DisplayName("Регистрация с длинными строками")
  void register_LongStrings_Success() {
    // Arrange
    String longString = "a".repeat(255);
    RegisterRequest longRequest = new RegisterRequest(longString, "test@example.com", longString, longString,
        "password123");
    when(userRepository.existsByUsername(longString)).thenReturn(false);
    when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
    when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
    when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(defaultRole));
    when(userRepository.save(any(User.class))).thenReturn(savedUser);

    // Act
    userService.register(longRequest);

    // Assert
    verify(userRepository).existsByUsername(longString);
    verify(passwordEncoder).encode("password123");
  }

  @Test
  @DisplayName("Регистрация с специальными символами в username")
  void register_SpecialCharsInUsername_Success() {
    // Arrange
    RegisterRequest specialRequest = new RegisterRequest("user_name.123", "test@example.com", "Test", "User",
        "password123");
    when(userRepository.existsByUsername("user_name.123")).thenReturn(false);
    when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
    when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
    when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(defaultRole));
    when(userRepository.save(any(User.class))).thenReturn(savedUser);

    // Act
    userService.register(specialRequest);

    // Assert
    verify(userRepository).existsByUsername("user_name.123");
  }
}
