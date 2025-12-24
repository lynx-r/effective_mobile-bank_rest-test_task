# План обновления серверов в Swagger на http://client-app:8080

## Информация, полученная из анализа:

### 1. docs/openapi.yaml
- **Текущий сервер**: `http://client-app:8080` ✅ (уже правильно)
- **Описание**: "Локальный сервер разработки"

### 2. OpenApiConfig.java  
- **Текущий сервер**: `http://client-app:8080` ✅ (уже правильно)
- **Описание**: "Локальный сервер разработки"

### 3. application.yaml (CORS настройки)
- **БЫЛО**: 
  - `http://localhost:8080` ❌
  - `http://client-api:8080` ❌
- **СТАЛО**: `http://client-app:8080` ✅

### 4. Другие файлы
- AppSecurityConfig.java: `http://client-app:8080` ✅ (уже правильно)
- authorizationserver/application.yaml: `http://client-app:8080` ✅ (уже правильно)

## Выполненные действия:

1. **✅ ОБНОВЛЕНО**: CORS настройки в application.yaml
   - Заменены `http://localhost:8080` и `http://client-api:8080` на `http://client-app:8080`

2. **✅ ПРОВЕРЕНО**: application-prod.yaml
   - CORS настройки отсутствуют (использует наследование из основного application.yaml)

3. **✅ ВЕРИФИЦИРОВАНО**: Все серверы в проекте
   - Все ссылки указывают на `http://client-app:8080`
   - Устаревшие ссылки на localhost:8080 и client-api:8080 не найдены

## Статус: ✅ ЗАВЕРШЕНО
