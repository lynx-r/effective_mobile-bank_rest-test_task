# API Documentation - Банковская система

## Обзор системы

Банковская REST API система состоит из двух основных модулей:
- **Authorization Server** - аутентификация и регистрация пользователей
- **Bank Cards** - управление картами и транзакциями

## Базовая информация

- **Base URL**: `http://localhost:8080` (по умолчанию)
- **Content-Type**: `application/json`
- **Аутентификация**: JWT токены (Spring Security)
- **Роли пользователей**: 
  - `USER` - обычные пользователи
  - `ADMIN` - администраторы

---

## Authentication Endpoints

### 1. Регистрация пользователя

**POST** `/auth/register`

Регистрирует нового пользователя в системе.

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "username": "string",        // 3-50 символов, обязательно
  "email": "string",           // валидный email, обязательно
  "firstName": "string",       // 3-50 символов, обязательно
  "lastName": "string",        // 3-50 символов, обязательно
  "password": "string"         // минимум 6 символов, обязательно
}
```

**Response:**
```json
"Пользователь успешно зарегистрирован"
```

**Статус коды:**
- `200 OK` - успешная регистрация
- `400 Bad Request` - ошибки валидации данных
- `409 Conflict` - пользователь с таким username или email уже существует

---

## User Endpoints (Роль: USER)

### 2. Получение списка своих карт

**GET** `/api/cardholder/cards`

Возвращает пагинированный список карт текущего пользователя.

**Headers:**
```
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

**Query Parameters:**
- `search` (optional) - поиск по маске номера карты
- `page` (optional) - номер страницы (по умолчанию 0)
- `size` (optional) - размер страницы (по умолчанию 20)
- `sort` (optional) - сортировка (например: `id,desc`)

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "ownerName": "Иван Иванов",
      "cardNumberMasked": "1234 **** **** 5678",
      "status": "ACTIVE",
      "balance": 15000.50,
      "isBlockRequested": false,
      "blockRequestedAt": null,
      "cardholderId": 1
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

### 3. Запрос блокировки карты

**PATCH** `/api/cardholder/cards/{cardId}/block`

Пользователь запрашивает блокировку своей карты.

**Headers:**
```
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

**Path Parameters:**
- `cardId` (required) - ID карты для блокировки

**Response:**
```json
// Пустой ответ с кодом 204 No Content
```

**Статус коды:**
- `204 No Content` - запрос на блокировку принят
- `403 Forbidden` - карта не принадлежит пользователю
- `404 Not Found` - карта не найдена
- `409 Conflict` - карта уже заблокирована

### 4. Перевод между своими картами

**POST** `/api/cardholder/transfer`

Выполняет перевод денег между картами одного пользователя.

**Headers:**
```
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

**Request Body:**
```json
{
  "fromCardId": 1,          // ID карты-отправителя, обязательно
  "toCardId": 2,            // ID карты-получателя, обязательно
  "amount": 1000.00         // сумма перевода, 0.01-1000000000, обязательно
}
```

**Response:**
```json
// Пустой ответ с кодом 204 No Content
```

**Статус коды:**
- `204 No Content` - перевод выполнен
- `400 Bad Request` - ошибки валидации данных
- `403 Forbidden` - карты не принадлежат пользователю
- `404 Not Found` - одна из карт не найдена
- `409 Conflict` - недостаточно средств или карты заблокированы

### 5. Получение баланса карты

**GET** `/api/cardholder/cards/{cardId}/balance`

Возвращает текущий баланс указанной карты.

**Headers:**
```
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

**Path Parameters:**
- `cardId` (required) - ID карты

**Response:**
```json
15000.50
```

**Статус коды:**
- `200 OK` - успешное получение баланса
- `403 Forbidden` - карта не принадлежит пользователю
- `404 Not Found` - карта не найдена

---

## Admin Endpoints (Роль: ADMIN)

### 6. Получение всех карт

**GET** `/api/admin/cards`

Возвращает пагинированный список всех карт в системе (только для администраторов).

**Headers:**
```
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

