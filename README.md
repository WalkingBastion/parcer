# Парсер вакансий

Многопоточный сервис на базе Spring Boot (MVC), который загружает страницы с описанием вакансий, извлекает из них **название вакансии, компанию, заработную плату, требования, город и дату публикации**, сохраняет эти данные во встроенной базе данных H2 и предоставляет доступ к ним через эндпоинт с поддержкой сортировки и пагинации.

## Требования

* JDK 17 или новее
* Maven 3.6+ (для первой сборки требуется доступ в интернет, чтобы загрузить зависимости)

Предварительная настройка базы данных, внешних сервисов или конфигурационных файлов не требуется.

## Запуск

```bash
mvn spring-boot:run
```

Или альтернативный вариант:

```bash
mvn clean package
java -jar target/vacancy-parser-1.0.0.jar
```

Приложение доступно по адресу `http://localhost:8080`.

При запуске приложение парсит девять **встроенных демонстрационных страниц** (`/demo/...`, которые обслуживаются самим приложением), что позволяет проверить логику работы в офлайн-режиме. Убедитесь, что в логах появилась строка `Demo seed finished: 9 of 9 pages parsed`, либо откройте `http://localhost:8080/answer`.

## API

### POST /post

Загружает и парсит одну или несколько страниц с вакансиями (параллельно), сохраняет полученные данные и возвращает их.
Принимает параметры `url` или `urls` (можно использовать оба). Дубликаты обрабатываются однократно; если URL уже существует в базе, данные по нему обновляются. Запрос:

```json
{
"urls": [
"http://localhost:8080/demo/hh.html",
"http://localhost:8080/demo/superjob.html",
"http://localhost:8080/demo/habr.html",
"http://localhost:8080/demo/vacancy/101",
"http://localhost:8080/demo/vacancy/102",
"http://localhost:8080/demo/vacancy/103",
"http://localhost:8080/demo/vacancy/104",
"http://localhost:8080/demo/vacancy/105"
]
}
```

curl (Linux / macOS / Git Bash):

```bash
curl -X POST http://localhost:8080/post \
-H "Content-Type: application/json" \
-d '{"urls":["http://localhost:8080/demo/hh.html","http://localhost:8080/demo/vacancy/101","http://localhost:8080/demo/vacancy/102","http://localhost:8080/demo/vacancy/103"]}'
```

PowerShell:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/post -ContentType "application/json" `
-Body '{"url":"http://localhost:8080/demo/habr.html"}'
```

Формат для одного URL: `{"url": "https://example.com/vacancy/1"}`. Ответ (сокращенный):

```json
{
"requested": 4,
"succeeded": 4,
"failed": 0,
"elapsedMs": 612,
"results": [
{
"url": "http://localhost:8080/demo/hh.html",
"success": true,
"error": null,
"processedBy": "parser-worker-1",
"vacancy": {
"id": 1,
"title": "Java-разработчик (Spring Boot)",
"company": "Демо Технологии",
"salary": "от 250 000 до 350 000 ₽ на руки",
"requirements": "Требования: ...",
"city": "Москва",
"postedDate": "2026-10-12"
}
}
]
}
```

Для страниц `/demo/vacancy/{id}` предусмотрена искусственная задержка в 400 мс (ее можно изменить с помощью параметра `?delayMs=`). Обработка 8 таких страниц занимает примерно 400–800 мс (вместо 3,2 с), а в поле `processedBy` отображаются разные потоки `parser-worker-N`.

Некорректные URL-адреса не приводят к сбою всего запроса: информация о них возвращается для каждого элемента отдельно с флагом `success: false`.
При отправке некорректных запросов (например, с пустым телом) возвращается HTTP-статус 400 и сообщение об ошибке в формате `{"error": "..."}`.

### GET /answer

Считывает вакансии из базы данных H2, выполняет их фильтрацию, сортировку и агрегацию с помощью `parallelStream()` (в отдельном пуле потоков `ForkJoinPool`).

| Параметр    | Значение по умолчанию | Описание |
|-------------|-----------------------|----------|
| `page`      | `0`                   | индекс страницы (начинается с 0) |
| `size`      | `10`                  | размер страницы (от 1 до 100) |
| `sortBy`    | `id`                  | `id`, `title`, `company`, `city`, `salary`, `postedDate`, `createdAt`, `updatedAt` |
| `direction` | `asc`   | `asc` или `desc` |
| `city`      | -       | фильтрация по подстроке (без учета регистра) |
| `company`   | -       | фильтрация по подстроке (без учета регистра) |
| `q`         | -       | поиск по подстроке в названии (title) и требованиях (requirements) |

