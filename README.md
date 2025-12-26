# Система управления банковскими картами

## Настройка DNS

Добавьте в `/etc/hosts`:

```/etc/hosts
127.0.0.1	auth-server
127.0.0.1	client-app
```

## Запуск приложений

```bash
# Запуск сервера авторизации
./gradlew authorizationserver:bootRun

# В отдельном терминале - запуск основного модуля после полного запуска сервера авторизации
./gradlew bankcards:bootRun
```

## Проверка работоспособности

```bash
# Проверка основного эндпоинта
curl http://client-app:8080/

# Проверка OpenAPI документации
open http://client-app:8080/swagger-ui/index.html
```

## Документация API

Полная OpenAPI документация доступна по адресам:
- **bankcards**: http://client-app:8080/swagger-ui/index.html
- **authorizationserver**: http://auth-server:9000/swagger-ui/index.html

### Примеры запросов

#### Получение токенов

Перейдите по адресу и авторизуйтесь с логин/паролем user1/p для пользователя или
admin/p для админа

```bash
# 1. Авторизация через браузер
open http://client-app:8080

# 2. Получите токен
open http://client-app:8080/api/token 
```

#### Создание карты

```bash
curl -X POST http://client-app:8080/api/admin/cards \
  -H "Authorization: Bearer {access_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "cardholderId": 2
  }'
```

## Тестирование

```bash
# Все тесты
./gradlew test

# Тесты конкретного модуля
./gradlew bankcards:test
./gradlew authorizationserver:test
```