**Query Parameters:**
- `search` (optional) - поиск по номеру карты или имени владельца
- `page` (optional) - номер страницы (по умолчанию 0)
- `size` (optional) - размер страницы (по умолчанию 20)
- `sort` (optional) - сортировка

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "ownerName": "Иван Иванов",
      "cardNumberMasked": "1234 **** **** 5678",
      "status": "ACTIVE",
      "balance": 15000.50,
      "isBlockRequested": true,
      "blockRequestedAt": "2023-12-01T10:30:00",
      "cardholderId": 1
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

### 7. Создание новой карты

**POST** `/api/admin/cards`

Создает новую карту для указанного пользователя.

**Headers:**
```
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

**Request Body:**
```json
{
  "cardholderId": 1          // ID пользователя, обязательно, положительное число
}
```

**Response:**
```json
{
  "id": 2,
  "ownerName": "Петр Петров",
  "cardNumberMasked": "9876 **** **** 1234",
  "status": "ACTIVE",
  "balance": 0.00,
  "isBlockRequested": false,
  "blockRequestedAt": null,
  "cardholderId": 1
}
```

**Статус коды:**
- `201 Created` - карта успешно создана
- `400 Bad Request` - ошибки валидации данных
- `404 Not Found` - пользователь не найден
- `409 Conflict` - пользователь заблокирован

### 8. Изменение статуса карты

**PATCH** `/api/admin/cards/{id}/status`

Изменяет статус карты (активна/заблокирована/просрочена).

**Headers:**
```
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

**Path Parameters:**
- `id` (required) - ID карты

**Query Parameters:**
- `status` (required) - новый статус карты (ACTIVE/BLOCKED/EXPIRED)

**Response:**
```json
// Пустой ответ с кодом 204 No Content
```

### 9. Удаление карты

**DELETE** `/api/admin/cards/{id}`

Удаляет карту из системы (мягкое удаление).

**Headers:**
```
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

**Path Parameters:**
- `id` (required) - ID карты

**Response:**
```json
// Пустой ответ с кодом 204 No Content
```

### 10. Получение всех пользователей

**GET** `/api/admin/cardholders`

Возвращает пагинированный список всех пользователей системы.

**Headers:**
```
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

**Query Parameters:**
- `search` (optional) - поиск по username, email, имени или фамилии
- `page` (optional) - номер страницы (по умолчанию 0)
- `size` (optional) - размер страницы (по умолчанию 20)
- `sort` (optional) - сортировка

**Response:**
```json
{
  "content": [
    {
      "id": 1,
      "username": "ivanivanov",
      "email": "ivan@example.com",
      "firstName": "Иван",
      "lastName": "Иванов",
      "enabled": true
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

### 11. Блокировка пользователя

**PUT** `/api/admin/cardholders/{id}/block`

Блокирует пользователя в системе.

**Headers:**
```
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

**Path Parameters:**
- `id` (required) - ID пользователя

**Response:**
```json
// Пустой ответ с кодом 204 No Content
```

### 12. Удаление пользователя

**DELETE** `/api/admin/cardholders/{id}`

Удаляет пользователя из системы (мягкое удаление).

