# Интеграция OpenAPI 3 с Spring Boot

## Обзор

Данный проект настроен для автоматической генерации интерактивной API документации с помощью SpringDoc OpenAPI 3 и Swagger UI.

## Автоматически доступные endpoints

После запуска приложения будут доступны:

### 1. Swagger UI (Интерактивная документация)
- **URL**: `http://localhost:8080/swagger-ui.html`
- **Описание**: Интерактивная документация API с возможностью тестирования запросов

### 2. OpenAPI JSON спецификация
- **URL**: `http://localhost:8080/v3/api-docs`
- **Описание**: JSON спецификация API в формате OpenAPI 3.0

### 3. OpenAPI YAML спецификация
- **URL**: `http://localhost:8080/v3/api-docs.yaml`
- **Описание**: YAML спецификация API в формате OpenAPI 3.0

## Конфигурация

### 1. Зависимости (уже добавлены в build.gradle)
```gradle
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.0'
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-api:3.0.0'
```

### 2. Конфигурация приложения (application.yaml)
```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
    enabled: true
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
    operations-sorter: method
    tags-sorter: alpha
    filter: true
    tryItOutEnabled: true
    displayRequestDuration: true
    docExpansion: none
    showExtensions: true
    showCommonExtensions: true
    defaultModelsExpandDepth: 2
    defaultModelExpandDepth: 2
    persistAuthorization: true
  packages-to-scan: 
    - com.example.bankcards.controller
    - com.example.authorizationserver.controller
  show-actuator: true
```

### 3. Конфигурационный класс OpenAPI
Создан класс `OpenApiConfig.java` который настраивает:
- Общую информацию об API
- Серверы (локальный, продакшн)
- Схему безопасности (JWT Bearer)
- Контактную информацию

## Использование OpenAPI аннотаций

### Базовые аннотации для контроллеров

#### 1. Теги (Tags)
```java
@Tag(name = "Admin Cards", description = "Административные endpoints для управления картами")
@RestController
@RequestMapping("/api/admin/cards")
public class AdminCardController {
    // ...
}
```

#### 2. Операции (Operations)
```java
@Operation(
    summary = "Получение всех карт",
    description = "Возвращает пагинированный список всех карт в системе (только для администраторов)",
    operationId = "getAllCards"
)
@SecurityRequirement(name = "Bearer Authentication")
@GetMapping
public ResponseEntity<Page<CardResponse>> getAllCards(
    @Parameter(description = "Поисковый запрос") @RequestParam(required = false) String search,
    @ParameterObject Pageable pageable) {
    // ...
}
```

#### 3. Параметры
```java
@Parameter(
    name = "id",
    description = "ID карты",
    required = true,
    example = "1"
)
@PathVariable Long id
```

#### 4. Ответы API
```java
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Список всех карт"),
    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ"),
    @ApiResponse(responseCode = "403", description = "Недостаточно прав доступа")
})
```

#### 5. Схемы (Schemas)
```java
@Schema(description = "Модель ответа с информацией о карте")
public record CardResponse(
    @Schema(description = "Уникальный идентификатор карты", example = "1") Long id,
    @Schema(description = "ФИО владельца", example = "Иван Иванов") String ownerName,
    // ...
) {}
```

### Пример аннотированного контроллера

```java
@Tag(name = "Admin Cards", description = "Административные endpoints для управления картами")
@RestController
@RequestMapping("/api/admin/cards")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCardController {

    private final AdminCardService cardService;

    @Operation(
        summary = "Получение всех карт",
        description = "Возвращает пагинированный список всех карт в системе (только для администраторов)",
        operationId = "getAllCards"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список всех карт"),
        @ApiResponse(responseCode = "401", description = "Неавторизованный доступ"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав доступа")
    })
    @GetMapping
    public ResponseEntity<Page<CardResponse>> getAllCards(
            @Parameter(description = "Поисковый запрос") @RequestParam(required = false) String search,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(cardService.findCards(search, pageable));
    }

    @Operation(
        summary = "Создание новой карты",
        description = "Создает новую карту для указанного пользователя",
        operationId = "createCard"
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Карта успешно создана"),
        @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    @PostMapping
    public ResponseEntity<CardResponse> createCard(
            @Valid @RequestBody CreateCardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cardService.createCard(request));
    }
}
```

## Функции Swagger UI

### 1. Интерактивное тестирование
- Возможность выполнения запросов прямо из браузера
- Автоматическая генерация примеров запросов
- Поддержка аутентификации (JWT Bearer token)

### 2. Навигация и поиск
- Группировка endpoints по тегам
- Фильтрация по тегам
- Поиск по операциям

### 3. Детализация
- Подробное описание каждого endpoint
- Схемы данных с примерами
- Коды ответов и их описания

### 4. Аутентификация
- Встроенная поддержка Bearer Token
- Возможность сохранения токенов между сессиями
- Автоматическое добавление заголовков Authorization

## Примеры использования

### 1. Просмотр документации
Откройте `http://localhost:8080/swagger-ui.html` в браузере

### 2. Тестирование API
1. Сначала зарегистрируйтесь через `/auth/register`
2. Получите JWT токен
3. В Swagger UI нажмите "Authorize" и введите токен в формате: `Bearer {your_token}`
4. Тестируйте endpoints

### 3. Экспорт спецификации
- JSON: `http://localhost:8080/v3/api-docs`
- YAML: `http://localhost:8080/v3/api-docs.yaml`

## Настройка для продакшена

### Отключение в продакшене
```yaml
springdoc:
  swagger-ui:
    enabled: false
  api-docs:
    enabled: false
```

### Кастомная конфигурация
Можно настроить различные параметры через `application.yaml` или программно через `OpenApiConfig`.

## Дополнительные возможности

### 1. Кастомные схемы безопасности
```java
@SecurityScheme(
    name = "Bearer Authentication",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
```

### 2. Серверная конфигурация
```java
@Server(url = "https://api.bank.com", description = "Продакшн сервер")
```

### 3. Примеры данных
```java
@Schema(example = "ivanivanov")
private String username;
```

### 4. Скрытие полей
```java
@Schema(hidden = true)
private String internalField;
```

## Решение проблем

### 1. Документация не отображается
- Проверьте, что контроллеры находятся в пакетах из `packages-to-scan`
- Убедитесь, что зависимости SpringDoc добавлены в build.gradle

### 2. Ошибки 401 при тестировании
- Убедитесь, что добавили Bearer token в Authorization header
- Проверьте, что токен не истек

### 3. Некорректные схемы данных
- Добавьте `@Schema` аннотации к DTO классам
- Проверьте, что используются правильные типы данных

## Лучшие практики

1. **Всегда документируйте endpoints** с помощью `@Operation`
2. **Используйте описательные summary и description**
3. **Добавляйте примеры** с помощью `@Schema(example = "...")`
4. **Группируйте endpoints** с помощью тегов
5. **Документируйте все возможные ответы** с `@ApiResponses`
6. **Поддерживайте актуальность документации** при изменении API

---

*Интеграция OpenAPI настроена и готова к использованию!*
