# Binance Price Service

Сервис по расписанию получает среднюю цену торговой пары через Binance `/api/v3/avgPrice`, сохраняет её в PostgreSQL и предоставляет REST API. По умолчанию используется пара `BNBUSDT`.

Стек: Java 21, Spring Boot 3.5.7, PostgreSQL, Liquibase, Maven. Тесты: JUnit 5, Mockito, WireMock.

## Запуск

1. Установите JDK 21, Maven и запустите PostgreSQL.
2. Создайте базу данных в PostgreSQL:

```sql
CREATE DATABASE binance_prices;
```

3. В корне проекта задайте пароль PostgreSQL и запустите приложение. Пример для PowerShell:

```powershell
$env:DB_PASSWORD = "ваш_пароль"
mvn spring-boot:run
```

По умолчанию используется пользователь `postgres` и адрес `localhost:5432`. Другие настройки задаются переменными ниже. Liquibase применяет миграции автоматически.

API доступен по адресу `http://localhost:8080`. Первый опрос выполняется после заданного интервала (по умолчанию 30 секунд); следующие — через этот же интервал после завершения предыдущего опроса.

## Переменные окружения

| Переменная | По умолчанию | Назначение |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/binance_prices` | Подключение к БД |
| `DB_USERNAME` | `postgres` | Пользователь БД |
| `DB_PASSWORD` | Нет, обязательна | Пароль БД |
| `SERVER_PORT` | `8080` | Порт приложения |
| `BINANCE_BASE_URL` | `https://api.binance.com` | Адрес Binance API |
| `BINANCE_SYMBOL` | `BNBUSDT` | Торговая пара |
| `BINANCE_POLL_INTERVAL` | `PT30S` | Интервал опроса в ISO-8601, например `PT1M` |

## REST API

Все методы используют настроенную пару `BINANCE_SYMBOL`.

| Метод и путь | Результат |
|---|---|
| `GET /api/v1/prices/latest` | Последняя запись: `symbol`, `price`, `fetchedAt` |
| `GET /api/v1/prices/average?from=...&to=...` | Среднее сохранённых цен: `averagePrice` |
| `GET /api/v1/prices?from=...&to=...&page=0&size=20` | История: `items`, `page`, `size`, `totalElements`, `hasNext` |
| `GET /api/v1/prices/trend?from=...&to=...` | Тренд: `symbol`, `start_date`, `end_date`, `trend` |

`from` и `to` обязательны для запросов за период, формат — ISO-8601 с часовым поясом, например `2026-11-03T00:00:00Z`. Начало включается, конец исключается: `[from, to)`; `from < to`. Время `fetchedAt` возвращается в UTC.

Страницы нумеруются с нуля, размер — от 1 до 100. История отсортирована от новых записей к старым. Тренд сравнивает первую и последнюю цены периода: `UPWARD`, `DOWNWARD` или `FLAT`; для одной записи — `FLAT`.

Примеры вызова в Windows (выберите период, за который уже есть записи):

```powershell
curl.exe "http://localhost:8080/api/v1/prices/latest"
curl.exe "http://localhost:8080/api/v1/prices/average?from=2026-11-03T00:00:00Z&to=2026-11-04T00:00:00Z"
curl.exe "http://localhost:8080/api/v1/prices?from=2026-11-03T00:00:00Z&to=2026-11-04T00:00:00Z&page=0&size=20"
curl.exe "http://localhost:8080/api/v1/prices/trend?from=2026-11-03T00:00:00Z&to=2026-11-04T00:00:00Z"
```

Некорректные параметры дают `400`. При отсутствии данных latest, average и trend возвращают `404`, история — пустой список. Ошибки опроса логируются; после `429` и `418` опрос приостанавливается с учётом `Retry-After`.

## Тесты и сборка

Создайте отдельную базу `binance_prices_test` в PostgreSQL. Тестовый профиль по умолчанию подключается к `localhost:5432` под пользователем `postgres`, пароль берётся из `DB_PASSWORD`. Запустите из корня проекта:

```powershell
mvn "-Dspring.profiles.active=test" verify
```

Профиль `test` нужен для всех тестов, включая проверку запуска контекста. Интеграционные тесты используют PostgreSQL и WireMock вместо Binance. JAR появится в `target/`.