**Headers:**
```
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

**Path Parameters:**
- `id` (required) - ID пользователя

**Response:**
```json
// Пустой ответ с кодом 204 No Content
```

---

## Модели данных

### RegisterRequest
Модель для регистрации нового пользователя.

```json
{
  "username": "string",        // 3-50 символов
  "email": "string",           // валидный email
  "firstName": "string",       // 3-50 символов
  "lastName": "string",        // 3-50 символов
  "password": "string"         // минимум 6 символов
}
```

### CreateCardRequest
Модель для создания новой карты.

```json
{
  "cardholderId": 1           // положительное число
}
```

### CardResponse
Модель ответа с информацией о карте.

```json
{
  "id": 1,                    // ID карты
  "ownerName": "string",      // ФИО владельца
  "cardNumberMasked": "string", // замаскированный номер карты
  "status": "ACTIVE",         // ACTIVE/BLOCKED/EXPIRED
  "balance": 15000.50,        // баланс карты
  "isBlockRequested": false,  // запрос на блокировку
  "blockRequestedAt": null,   // дата запроса блокировки
  "cardholderId": 1           // ID владельца карты
}
```

### CardholderResponse
Модель ответа с информацией о пользователе.

```json
{
  "id": 1,                    // ID пользователя
  "username": "string",       // имя пользователя
  "email": "string",          // email
  "firstName": "string",      // имя
  "lastName": "string",       // фамилия
  "enabled": true             // статус активности
}
```

### InternalTransferRequest
Модель для внутреннего перевода между картами.

```json
{
  "fromCardId": 1,            // ID карты-отправителя
  "toCardId": 2,              // ID карты-получателя
  "amount": 1000.00           // сумма перевода (0.01-1000000000)
}
```

### CardStatus
Перечисление статусов карты.

```json
"ACTIVE"    // Активная карта
"BLOCKED"   // Заблокированная карта
"EXPIRED"   // Просроченная карта
```

---

## Пагинация

Все списочные endpoints поддерживают пагинацию через параметры:

- `page` - номер страницы (начинается с 0)
- `size` - количество элементов на странице
- `sort` - сортировка (формат: `поле,направление`)

**Пример запроса:**
```
GET /api/admin/cards?page=0&size=10&sort=id,desc
```

**Структура ответа пагинации:**
```json
{
  "content": [...],           // массив элементов
  "totalElements": 100,       // общее количество элементов
  "totalPages": 10,           // общее количество страниц
  "size": 10,                 // размер страницы
  "number": 0                 // текущий номер страницы
}
```

---

## Обработка ошибок

Система возвращает стандартные HTTP статус коды и структурированные сообщения об ошибках:

**400 Bad Request** - ошибки валидации входных данных
```json
{
  "timestamp": "2023-12-01T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "username",
      "message": "Username cannot be blank"
    }
  ]
}
```

**401 Unauthorized** - отсутствует или недействительный токен
```json
{
  "timestamp": "2023-12-01T10:30:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Full authentication is required to access this resource"
}
```

**403 Forbidden** - недостаточно прав доступа
```json
{
  "timestamp": "2023-12-01T10:30:00",
  "status": 403,
  "error": "Forbidden",
  "message": "Access is denied"
}
```

**404 Not Found** - ресурс не найден
```json
{
  "timestamp": "2023-12-01T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Resource not found"
}
```

**409 Conflict** - конфликт данных
```json
{
  "timestamp": "2023-12-01T10:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "Insufficient funds for transfer"
}
```

**500 Internal Server Error** - внутренняя ошибка сервера
```json
{
  "timestamp": "2023-12-01T10:30:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred"
}
```

---

## Безопасность

### Аутентификация
Все endpoints (кроме регистрации) требуют JWT токен в заголовке:
```
Authorization: Bearer {jwt_token}
```

### Авторизация
- **USER** - доступ к собственным картам и операциям
- **ADMIN** - полный доступ ко всем ресурсам системы

### Валидация данных
- Все входные данные валидируются на сервере
- Используются аннотации Jakarta Validation (@NotNull, @Size, @Email, etc.)
- Сообщения об ошибках возвращаются на русском языке

### Шифрование
- Номера карт хранятся в зашифрованном виде
- В API возвращаются только замаскированные номера
- Используется кастомная криптография для защиты чувствительных данных

---

## Примечания

1. **Нумерация карт**: Полные номера карт никогда не возвращаются в API для безопасности
2. **Транзакции**: В данной версии API поддерживаются только внутренние переводы между картами одного пользователя
3. **Аудит**: Все операции с картами и пользователями логируются в системе аудита
4. **Масштабируемость**: API поддерживает пагинацию для работы с большими объемами данных
5. **Совместимость**: API следует REST принципам и стандартам OpenAPI

---

*Документация создана на основе анализа исходного кода проекта*
*Дата создания: 2023-12-01*
