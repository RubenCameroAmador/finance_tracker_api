# PocketMinder — Architecture Document

> **Version:** 1.0  
> **Last updated:** 2026-06-03  
> **Scope:** Backend monolith (`com.pocketminder:pocketminder:0.0.1-SNAPSHOT`)  

---

## Table of Contents

1. [Product Vision](#product-vision)
2. [Business Domain](#business-domain)
3. [Core Capabilities](#core-capabilities)
4. [Architectural Principles](#architectural-principles)
5. [Current Architecture](#current-architecture)
6. [Module Structure](#module-structure)
7. [Layer Responsibilities](#layer-responsibilities)
8. [SMS Ingestion Flow](#sms-ingestion-flow)
9. [Transaction Processing Flow](#transaction-processing-flow)
10. [Database Architecture](#database-architecture)
11. [Security Architecture](#security-architecture)
12. [External Integrations](#external-integrations)
13. [Error Handling Strategy](#error-handling-strategy)
14. [Logging Strategy](#logging-strategy)
15. [Testing Strategy](#testing-strategy)
16. [Technical Debt](#technical-debt)
17. [Future Evolution](#future-evolution)
18. [Repository Evidence](#repository-evidence)

---

## Product Vision

> *From `README.md`: "PocketMinder is a modern personal finance platform focused on helping users easily track expenses, manage income, build saving habits, and receive AI-powered financial insights. Most financial apps fail because they require too much manual effort. PocketMinder aims to solve that problem by combining expense tracking, income management, savings goals, smart categorization, financial analytics, AI-powered recommendations, and automation-first UX."*

The system is designed to evolve from a manual transaction tracker into an **automated financial intelligence platform** that ingests transactions from SMS, bank APIs, and CSV imports, categorizes them intelligently, and provides actionable insights.

---

## Business Domain

PocketMinder operates in the **Personal Financial Management (PFM)** domain. The core business sub-domains are:

| Sub-domain | Description | Strategic Classification |
|---|---|---|
| **Identity & Access** | User registration, authentication, session management | Supporting |
| **Transaction Management** | Recording, updating, and querying financial transactions | Core |
| **Transaction Ingestion** | Automatic capture from SMS, bank APIs, CSV, email | Core |
| **Transaction Categorization** | Assigning categories to transactions via rules, ML, or user input | Core |
| **Financial Analytics** | Aggregating data into summaries, trends, and reports | Supporting |
| **AI Insights** | Generating personalized financial recommendations | Core (future) |
| **User Profile** | Managing user preferences and settings | Generic |

**Bounded context candidates** (per current package boundaries): `identity`, `transaction`, `ingestion`, `categorization`, `analytics`, `ai`.

---

## Core Capabilities

Identified from source code analysis of all 43 Java files:

1. **User Registration & Login** — `AuthController.java`, `AuthService.java`, JWT issuance
2. **JWT-authenticated Session Management** — `SecurityConfig.java`, `JwtAuthenticationFilter.java`, `JwtService.java`
3. **Manual Transaction Creation & Listing** — `TransactionController.java`, `TransactionService.java`, `TransactionRepository.java`
4. **SMS-based Transaction Ingestion** — `SmsIngestionController.java`, `SmsIngestionService.java`, `SmsParserFactory.java`, `BancolombiaSmsParser.java`
5. **Financial Summary (Income/Expenses/Balance)** — `AnalyticsController.java`, `AnalyticsService.java`, `TransactionRepository.java` (queries via JPQL)
6. **Expense Category Breakdown** — `TransactionRepository.getExpensesByCategory()` → `CategorySummaryDTO`
7. **User Profile Read/Update** — `UserController.java`, `UserService.java`

---

## Architectural Principles

The following principles are derived from the codebase's existing conventions and the target state recommendations:

| Principle | Rationale | Evidence |
|---|---|---|
| **Package by feature** | Each domain has its own controller/service/repository stack | Layout: `auth/`, `transaction/`, `ingestion/`, `user/` |
| **DTOs separate from entities** | Controllers expose DTOs, not JPA entities (partially followed) | `TransactionResponseDTO.java` used by `TransactionController`. Violation: `AuthController.register()` returns `User` entity directly (`AuthController.java:19`) |
| **Strategy pattern for parsers** | Bank-specific parsing is pluggable | `SmsParser` interface + `BancolombiaSmsParser` implementation + `SmsParserFactory` |
| **Global exception handling** | Centralized error responses via `@RestControllerAdvice` | `GlobalExceptionHandler.java` handles 4 exception types |
| **Stateless authentication** | JWT-based, no HTTP sessions | `SecurityConfig.java:26-29` sets `SessionCreationPolicy.STATELESS` |
| **Lombok for boilerplate** | Minimize getter/setter/constructor code | Every entity and DTO uses `@Data`, `@Builder`, `@RequiredArgsConstructor` |

---

## Current Architecture

### Style

**Layered architecture organized by feature (package-by-feature).** Each feature module contains its own internal layers:

```
Controller → Service → Repository → Entity
```

The README describes this as a "simplified clean architecture approach" but the current implementation is a conventional **Spring Boot layered monolith**. There is no separation between domain model and persistence model, no use-case interactors, and dependency inversion is partial.

### High-Level Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                      HTTP Clients (REST)                      │
├─────────────────────────────────────────────────────────────┤
│  auth.controller     user.controller     transaction.ctrl    │
│  ingestion.controller      analytics.controller              │
├─────────────────────────────────────────────────────────────┤
│  auth.service        user.service        transaction.svc     │
│  ingestion.service   analytics.service                       │
├─────────────────────────────────────────────────────────────┤
│  auth.repository     user.repository     transaction.repo    │
├─────────────────────────────────────────────────────────────┤
│  auth.entity         transaction.entity   (JPA entities)     │
├─────────────────────────────────────────────────────────────┤
│                    PostgreSQL Database                        │
└─────────────────────────────────────────────────────────────┘
```

### Dependency Flow Between Packages

```
auth.controller → auth.service → auth.repository → auth.entity
                                       ↓
user.controller → user.service → user.repository → auth.entity
                                       ↓
transaction.controller → transaction.service → transaction.repository
                          ↕                        ↕
                    user.service            auth.entity
                          ↕
               ingestion.controller → ingestion.service → transaction.service
                          ↕
               analytics.controller → analytics.service → transaction.repository
```

---

## Module Structure

### Current Package Layout

```
com.pocketminder
├── PocketminderApplication.java
├── auth/
│   ├── config/
│   │   ├── ApplicationConfig.java          (beans: UserDetailsService, PasswordEncoder, AuthManager)
│   │   └── SecurityConfig.java             (SecurityFilterChain, CSRF disabled, stateless)
│   ├── controller/
│   │   └── AuthController.java             (POST /auth/register, GET /auth/login, GET /auth/me)
│   ├── dto/
│   │   ├── AuthResponseDTO.java            (token string)
│   │   ├── LoginRequestDTO.java            (email, password)
│   │   └── RegisterRequestDTO.java         (name, email, password)
│   ├── entity/
│   │   └── User.java                       (@Entity, implements UserDetails)
│   ├── repository/
│   │   └── UserRepository.java             (JpaRepository, findByEmail)
│   ├── security/
│   │   ├── JwtAuthenticationFilter.java    (OncePerRequestFilter)
│   │   └── JwtService.java                 (generateToken, extractEmail)
│   └── service/
│       ├── AuthService.java                (register, login)
│       └── CustomUserDetailsService.java    (loadUserByUsername)
│
├── common/
│   └── exception/
│       ├── EmailAlreadyExistsException.java
│       ├── ErrorResponseDTO.java           (message, status, timestamp)
│       ├── GlobalExceptionHandler.java      (@RestControllerAdvice)
│       └── UnsupportedBankException.java
│
├── ingestion/
│   └── sms/
│       ├── controller/
│       │   └── SmsIngestionController.java  (POST /ingestion/sms)
│       ├── dto/
│       │   └── SmsMessageDTO.java           (message string)
│       ├── parser/
│       │   ├── BancolombiaSmsParser.java    (Bancolombia-specific regex parsing)
│       │   ├── SmsParser.java               (interface: supports, parse)
│       │   └── SmsParserFactory.java        (finds matching parser)
│       └── service/
│           └── SmsIngestionService.java     (orchestrates parse → create)
│
├── transaction/
│   ├── analytics/
│   │   ├── controller/
│   │   │   └── AnalyticsController.java     (@Controller — likely a bug, missing @RestController)
│   │   ├── dto/
│   │   │   ├── CategorySummaryDTO.java      (category, total)
│   │   │   └── FinancialSummaryDTO.java     (income, expenses, balance)
│   │   └── service/
│   │       └── AnalyticsService.java        (getFinancialSummary, getExpenseCategoriesSummary)
│   ├── controller/
│   │   ├── SmsTransactionController.java    (POST /transactions/sms — DUPLICATE of ingestion)
│   │   └── TransactionController.java       (POST/GET /transactions)
│   ├── dto/
│   │   ├── CreateTransactionDTO.java        (title, description, amount, type, category)
│   │   ├── InternalTransactionRequest.java  (internal DTO used by service)
│   │   └── TransactionResponseDTO.java      (response DTO)
│   ├── entity/
│   │   ├── Transaction.java                 (@Entity with @ManyToOne → User)
│   │   ├── TransactionCategory.java         (enum: FOOD, TRANSPORT, ..., OTHER)
│   │   ├── TransactionSource.java           (enum: MANUAL, SMS, BANK_API, CSV_IMPORT, AI_PARSER)
│   │   └── TransactionType.java             (enum: INCOME, EXPENSE, TRANSFER)
│   ├── mapper/
│   │   └── TransactionMapper.java           (toResponse)
│   ├── repository/
│   │   └── TransactionRepository.java       (JpaRepository + custom JPQL queries)
│   └── service/
│       └── TransactionService.java          (createTransaction, getMyTransactions)
│
└── user/
    ├── controller/
    │   └── UserController.java              (GET/PUT /user/me)
    ├── dto/
    │   ├── UpdateUserDTO.java               (name)
    │   └── UserResponseDTO.java             (id, name, email)
    ├── mapper/
    │   └── UserMapper.java                  (toResponse)
    └── service/
        └── UserService.java                 (getCurrentUser, updateProfile)
```

### File Inventory

Total: **44 Java source files** (43 main + 1 test), **2 configuration files** (application.properties, application-example.properties), **1 README**, **1 POM**.

---

## Layer Responsibilities

| Layer | Current Responsibility | Evidence |
|---|---|---|
| **Controller** | Receive HTTP requests, invoke services, map to response DTOs | `TransactionController.java:24-46` maps `CreateTransactionDTO` → `InternalTransactionRequest` → service → `TransactionResponseDTO` |
| **Service** | Business logic + orchestration + persistence calls | `AuthService.java:24-38` handles validation, password encoding, entity creation, and save |
| **Repository** | Data access via Spring Data JPA + custom JPQL | `TransactionRepository.java:24-48` defines aggregate queries for analytics |
| **Entity** | JPA-persisted data model, sometimes coupled to framework concerns | `User.java:18` implements `UserDetails`; `Transaction.java:48` has `@ManyToOne` |
| **DTO** | Request/response contracts, validation annotations | `CreateTransactionDTO.java:13-26` uses `@NotNull`, `@NotBlank` |
| **Mapper** | Transform entities to DTOs | `TransactionMapper.java:10-27` copies fields from `Transaction` to `TransactionResponseDTO` |
| **Security** | JWT filter, token generation/validation | `JwtAuthenticationFilter.java:26-72`, `JwtService.java:28-50` |
| **Config** | Spring bean wiring, security filter chain | `ApplicationConfig.java`, `SecurityConfig.java` |

### Issues with Current Layer Boundaries

1. **Controller returns entity** — `AuthController.register()` returns `User` (JPA entity) instead of a DTO, exposing the password hash (via `@Getter` on `User.java:29`).
2. **Controller annotation mismatch** — `AnalyticsController.java:7` uses `@Controller` instead of `@RestController`, so response bodies may not be serialized without explicit `@ResponseBody`.
3. **Service depends on concrete service** — `TransactionService.java:19` depends on `UserService` (concrete class), not an interface — violation of Dependency Inversion.
4. **DTO constructed in repository** — `TransactionRepository.java:36-38` uses a JPQL `new` expression to construct `CategorySummaryDTO` from the `analytics` package, creating a backward dependency from repository → DTO.

---

## SMS Ingestion Flow

### Current Flow (Two Duplicate Paths)

**Path A:** `POST /ingestion/sms` → `SmsIngestionController.java`
**Path B:** `POST /transactions/sms` → `SmsTransactionController.java`

Both are functionally identical:

```
Client → SmsIngestionController / SmsTransactionController
  → SmsIngestionService.ingest(message)
    → SmsParserFactory.getParser(message)
      → BancolombiaSmsParser.supports(message)  [contains "Bancolombia"?]
    → BancolombiaSmsParser.parse(message)
      → regex extraction: amount ($X,XXX), type (recibiste=INCOME), merchant (a [MERCHANT] desde)
    → InternalTransactionRequest (title, description, amount, type, category=OTHER, source=SMS)
    → TransactionService.createTransaction(request)
      → UserService.getCurrentUser()  [via SecurityContextHolder]
      → TransactionRepository.save(entity)
    → TransactionMapper.toResponse(transaction)
    → TransactionResponseDTO
```

### Key Code Evidence

| Step | File | Lines |
|---|---|---|
| Parser selection | `SmsParserFactory.java:19-28` | Stream filter + findFirst, throws `UnsupportedBankException` |
| Bancolombia matching | `BancolombiaSmsParser.java:15-18` | `message.contains("Bancolombia")` — fragile string match |
| Amount extraction | `BancolombiaSmsParser.java:25-40` | Regex `\$([\d,.]+)`, removes commas, `new BigDecimal(...)` |
| Type detection | `BancolombiaSmsParser.java:42-51` | `message.contains("recibiste")` → INCOME, else EXPENSE |
| Merchant extraction | `BancolombiaSmsParser.java:53-66` | Regex `a ([A-Z0-9\\s]+?) desde` |
| Category always OTHER | `BancolombiaSmsParser.java:74` | Hardcoded `TransactionCategory.OTHER` |

### Current Limitations

- **Only one bank parser** — Bancolombia only; any other SMS format throws `UnsupportedBankException`
- **No categorization** — all SMS transactions land in `OTHER`
- **Duplicate endpoints** — two identical HTTP endpoints doing the same thing
- **No enrichment pipeline** — merchant extraction is basic regex, no normalization
- **Synchronous only** — ingestion blocks the HTTP thread

---

## Transaction Processing Flow

### Manual Transaction Creation

```
Client → POST /transactions
  → TransactionController.createTransaction(CreateTransactionDTO)
    → InternalTransactionRequest.builder()
        .source(MANUAL)
        .autoDetected(false)
        .transactionDate(LocalDateTime.now())   ← hardcoded to now
    → TransactionService.createTransaction(internalRequest)
      → UserService.getCurrentUser()
      → Transaction.builder() ... .user(user) .createdAt(now)
      → TransactionRepository.save(transaction)
    → TransactionMapper.toResponse(transaction)
    → TransactionResponseDTO
```

### SMS Transaction Creation

```
(same as SMS Ingestion Flow above, converging at TransactionService.createTransaction)
```

### Transaction Listing

```
Client → GET /transactions
  → TransactionController.getMyTransactions()
    → TransactionService.getMyTransactions()
      → UserService.getCurrentUser()
      → TransactionRepository.findByUser(user)
    → stream().map(mapper::toResponse).toList()
    → List<TransactionResponseDTO>
```

### Financial Summary

```
Client → GET /analytics/summary
  → AnalyticsService.getFinancialSummary()
    → UserService.getCurrentUser()
    → TransactionRepository.getTotalByType(user, INCOME)   ← JPQL SUM query
    → TransactionRepository.getTotalByType(user, EXPENSE)  ← JPQL SUM query
    → income.subtract(expenses) = balance
    → FinancialSummaryDTO

Client → GET /analytics/expenses/categories
  → AnalyticsService.getExpenseCategoriesSummary()
    → UserService.getCurrentUser()
    → TransactionRepository.getExpensesByCategory(user)   ← JPQL GROUP BY query
    → List<CategorySummaryDTO>
```

### Current Limitations

- **No `@Transactional`** — Service methods rely on implicit JpaRepository transactions. Multi-step operations (e.g., create + update read model) have no transactional boundary.
- **Hardcoded transactionDate** — `TransactionController.java:37` sets `transactionDate = LocalDateTime.now()`. The `CreateTransactionDTO` has no date field.
- **Analytics queries are live aggregates** — Every summary request scans the `transactions` table and calculates SUM/GROUP BY. At scale this will degrade performance.
- **Cross-context coupling** — Both `TransactionService` and `AnalyticsService` directly depend on `UserService` (concrete class in `user.service` package).

---

## Database Architecture

### Schema

Derived from entity annotations with `ddl-auto=update` (no migration scripts):

#### Table: `users`

| Column | Type | Constraints | Source |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | `User.java:21-22` |
| name | VARCHAR(255) | | `User.java:24` |
| email | VARCHAR(255) | UNIQUE | `User.java:26-27` |
| password | VARCHAR(255) | | `User.java:29` |

#### Table: `transactions`

| Column | Type | Constraints | Source |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | `Transaction.java:20-21` |
| title | VARCHAR(255) | | `Transaction.java:23` |
| description | VARCHAR(255) | | `Transaction.java:25` |
| amount | DECIMAL(38,2) | | `Transaction.java:27` |
| type | VARCHAR(255) | ENUM: INCOME/EXPENSE/TRANSFER | `Transaction.java:29-30` |
| category | VARCHAR(255) | ENUM: FOOD/TRANSPORT/.../OTHER | `Transaction.java:32-33` |
| source | VARCHAR(255) | ENUM: MANUAL/SMS/BANK_API/CSV_IMPORT/AI_PARSER | `Transaction.java:35-36` |
| raw_message | VARCHAR(255) | | `Transaction.java:38` |
| detected_merchant | VARCHAR(255) | | `Transaction.java:40` |
| auto_detected | BOOLEAN | | `Transaction.java:42` |
| transaction_date | TIMESTAMP | | `Transaction.java:44` |
| created_at | TIMESTAMP | | `Transaction.java:46` |
| user_id | BIGINT | FK → users(id) | `Transaction.java:48-49` |

### Relationship

```
users 1 ──── * transactions   (One user has many transactions)
```

### ORM Configuration

```properties
spring.jpa.hibernate.ddl-auto=update   ← Hibernate generates schema from entities
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

**Evidence:** `application.properties:9-11`

### Current Issues

1. **No migration tooling** — `ddl-auto=update` is unsafe for production; schema changes are not versioned, reviewed, or reversible.
2. **No indexing** — No explicit `@Index` annotations. The JPQL queries filter by `user` + `type` + `category` — performance will degrade with volume.
3. **No audit columns on `users`** — `users` table lacks `created_at` / `updated_at` timestamps. The `transactions` table has `created_at` but no `updated_at`.
4. **No read model** — Analytics queries hit the transaction table directly with aggregates. No materialized views or summary tables.
5. **raw_message length** — `String` (defaults to VARCHAR(255)) may truncate longer SMS messages.

---

## Security Architecture

### Authentication Flow

```
Login Request
  POST /auth/login  [BUG: currently @GetMapping]
    → AuthService.login(request)
      → AuthenticationManager.authenticate(UsernamePasswordAuthenticationToken)
        → CustomUserDetailsService.loadUserByUsername(email)
          → UserRepository.findByEmail(email)
            → Returns User (which implements UserDetails)
        → BCryptPasswordEncoder.matches(password, user.password)
      → JwtService.generateToken(email)
        → Jwts.builder().subject(email).signWith(hmacKey).compact()
    → AuthResponseDTO(token)

Subsequent Requests
  Header: Authorization: Bearer <token>
    → JwtAuthenticationFilter.doFilterInternal()
      → JwtService.extractEmail(token)
        → Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject()
      → CustomUserDetailsService.loadUserByUsername(email)
      → UsernamePasswordAuthenticationToken → SecurityContextHolder.setAuthentication()
```

### Security Configuration

| Concern | Setting | Evidence |
|---|---|---|
| CSRF | Disabled | `SecurityConfig.java:24` |
| Session | Stateless (no HTTP session) | `SecurityConfig.java:26-29` |
| Public endpoints | `/auth/register`, `/auth/login` | `SecurityConfig.java:34-37` |
| Protected endpoints | All others require authentication | `SecurityConfig.java:39` |
| Password hashing | BCrypt | `ApplicationConfig.java:31` |
| JWT filter position | Before `UsernamePasswordAuthenticationFilter` | `SecurityConfig.java:42-44` |
| JWT expiration | 1 hour | `JwtService.java:36-37` |
| JWT signing | HMAC-SHA from `jwt.secret` property | `JwtService.java:23-25` |
| Secret | Read from `${jwt.secret}` | `JwtService.java:16-17` |

### Current Security Issues

1. **`/auth/login` is GET** — `AuthController.java:25` uses `@GetMapping` for login. Login is a state-changing, sensitive operation and must be `@PostMapping`. GET requests can be logged in plain text in server access logs, bookmarked, and cached.
2. **`User` is `UserDetails`** — `User.java:18` implements Spring Security's `UserDetails`. This couples the JPA entity to the security framework. All 6 `UserDetails` methods are implemented, with 4 returning hardcoded `true`.
3. **No refresh tokens** — JWT expires after 1 hour with no mechanism to refresh without re-authentication.
4. **No rate limiting** — Login endpoint has no brute-force protection.
5. **No CORS configuration** — No `CorsConfigurationSource` bean; browser-based frontends will be blocked.
6. **No password constraints** — `RegisterRequestDTO.java:17` only has `@NotBlank` on password — no minimum length, complexity, or strength requirements.
7. **No role-based authorization** — `SecurityConfig.java` has no role checks; all authenticated users have the same access level.

---

## External Integrations

### Current Integrations

| Integration | Type | Status | Evidence |
|---|---|---|---|
| PostgreSQL | Database driver | Active | `pom.xml`: `postgresql` dependency (runtime scope) |
| JWT (jjwt) | Token library | Active | `pom.xml`: `jjwt-api` 0.12.5, `jjwt-impl` 0.12.6, `jjwt-jackson` 0.12.5 |

### Zero-Code Integrations (defined in enums but not implemented)

| Integration | Enum Value | Location | Status |
|---|---|---|---|
| Bank API | `TransactionSource.BANK_API` | `TransactionSource.java:6` | Not implemented |
| CSV Import | `TransactionSource.CSV_IMPORT` | `TransactionSource.java:7` | Not implemented |
| AI Parser | `TransactionSource.AI_PARSER` | `TransactionSource.java:8` | Not implemented |

There are zero HTTP client integrations (no `WebClient`, `RestTemplate`, or `FeignClient` beans). The system does not currently call any external APIs.

---

## Error Handling Strategy

### Current Implementation

**Global exception handler:** `GlobalExceptionHandler.java` (at `common.exception` package)

| Exception | HTTP Status | Handler | Evidence |
|---|---|---|---|
| `EmailAlreadyExistsException` | 409 CONFLICT | `GlobalExceptionHandler.java:14-31` |
| `UnsupportedBankException` | 400 BAD_REQUEST | `GlobalExceptionHandler.java:77-95` |
| `MethodArgumentNotValidException` | 400 BAD_REQUEST | `GlobalExceptionHandler.java:53-75` | Extracts first field error message |
| `Exception` (catch-all) | 500 INTERNAL_SERVER_ERROR | `GlobalExceptionHandler.java:33-51` |

**Error response shape** (`ErrorResponseDTO.java`):
```json
{
  "message": "...",
  "status": 400,
  "timestamp": "2026-06-03T12:00:00"
}
```

### Missing Cases

| Scenario | Current Behavior | Should Be |
|---|---|---|
| JWT expired | Returns 500 (generic Spring Security error) | 401 with `token_expired` message |
| Invalid JWT | Returns 500 | 401 with `invalid_token` message |
| Resource not found (e.g., user, transaction) | NPE or generic `Exception` | `ResourceNotFoundException` → 404 |
| Access denied | Spring default 403 | Custom `AccessDeniedHandler` |
| Constraint violation (DB unique, not-null) | Hibernate 500 | `DataIntegrityViolationException` handler → 409/400 |
| `HttpMessageNotReadableException` (bad JSON) | Spring default 400 | Custom handler with validation details |
| `MissingServletRequestParameterException` | Spring default 400 | Custom handler |

---

## Logging Strategy

### Current State

**No custom logging configuration exists.** The application relies on Spring Boot's default Logback configuration (console output only). Key gaps:

- No `logback-spring.xml` or `application.properties` logging config beyond defaults
- No structured logging (JSON format)
- No log levels per package
- No log file rotation
- No correlation IDs for request tracing
- No audit log for security events (login failures, registration)
- No business event logging (transaction created, SMS ingested)

**Evidence:** No `logging.*` properties in `application.properties`; no `logback.xml` or `logback-spring.xml` anywhere in the project.

---

## Testing Strategy

### Current State

**A single test file exists:**
- `PocketminderApplicationTests.java` — a context-load test (`@SpringBootTest`, empty `contextLoads()` method)

**Zero tests for:**
- Controllers (no `@WebMvcTest` or `MockMvc` tests)
- Services (no unit tests)
- Repositories (no `@DataJpaTest`)
- SMS parsers (no parser unit tests)
- Security (no `@WithMockUser` or security chain tests)
- Analytics (no aggregation query tests)

**Test dependencies in POM** (all unused):
```xml
spring-boot-starter-data-jpa-test
spring-boot-starter-security-test
spring-boot-starter-validation-test
spring-boot-starter-webmvc-test
```

**Evidence:** `pom.xml:38-53` — all test starters are declared but no test classes use them.

---

## Technical Debt

All items are ranked by severity (High / Medium / Low) and cross-referenced to source.

| # | Issue | Severity | File | Lines |
|---|---|---|---|---|
| T1 | **Login endpoint uses GET instead of POST** | **High** | `AuthController.java` | 25 |
| T2 | **Register endpoint returns `User` entity directly** (exposes password hash via `@Getter`) | **High** | `AuthController.java` | 19-23 |
| T3 | **No database migrations** (`ddl-auto=update`) | **High** | `application.properties` | 9 |
| T4 | **Inadequate test coverage** — 0 unit/integration tests | **High** | `src/test/` | All |
| T5 | **Duplicate SMS endpoint** — two controllers, same logic | **Medium** | `SmsIngestionController.java`, `SmsTransactionController.java` | Both |
| T6 | **`AnalyticsController` uses `@Controller` instead of `@RestController`** (missing `@ResponseBody`) | **Medium** | `transaction/analytics/controller/AnalyticsController.java` | 7 |
| T7 | **Repository constructs DTO in JPQL query** — cross-package dependency | **Medium** | `TransactionRepository.java` | 35-44 |
| T8 | **`User` entity implements `UserDetails`** — SRP violation | **Medium** | `User.java` | 18 |
| T9 | **Services depend on concrete `UserService`** — DIP violation | **Medium** | `TransactionService.java`, `AnalyticsService.java` | 19, 19 |
| T10 | **`/auth/me` returns hardcoded string** instead of user data | **Low** | `AuthController.java` | 33-35 |
| T11 | **`CreateTransactionDTO` has no `transactionDate` field** — hardcoded to `now()` | **Low** | `TransactionController.java` | 37 |
| T12 | **Placeholder JWT secret in example config** | **Low** | `application-example.properties` | 10 |
| T13 | **No JavaDoc** on any class, method, or field across all 43 files | **Low** | Entire project | All |
| T14 | **Commented-out TODO code**: `createSmsTransaction()` stub | **Low** | `TransactionService.java` | 52-55 |
| T15 | **`UserResponseDTO` missing `@AllArgsConstructor`** — `@Builder` requires it at runtime | **Low** | `user/dto/UserResponseDTO.java` | 8 |
| T16 | **No CORS configuration** | **Medium** | `SecurityConfig.java` | Missing |
| T17 | **No `@Transactional` annotations** on service operations | **Medium** | All service classes | All |
| T18 | **No indexing on transaction table** (user_id, type, category) | **Low** | `Transaction.java` | All JPA |
| T19 | **`raw_message` column is VARCHAR(255)** — may truncate longer SMS | **Low** | `Transaction.java` | 38 |

---

## Future Evolution

### Target Architecture: Hexagonal + DDD

The target architecture separates the monolith into bounded contexts, each using **Hexagonal (Ports & Adapters)** architecture with **Domain-Driven Design** tactical patterns.

```
┌──────────────────────────────────────────────────────────────────┐
│                   REST / CLI Interfaces                           │
│  Controllers map HTTP ↔ Application DTOs (no entities exposed)   │
├──────────────────────────────────────────────────────────────────┤
│                  Application Layer (Use Cases)                    │
│  RegisterUseCase, IngestSmsUseCase, CreateTransactionUseCase     │
│  → orchestrates domain objects, publishes events, manages TX     │
├──────────────────────────────────────────────────────────────────┤
│                     Domain Layer (Pure Java)                      │
│  Transaction, User (Aggregates), Money, Email (Value Objects)     │
│  Domain Events, Repository interfaces, SmsParser port            │
│  ZERO framework annotations (no @Entity, no @Service)            │
├──────────────────────────────────────────────────────────────────┤
│                 Infrastructure Layer (Adapters)                   │
│  JPA repositories, JwtService adapter, Bank parsers,             │
│  Event bus, AI client adapter, Flyway migrations                 │
└──────────────────────────────────────────────────────────────────┘
```

### Proposed Package Structure

```
com.pocketminder
├── shared/                              (cross-cutting)
│   ├── domain/                          (base types: AggregateRoot, DomainEvent, ValueObject)
│   └── infrastructure/                  (shared adapters: event bus, base entity)
│
├── identity/                            (Bounded Context)
│   ├── domain/                          (User aggregate, Email VO, Password VO, UserRepository port)
│   ├── application/                     (RegisterUseCase, LoginUseCase, DTOs, ports)
│   ├── infrastructure/                  (JPA adapter, JWT adapter, BCrypt adapter)
│   └── interfaces/                      (AuthController, request/response DTOs)
│
├── transaction/                         (Bounded Context)
│   ├── domain/                          (Transaction aggregate, Money VO, event)
│   ├── application/                     (CreateTransactionUseCase, ...)
│   ├── infrastructure/                  (JPA adapter, event publisher)
│   └── interfaces/                      (TransactionController)
│
├── ingestion/                           (Bounded Context)
│   ├── domain/                          (SmsParser port, RawMessage VO, ParsedTransaction)
│   ├── application/                     (IngestSmsUseCase)
│   ├── infrastructure/                  (BancolombiaParser, DaviviendaParser, factory)
│   └── interfaces/                      (SmsIngestionController)
│
├── categorization/                      (Bounded Context)
│   ├── domain/                          (CategorizationEngine port, CategorizationResult)
│   ├── application/                     (CategorizeTransactionUseCase, handler)
│   └── infrastructure/                  (RuleBasedCategorizer, MlCategorizationAdapter)
│
├── analytics/                           (Bounded Context — CQRS read model)
│   ├── domain/                          (FinancialSummary, CategoryBreakdown)
│   ├── application/                     (GetFinancialSummaryUseCase)
│   ├── infrastructure/                  (SummaryViewRepository, event handler)
│   └── interfaces/                      (AnalyticsController)
│
└── ai/                                  (Bounded Context — future)
    ├── domain/                          (InsightEngine port, RecommendationEngine port)
    ├── application/                     (GenerateInsightsUseCase)
    ├── infrastructure/                  (OpenAiAdapter, PromptTemplateService)
    └── interfaces/                      (AiController)
```

### Key Architectural Changes

#### From Layered → Hexagonal
- Domain objects become pure POJOs with zero framework annotations
- Repository interfaces become domain ports; JPA implementations become infrastructure adapters
- Services become Use Cases with explicit input/output DTOs

#### From Synchronous → Event-Driven
- `TransactionCreatedEvent` published after creation
- Categorization, analytics, and AI subscribe asynchronously
- Start with `SpringApplicationEventPublisher` + `@Async`; upgrade to message broker when scaling

#### From Enums → Value Objects + Database-backed taxonomy
- `TransactionCategory` evolves from enum to entity (allow user-defined categories)
- `Money` value object wraps `BigDecimal` + `Currency` for multi-currency support

#### From Live Queries → CQRS for Analytics
- `TransactionEventHandler` updates a pre-computed summary read model
- Analytics endpoints query the read model (table or cache), not the transaction table

### Categorization Pipeline (Target)

```
Transaction Created (category = UNCATEGORIZED)
  → TransactionCreatedEvent published
    → CategorizationHandler receives event
      → RuleBasedCategorizer.match(merchant, description, amount)
        → matched → update transaction category
        → unmatched → MlCategorizationAdapter.classify(merchant, description)
          → high confidence → update category
          → low confidence → flag for user review
```

### Future Microservice Boundaries

In order of extraction priority:

| Order | Service | Rationale |
|---|---|---|
| 1 | **Identity Service** | Security-critical, independent domain, benefits from isolation |
| 2 | **AI Service** | Needs GPU/specialized infra, high latency, separation prevents resource contention |
| 3 | **Ingestion Service** | Burst load from SMS/email, needs independent scaling |
| 4 | **Analytics Service** | Read-heavy, benefits from independent caching and read replicas |
| 5 | **Transaction Service** | Core domain, extracted last as it is the most coupled |

### Migration Roadmap

| Phase | Changes |
|---|---|
| **Phase 1 (Immediate)** | Fix `@GetMapping` login → `@PostMapping`; add `@AllArgsConstructor` to `UserResponseDTO`; change `AnalyticsController` to `@RestController`; deduplicate SMS endpoints; add `@Transactional` annotations |
| **Phase 2 (Data)** | Add Flyway/Liquibase; dump current schema as V1; set `ddl-auto=validate`; add DB indexes; add `created_at`/`updated_at` to `users` |
| **Phase 3 (Domain)** | Extract domain POJOs; add JPA-annotated entity copies as infrastructure; write adapter mappers; decouple `Transaction` from `User` object reference → use `userId` field |
| **Phase 4 (Events)** | Introduce `DomainEventPublisher` port; publish `TransactionCreatedEvent`; wire async categorization handler; build `RuleBasedCategorizer` |
| **Phase 5 (Analytics CQRS)** | Create summary read model; update via event handler; migrate analytics endpoints to read model |
| **Phase 6 (AI)** | Add `InsightEngine` port; implement `OpenAiAdapter`; add AI-powered categorization fallback; generate spending insights |
| **Phase 7 (Scale)** | Extract microservices per bounded context if monolith limits arise; introduce message broker between services |

---

## Repository Evidence

Every claim in this document is grounded in the source. Key files:

| File | Purpose |
|---|---|
| `pom.xml` | Build configuration, dependencies (Java 17, Spring Boot 4.0.6, PostgreSQL, jjwt) |
| `application.properties` | Database URL, `ddl-auto=update`, JWT secret |
| `PocketminderApplication.java` | `@SpringBootApplication` entry point |
| `auth/entity/User.java` | JPA entity implementing `UserDetails` — SRP violation evidence |
| `auth/controller/AuthController.java` | `@GetMapping("/login")` — HTTP method bug; returns `User` entity |
| `auth/security/JwtService.java` | Token generation with 1-hour expiration |
| `auth/config/SecurityConfig.java` | CSRF disabled, stateless session, JWT filter chain |
| `transaction/entity/Transaction.java` | `@ManyToOne` → User, `rawMessage` VARCHAR(255) length issue |
| `transaction/entity/TransactionCategory.java` | Enum with 10 values (no user customizability) |
| `transaction/entity/TransactionSource.java` | `AI_PARSER` / `BANK_API` — defined but unused |
| `transaction/repository/TransactionRepository.java` | JPQL constructing DTO from analytics package |
| `transaction/controller/TransactionController.java` | Hardcoded `transactionDate = LocalDateTime.now()` |
| `transaction/analytics/controller/AnalyticsController.java` | `@Controller` instead of `@RestController` |
| `ingestion/sms/parser/BancolombiaSmsParser.java` | Regex-based parser, hardcoded `OTHER` category |
| `ingestion/sms/parser/SmsParserFactory.java` | Strategy pattern via injected `List<SmsParser>` |
| `ingestion/sms/controller/SmsIngestionController.java` | Duplicate of `SmsTransactionController` |
| `user/service/UserService.java` | `SecurityContextHolder` usage, concrete dependency |
| `common/exception/GlobalExceptionHandler.java` | Handles 4 exception types, returns `ErrorResponseDTO` |
| `user/dto/UserResponseDTO.java` | Missing `@AllArgsConstructor` — Lombok builder risk |
| `src/test/.../PocketminderApplicationTests.java` | Only test file — context load only |
| `.gitignore` | Excludes `application.properties` (secrets), includes example config |
| `application-example.properties` | Template with placeholder values |

---

*This document is based on analysis of all 44 Java source files, 2 configuration files, the Maven POM, README, and .gitignore as of 2026-06-03.*