Примеры:

```bash
curl "http://localhost:8080/answer"
curl "http://localhost:8080/answer?sortBy=postedDate&direction=desc&size=5"
curl "http://localhost:8080/answer?sortBy=title&page=1&size=3"
curl "http://localhost:8080/answer?city=berlin&q=java"
```

Ответ содержит поле `content`, данные о пагинации и `cityCounts` (количество вакансий по городам, рассчитанное с помощью `groupingByConcurrent`).

### GET /stats

Счетчики, реализованные с использованием потокобезопасных структур данных: количество успешных операций, ошибок, выполненных запланированных обновлений, время последнего запуска, а также количество задач, обработанных каждым рабочим потоком.

### Консоль H2

`http://localhost:8080/h2-console`, JDBC URL: `jdbc:h2:mem:vacancies`, пользователь: `sa`, пароль: пустой.
Таблица: `VACANCIES`.

## Проверка запланированной задачи (scheduled job)

Через 20 секунд после запуска, а затем каждые 60 секунд, `VacancyRefreshJob` выполняет повторную загрузку данных по всем сохраненным URL-адресам в параллельном режиме. В логах отображается сообщение: `Scheduled refresh finished: N vacancies re-checked`. В ответе `GET /stats` увеличивается значение `refreshRuns`, а в `GET /answer?sortBy=updatedAt&direction=desc` появляются обновленные значения поля `updatedAt`.

## Реализация требований к многопоточности

| Требование | Реализация |
|------------|------------|
| Использование собственного `ExecutorService` / `ForkJoinPool` | `ConcurrencyConfig`: `parserExecutor` (`ThreadPoolEx...`) | `Executor` (с именованными рабочими потоками и ограниченной очередью) выполняет задачи по парсингу; `queryPool` (`ForkJoinPool`) используется для параллельных потоков (`parallel streams`) |
| Планирование выполнения | `VacancyRefreshJob` с использованием аннотаций `@Scheduled` и `@EnableScheduling` |
| Встроенная БД H2 | Spring Data JPA, H2 в оперативной памяти, сущность `Vacancy` |
| Отдельный эндпоинт для получения результатов (с поддержкой пагинации и сортировки) | `GET /answer` |
| Извлечение и сортировка данных через `parallelStream()` | `VacancyQueryService` |
| Взаимодействие между сервисами | `PageFetcher` использует `WebClient` из состава WebFlux |
| Потокобезопасные структуры | `LongAdder`, `ConcurrentHashMap`, `AtomicReference` в `StatsRegistry`; массив объектов `ReentrantLock` (техника «расслоения»/striping) в `VacancyStore` для предотвращения дублирования записей при одновременной обработке одного и того же URL; `groupingByConcurrent` в запросах; `CompletableFuture` для композиции задач |

## Принцип извлечения данных

Источники применяются последовательно: для каждого поля выбирается первое найденное непустое значение:

1. Источники на основе CSS-селекторов для hh.ru, SuperJob и Habr Career (`SourcesConfig`).
2. Данные в формате JSON-LD (схема `JobPosting` от schema.org), которые часто присутствуют на сайтах вакансий (`JsonLdSource`).
3. Универсальные источники: Open Graph или содержимое тега `<title>` (в качестве резервного варианта — `FallbackSource`).

Даты публикации приводятся к единому формату `yyyy-MM-dd` (поддерживаются ISO, текстовые форматы вроде «12 октября 2026» и числовые — «05.10.2026»).

Примечание: поскольку структура разметки на реальных сайтах может меняться, а также могут применяться средства защиты от ботов (капча, ошибки 403, требование авторизации), работа селекторов не гарантируется на 100%. В случае блокировки страницы соответствующий элемент помечается как «неудачный» (failed). Для проверки корректности логики лучше всего использовать встроенные демонстрационные страницы.

## Конфигурация (все параметры — опциональны)

`src/main/resources/application.properties`:

| Параметр | Значение по умолчанию | Описание |
|----------|---------|---------|
| `app.pool.core` / `app.pool.max` / `app.pool.queue` | 8 / 16 / 200 | пул потоков для парсинга |
| `app.pool.query-parallelism` | 4 | параллелизм запросов в `ForkJoinPool` |
| `app.fetch.timeout-ms` | 10000 | таймаут HTTP-запроса |
| `app.fetch.max-urls-per-request` | 100 | лимит для `POST /post` |
| `app.refresh.initial-delay-ms` / `app.refresh.interval-ms` | 20000 / 60000 | параметры запланированного обновления |
| `app.seed-demo` | true | парсинг демонстрационных страниц при запуске |
