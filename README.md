# JavaTests

Публичный сайт с тестами по Java и Spring. На главной показано дерево курсов и тем; вопросы темы выдаются в случайном порядке. Регистрация не требуется.

## Локальный запуск

Нужны JDK 21+, Maven 3.9+ и Docker Compose.

```powershell
docker compose up --build
```

Откройте http://localhost:8097. PostgreSQL доступен приложению внутри Compose, а Flyway создаёт схему и начальное дерево курсов при первом запуске.

Для запуска Spring Boot из IDE задайте доступную PostgreSQL базу и при необходимости переопределите DB_URL, DB_USERNAME, DB_PASSWORD, PORT (по умолчанию 8097). Миграции находятся в src/main/resources/db/migration.

Тесты: mvn test.

## Контракты

Соглашения по маршрутам, БД, миграциям и веткам разработки описаны в AGENT.md.
