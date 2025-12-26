package com.example.authorizationserver.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.CharacterEncodingFilter;

import com.example.authorizationserver.dto.RegisterRequest;
import com.example.authorizationserver.exception.GlobalExceptionHandler;
import com.example.authorizationserver.exception.UserAlreadyExistsException;
import com.example.authorizationserver.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unit тесты для RegistrationController
 *
 * Тестирует REST API для регистрации пользователей:
 * - Успешная регистрация
 * - Обработка ошибок валидации
 * - Обработка бизнес-исключений
 */
@DisplayName("Тесты RegistrationController")
class RegistrationControllerTest {

  @Mock
  private UserService userService;

  @InjectMocks
  private RegistrationController registrationController;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    mockMvc = MockMvcBuilders.standaloneSetup(registrationController)
        .addFilters(new CharacterEncodingFilter("UTF-8", true)) // Форсируем UTF-8
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
    objectMapper = new ObjectMapper();
  }

  // ==================== ТЕСТЫ УСПЕШНОЙ РЕГИСТРАЦИИ ====================

  @Test
  @DisplayName("Успешная регистрация пользователя")
  void register_ValidRequest_ReturnsOk() throws Exception {
    // Arrange
    RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "Test", "User", "password123");
    doNothing().when(userService).register(any(RegisterRequest.class));

    // Act & Assert
    mockMvc.perform(post("/auth/register")
        .characterEncoding("UTF-8")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Пользователь успешно зарегистрирован")));

    verify(userService).register(any(RegisterRequest.class));
  }

  @Test
  @DisplayName("Регистрация с корректными данными")
  void register_ValidData_CallsService() throws Exception {
    // Arrange
    RegisterRequest request = new RegisterRequest("john_doe", "john@example.com", "John", "Doe", "securePass123");
    doNothing().when(userService).register(any(RegisterRequest.class));

    // Act
    mockMvc.perform(post("/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());

    // Assert
    verify(userService).register(any(RegisterRequest.class));
  }

  // ==================== ТЕСТЫ ВАЛИДАЦИИ ====================

  @Test
  @DisplayName("Регистрация с пустым username")
  void register_EmptyUsername_ReturnsBadRequest() throws Exception {
    // Arrange
    RegisterRequest request = new RegisterRequest("", "test@example.com", "Test", "User", "password123");

    // Act & Assert
    mockMvc.perform(post("/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Регистрация с null username")
  void register_NullUsername_ReturnsBadRequest() throws Exception {
    // Arrange
    String requestJson = """
        {
          "username": null,
          "email": "test@example.com",
          "firstName": "Test",
          "lastName": "User",
          "password": "password123"
        }
        """;

    // Act & Assert
    mockMvc.perform(post("/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(requestJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Регистрация с пустым email")
  void register_EmptyEmail_ReturnsBadRequest() throws Exception {
    // Arrange
    RegisterRequest request = new RegisterRequest("testuser", "", "Test", "User", "password123");

    // Act & Assert
    mockMvc.perform(post("/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Регистрация с некорректным email")
  void register_InvalidEmail_ReturnsBadRequest() throws Exception {
    // Arrange
    RegisterRequest request = new RegisterRequest("testuser", "invalid-email", "Test", "User", "password123");

    // Act & Assert
    mockMvc.perform(post("/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Регистрация с пустым firstName")
  void register_EmptyFirstName_ReturnsBadRequest() throws Exception {
    // Arrange
    RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "", "User", "password123");

    // Act & Assert
    mockMvc.perform(post("/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Регистрация с пустым lastName")
  void register_EmptyLastName_ReturnsBadRequest() throws Exception {
    // Arrange
    RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "Test", "", "password123");

    // Act & Assert
    mockMvc.perform(post("/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Регистрация с пустым password")
  void register_EmptyPassword_ReturnsBadRequest() throws Exception {
    // Arrange
    RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "Test", "User", "");

    // Act & Assert
    mockMvc.perform(post("/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  // ==================== ТЕСТЫ БИЗНЕС-ИСКЛЮЧЕНИЙ ====================

  @Test
  @DisplayName("Регистрация существующего пользователя по username")
  void register_ExistingUsername_ThrowsException() throws Exception {
    // Arrange
    RegisterRequest request = new RegisterRequest("existinguser", "new@example.com", "Test", "User", "password123");
    doThrow(new UserAlreadyExistsException("Пользователь уже существует"))
        .when(userService).register(any(RegisterRequest.class));

    // Act & Assert
    mockMvc.perform(post("/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("Регистрация существующего пользователя по email")
  void register_ExistingEmail_ThrowsException() throws Exception {
    // Arrange
    RegisterRequest request = new RegisterRequest("newuser", "existing@example.com", "Test", "User", "password123");
    doThrow(new UserAlreadyExistsException("Пользователь с таким email уже существует"))
        .when(userService).register(any(RegisterRequest.class));

    // Act & Assert
    mockMvc.perform(post("/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict());
  }

  // ==================== ТЕСТЫ НЕКОРРЕКТНОГО JSON ====================

  @Test
  @DisplayName("Регистрация с некорректным JSON")
  void register_InvalidJson_ReturnsBadRequest() throws Exception {
    // Arrange
    String invalidJson = "{ invalid json }";

    // Act & Assert
    mockMvc.perform(post("/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(invalidJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("Регистрация с пустым телом запроса")
  void register_EmptyBody_ReturnsBadRequest() throws Exception {
    // Act & Assert
    mockMvc.perform(post("/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(""))
        .andExpect(status().isBadRequest());
  }

  // ==================== ТЕСТЫ РАЗЛИЧНЫХ ДАННЫХ ====================

  @Test
  @DisplayName("Регистрация с длинными строками")
  void register_LongStrings_Valid() throws Exception {
    // Arrange
    String longString = "a".repeat(255);
    RegisterRequest request = new RegisterRequest(longString, "test@example.com", longString, longString,
        "password123");
    doNothing().when(userService).register(any(RegisterRequest.class));

    // Act & Assert
    mockMvc.perform(post("/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(userService);
  }

  @Test
  @DisplayName("Регистрация с специальными символами в username")
  void register_SpecialCharsInUsername_Valid() throws Exception {
    // Arrange
    RegisterRequest request = new RegisterRequest("user_name.123", "test@example.com", "Test", "User", "password123");
    doNothing().when(userService).register(any(RegisterRequest.class));

    // Act & Assert
    mockMvc.perform(post("/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());

    verify(userService).register(any(RegisterRequest.class));
  }

  @Test
  @DisplayName("Регистрация с русскими символами")
  void register_CyrillicCharacters_Valid() throws Exception {
    // Arrange
    RegisterRequest request = new RegisterRequest("тестпользователь", "тест@example.com", "Тест", "Пользователь",
        "пароль123");
    doNothing().when(userService).register(any(RegisterRequest.class));

    // Act & Assert
    mockMvc.perform(post("/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());

    verify(userService).register(any(RegisterRequest.class));
  }
}
