# PocketMinder — Development Guide

> **Version:** 1.0  
> **Last updated:** 2026-06-03  
> **Applies to:** `com.pocketminder:pocketminder:0.0.1-SNAPSHOT`

---

## Table of Contents

1. [Technology Stack](#technology-stack)
2. [Java Standards](#java-standards)
3. [Spring Standards](#spring-standards)
4. [Package Organization](#package-organization)
5. [DTO Conventions](#dto-conventions)
6. [Entity Conventions](#entity-conventions)
7. [Service Conventions](#service-conventions)
8. [Validation Standards](#validation-standards)
9. [Error Handling Standards](#error-handling-standards)
10. [Logging Standards](#logging-standards)
11. [Testing Standards](#testing-standards)
12. [Database Standards](#database-standards)
13. [API Standards](#api-standards)
14. [Naming Conventions](#naming-conventions)
15. [Code Review Checklist](#code-review-checklist)

---

## Technology Stack

| Technology | Version | Purpose | Evidence |
|---|---|---|---|
| Java | 17 | Runtime language | `pom.xml:30` |
| Spring Boot | 4.0.6 | Application framework | `pom.xml:8` (via parent) |
| Spring Data JPA | — (managed) | ORM / persistence | `pom.xml:34` |
| Spring Security | — (managed) | Authentication & authorization | `pom.xml:37` |
| Spring Validation | — (managed) | Bean Validation (jakarta.validation) | `pom.xml:40` |
| Spring WebMVC | — (managed) | REST endpoints | `pom.xml:43` |
| Spring Boot DevTools | — (managed) | Development-time hot reload | `pom.xml:47-49` |
| PostgreSQL | — (runtime) | Database | `pom.xml:53` |
| Lombok | — | Boilerplate reduction | `pom.xml:57` |
| jjwt (io.jsonwebtoken) | 0.12.5 / 0.12.6 | JWT creation and parsing | `pom.xml:86-103` |
| Maven | 3.9.15 | Build tool | `.mvn/wrapper/maven-wrapper.properties:3` |

---

## Java Standards

### Language Level

Use Java 17 features where appropriate. The project is compiled at `java.version=17` (`pom.xml:30`).

### Records vs Classes

Prefer plain classes with Lombok annotations for DTOs and entities (established convention). Java `record` types may be used for internal data carriers that do not require JPA or Jackson mapping.

### Nullability

- Use `java.util.Objects.requireNonNull()` for critical null checks on `SecurityContextHolder` and similar framework objects (see `UserService.java:19`).
- Use `java.util.Optional` for repository return types that may be absent (see `UserRepository.java:10`).
- Avoid `null` returns from service methods; throw exceptions for absent values (see `CustomUserDetailsService.java:21-25`).

### Imports

- No wildcard imports except for closely related enum types in the same package (see `TransactionService.java:5` uses `import com.pocketminder.transaction.entity.*`).
- Organize imports: Jakarta/Jakarta Validation types first, then Spring, then project types.
- Remove unused imports before committing.

### Lombok Annotations

The project uses Lombok consistently. Follow these rules:

| Annotation | Where | Example |
|---|---|---|
| `@Data` | All DTOs | `CreateTransactionDTO.java:7`, `RegisterRequestDTO.java:7` |
| `@Builder` | Response DTOs, entities | `TransactionResponseDTO.java:11`, `User.java:17` |
| `@AllArgsConstructor` | Entities | `User.java:16` |
| `@NoArgsConstructor` | Entities (JPA requires) | `User.java:15` |
| `@RequiredArgsConstructor` | Controllers, Services, Configs | `AuthController.java:14`, `AuthService.java:17` |
| `@Getter` / `@Setter` | Entities (when selective control needed) | `User.java:13-14`, `Transaction.java:12-13` |

**Rules:**
- Do NOT mix `@Data` with `@Builder` — `@Data` generates `@AllArgsConstructor` only if no other constructor annotation is present. If using `@Builder`, explicitly add `@AllArgsConstructor` when needed.
- `@RequiredArgsConstructor` is preferred over explicit constructors for dependency injection.
- Do not use `@Setter` on entities unless mutation is required (`User` has it, `Transaction` has it).

---

## Spring Standards

### Bean Declaration

- Use `@Component`, `@Service`, `@Repository`, `@Controller` stereotype annotations on classes (established convention).
- Use `@Bean` in `@Configuration` classes only for third-party or framework integrations (`ApplicationConfig.java:20`, `SecurityConfig.java:18`).
- Use constructor injection via `@RequiredArgsConstructor` (entire codebase convention). Never use `@Autowired` field injection.

### Configuration

- Application properties: `src/main/resources/application.properties` (gitignored, see `.gitignore:16`).
- Example template: `src/main/resources/application-example.properties` (tracked in git, see `.gitignore:19-20`).
- Use `@Value` for property injection on simple fields (`JwtService.java:16`).
- Do not commit real secrets to `application.properties` use the example file for templates.

### Transaction Management

- Add `@Transactional` on service methods that perform multiple writes or cross-repository operations.
- Read-only operations should use `@Transactional(readOnly = true)`.
- Currently: `TransactionService.java` and `AuthService.java` rely on implicit transactions from `JpaRepository` default methods. Explicit `@Transactional` must be added when introducing multi-repository operations.

### Profiles

- No Spring profiles are currently defined. Add `application-dev.properties` and `application-prod.properties` when environment-specific configuration is needed.

---

## Package Organization

The project uses **package-by-feature** organization. Every feature module contains its own internal layers:

```
com.pocketminder
├── <feature>/
│   ├── controller/     (REST endpoints)
│   ├── service/        (business logic)
│   ├── repository/     (data access interfaces)
│   ├── entity/         (JPA entities)
│   ├── dto/            (request/response data transfer objects)
│   ├── mapper/         (entity ↔ DTO transformations)
│   ├── security/       (auth-related filters and utilities)
│   └── config/         (Spring beans and security configuration)
```

### Rules

1. **One feature, one top-level package** under `com.pocketminder`. Current features: `auth`, `transaction`, `ingestion`, `user`.
2. **Common code** goes in `com.pocketminder.common`. Currently: `common.exception`.
3. **Sub-features** use dot-separated packages: `transaction.analytics.controller`, `ingestion.sms.parser`.
4. **Cross-feature imports are allowed** but must be justified. Example: `TransactionService` imports `user.service.UserService` — this is acceptable for current architecture but introduces coupling.
5. **Never create circular dependencies** between feature packages.

### Current Package Inventory

```
com.pocketminder
├── auth.config
├── auth.controller
├── auth.dto
├── auth.entity
├── auth.repository
├── auth.security
├── auth.service
├── common.exception
├── ingestion.sms.controller
├── ingestion.sms.dto
├── ingestion.sms.parser
├── ingestion.sms.service
├── transaction.analytics.controller
├── transaction.analytics.dto
├── transaction.analytics.service
├── transaction.controller
├── transaction.dto
├── transaction.entity
├── transaction.mapper
├── transaction.repository
├── transaction.service
├── user.controller
├── user.dto
├── user.mapper
└── user.service
```

---

## DTO Conventions

### Request DTOs

- Annotate with `@Data` (Lombok).
- Use `jakarta.validation.constraints` annotations for input validation.
- Keep in the `<feature>.dto` package.
- Name pattern: `<Action><Entity>DTO` (e.g., `RegisterRequestDTO`, `CreateTransactionDTO`).

```java
// src/main/java/com/pocketminder/auth/dto/RegisterRequestDTO.java
package com.pocketminder.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequestDTO {
    @NotBlank
    private String name;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;
}
```

### Response DTOs

- Annotate with `@Data` + `@Builder`.
- Do NOT include `@AllArgsConstructor` — `@Builder` generates the all-args constructor internally.
- Keep in the `<feature>.dto` package.
- Name pattern: `<Entity>ResponseDTO` (e.g., `AuthResponseDTO`, `TransactionResponseDTO`).
- Never expose JPA entities as response types from controllers. Use DTOs exclusively.

```java
// src/main/java/com/pocketminder/transaction/dto/TransactionResponseDTO.java
package com.pocketminder.transaction.dto;

import com.pocketminder.transaction.entity.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionResponseDTO {
    private Long id;
    private String title;
    private String description;
    private BigDecimal amount;
    private TransactionType type;
    private TransactionCategory category;
    private TransactionSource source;
    private Boolean autoDetected;
    private LocalDateTime transactionDate;
}
```

### Internal DTOs

- For data transfer between layers within the same feature (service to service, or parser to service).
- Name pattern: `Internal<Entity>Request` (e.g., `InternalTransactionRequest`).
- Annotate with `@Data` + `@Builder`.

### Mappers

- One mapper class per entity, placed in `<feature>.mapper`.
- Annotate with `@Component`.
- Method naming convention: `toResponse(Entity entity)` returns `ResponseDTO`.
- Use `Builder` pattern for mapping (see `TransactionMapper.java:14-26`).

```java
@Component
public class TransactionMapper {
    public TransactionResponseDTO toResponse(Transaction transaction) {
        return TransactionResponseDTO.builder()
                .id(transaction.getId())
                .title(transaction.getTitle())
                // ...
                .build();
    }
}
```

---

## Entity Conventions

### JPA Setup

```java
@Entity
@Table(name = "table_name")      // snake_case plural
@Getter
@Setter
@NoArgsConstructor                // required by JPA
@AllArgsConstructor
@Builder
public class EntityName {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // fields...
}
```

### Relationships

- Use `@ManyToOne(fetch = FetchType.LAZY)` for parent references (see `Transaction.java:48`).
- Always use LAZY fetching. Never use EAGER.
- Reference other aggregates by `@ManyToOne` to the entity OR by a simple `Long foreignKeyId` field.
- Bidirectional relationships (`@OneToMany`) are discouraged unless required by domain logic.

### Column Constraints

- Use `@Column(unique = true)` for unique fields (see `User.java:26-27`).
- Enums use `@Enumerated(EnumType.STRING)` (see `Transaction.java:29-30`, `Transaction.java:32-33`, `Transaction.java:35-36`).
- Use `@Column(nullable = false)` for required columns. Currently missing in some entities — add as needed.

### Enum Conventions

```java
// src/main/java/com/pocketminder/transaction/entity/TransactionType.java
package com.pocketminder.transaction.entity;

public enum TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER
}
```

- Enums live in the entity package alongside the entity that uses them.
- Values are UPPER_CASE.
- Stored as strings in the database (`@Enumerated(EnumType.STRING)`).
- Do NOT reorder enum values once deployed — adding new values is safe, removing is not.

### Entity Relationships Map

| Entity | Field | Relationship | Target | Fetch Type |
|---|---|---|---|---|
| `Transaction` | `user` | `@ManyToOne` | `User` | LAZY |

---

## Service Conventions

### Service Class Template

```java
package com.pocketminder.<feature>.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class <Feature>Service {

    private final <Feature>Repository <feature>Repository;

    public ReturnType operation(RequestType request) {
        // business logic
    }
}
```

### Rules

1. **One service class per entity/aggregate.** Current services: `AuthService`, `UserService`, `TransactionService`, `AnalyticsService`, `SmsIngestionService`.
2. **Use `@Service` + `@RequiredArgsConstructor`** (entire codebase convention).
3. **Methods return domain objects (entities) or DTOs**, never `ResponseEntity`. Controllers are responsible for HTTP response wrapping.
4. **Authorization checks belong in services**, not controllers. Use `SecurityContextHolder` (see `UserService.java:19-24`) or method-level `@PreAuthorize`.
5. **Do not inject services from other feature packages unless necessary.** Cross-feature calls are allowed but documented coupling. Example: `TransactionService` depends on `UserService` (`TransactionService.java:19`).
6. **Use explicit `@Transactional`** on methods that perform multiple writes.

---

## Validation Standards

### Request Validation

Use `jakarta.validation.constraints` annotations on request DTO fields:

```java
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@NotBlank     // String fields that must not be null or blank
@NotNull      // Non-String fields that must not be null
@Email        // Email format validation
@Positive     // BigDecimal/Long fields that must be positive
@Size(min, max) // String length constraints
```

### Controller Validation

Always annotate request body parameters with `@Valid`:

```java
public ResponseType someOperation(@Valid @RequestBody SomeRequestDTO request) { ... }
```

See `TransactionController.java:25`, `AuthController.java:20`, `AuthController.java:28`.

### Error Responses for Validation

Validation errors are handled by `GlobalExceptionHandler.java:53-75`, which extracts the first field error and returns a 400 BAD_REQUEST with an `ErrorResponseDTO`.

---

## Error Handling Standards

### Error Response Contract

All errors return `ErrorResponseDTO` (`common/exception/ErrorResponseDTO.java`):

```json
{
  "message": "Human-readable error description",
  "status": 400,
  "timestamp": "2026-06-03T12:00:00"
}
```

### Exception Hierarchy

| Exception | HTTP Status | When to Throw | Handler |
|---|---|---|---|
| `EmailAlreadyExistsException` | 409 CONFLICT | Duplicate email during registration | `GlobalExceptionHandler.java:14-31` |
| `UnsupportedBankException` | 400 BAD_REQUEST | SMS from unknown bank | `GlobalExceptionHandler.java:77-95` |
| `MethodArgumentNotValidException` | 400 BAD_REQUEST | Validation failure (auto-thrown by Spring) | `GlobalExceptionHandler.java:53-75` |
| `Exception` (catch-all) | 500 INTERNAL_SERVER_ERROR | Unexpected errors | `GlobalExceptionHandler.java:33-51` |

### Adding a New Exception

1. Create the exception class in `common.exception` extending `RuntimeException`.
2. Add a handler method in `GlobalExceptionHandler` with `@ExceptionHandler`.
3. Use the appropriate HTTP status (never hardcode status codes — use `HttpStatus` constants).

```java
// 1. Exception class
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

// 2. Handler in GlobalExceptionHandler
@ExceptionHandler(ResourceNotFoundException.class)
public ResponseEntity<ErrorResponseDTO> handleNotFound(ResourceNotFoundException ex) {
    ErrorResponseDTO error = ErrorResponseDTO.builder()
            .message(ex.getMessage())
            .status(HttpStatus.NOT_FOUND.value())
            .timestamp(LocalDateTime.now())
            .build();
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
}
```

### Guidelines

- Do not catch generic `Exception` in business code — let it propagate to `GlobalExceptionHandler`.
- Do not expose stack traces or internal implementation details in error messages.
- Log the exception at the appropriate level in the handler before returning the response.

---

## Logging Standards

### Current State

No custom logging configuration exists. The project uses Spring Boot default Logback (console only).

### Standard Practice

- Use SLF4J `Logger` from Lombok: `@Slf4j` on any class that needs logging.
- Do not use `System.out.println` or `System.err.println`.

```java
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SomeService {
    public void someOperation() {
        log.info("Processing request for user {}", userId);
        log.debug("Detailed data: {}", internalState);
        log.error("Operation failed for user {}", userId, exception);
    }
}
```

### Log Levels

| Level | When to Use |
|---|---|
| `ERROR` | System is broken or data is corrupted. Immediate attention required. |
| `WARN` | Unexpected but handled situation (e.g., unsupported bank SMS). |
| `INFO` | Business events: registration, transaction creation, login. |
| `DEBUG` | Detailed flow for development troubleshooting. |
| `TRACE` | Not used. Reserved for extremely detailed diagnostics. |

### What to Log

- **Always log:** Authentication failures, registration, transaction creation (with user context, not full payload).
- **Never log:** Passwords, JWT secrets, full credit card numbers, raw security tokens.
- **Log context:** Include `userId` or correlation ID to trace requests across operations.

---

## Testing Standards

### Test Framework

- JUnit 5 (Spring Boot starter parent manages version).
- Spring Boot test starters are declared in `pom.xml:67-84` but currently unused:
  - `spring-boot-starter-data-jpa-test`
  - `spring-boot-starter-security-test`
  - `spring-boot-starter-validation-test`
  - `spring-boot-starter-webmvc-test`

### Test Structure

Place tests in `src/test/java/com/pocketminder/`, mirroring the main source structure:

```
src/test/java/com/pocketminder/
├── PocketminderApplicationTests.java        (context load test)
├── auth/
│   ├── controller/
│   │   └── AuthControllerTest.java
│   ├── service/
│   │   └── AuthServiceTest.java
│   └── security/
│       └── JwtServiceTest.java
├── transaction/
│   ├── controller/
│   │   └── TransactionControllerTest.java
│   ├── service/
│   │   └── TransactionServiceTest.java
│   └── repository/
│       └── TransactionRepositoryTest.java
└── ingestion/
    └── sms/
        └── parser/
            ├── BancolombiaSmsParserTest.java
            └── SmsParserFactoryTest.java
```

### Test Types

| Type | Annotation | Scope | When to Use |
|---|---|---|---|
| Unit | None (plain JUnit 5) | Single class in isolation | Service logic, parser logic, mapper logic |
| Web slice | `@WebMvcTest(SomeController.class)` | Controller only, mocked service layer | Controller request/response mapping |
| Data slice | `@DataJpaTest` | Repository only | Repository queries, custom JPQL |
| Security slice | `@SpringBootTest` + `@WithMockUser` | Full auth flow | Authentication filter, JWT validation |
| Integration | `@SpringBootTest` | Multiple layers | End-to-end flows |

### Testing Patterns

**Service tests** — mock the repository:

```java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @InjectMocks private AuthService authService;

    @Test
    void register_whenEmailExists_shouldThrow() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.of(new User()));
        assertThrows(EmailAlreadyExistsException.class,
                () -> authService.register(new RegisterRequestDTO()));
    }
}
```

**Controller tests** — mock the service layer:

```java
@WebMvcTest(TransactionController.class)
class TransactionControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private TransactionService transactionService;
    @MockBean private TransactionMapper transactionMapper;

    @Test
    void createTransaction_shouldReturn201() throws Exception {
        // ...
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("..."))
                .andExpect(status().isOk());
    }
}
```

**Parser tests** — pure unit tests, no Spring needed:

```java
class BancolombiaSmsParserTest {
    private final BancolombiaSmsParser parser = new BancolombiaSmsParser();

    @Test
    void supports_shouldReturnTrueForBancolombiaMessage() {
        assertTrue(parser.supports("Recibiste $50.000 de Bancolombia"));
    }

    @Test
    void parse_shouldExtractAmount() {
        InternalTransactionRequest result = parser.parse("Recibiste $50.000 de Bancolombia");
        assertEquals(new BigDecimal("50000"), result.getAmount());
    }
}
```

---

## Database Standards

### Schema Management

- **Do NOT use `ddl-auto=update` in production.** The current setting (`application.properties:9`) is for development only.
- Use Flyway or Liquibase for production schema migrations.
- Naming convention for Flyway: `V<major>__<description>.sql` (e.g., `V1__initial_schema.sql`).

### Naming Conventions

| Element | Convention | Example |
|---|---|---|
| Tables | snake_case, plural | `users`, `transactions` |
| Columns | snake_case | `user_id`, `created_at`, `raw_message` |
| Primary keys | `id` | `id` |
| Foreign keys | `<referenced_table_singular>_id` | `user_id` |
| Join tables | `table1_table2` | `user_roles` |

### Indexing

- JPA/Hibernate auto-generates indexes for primary keys and unique constraints.
- Add explicit `@Index` annotations for columns used in `WHERE` clauses and `JOIN` conditions.
- Current tables lack custom indexes. The `transactions` table should have indexes on:
  - `user_id` (foreign key, used in all queries)
  - `type` (filtered in analytics)
  - `category` (grouped in analytics)

### Data Types

| Java | PostgreSQL | When |
|---|---|---|
| `Long` | `BIGINT` | IDs |
| `String` (short) | `VARCHAR(255)` | Default string length |
| `String` (long) | `TEXT` | Raw SMS messages, descriptions |
| `BigDecimal` | `DECIMAL(38,2)` | Monetary amounts |
| `LocalDateTime` | `TIMESTAMP` | Dates and timestamps |
| `Boolean` | `BOOLEAN` | Flags (`auto_detected`) |

---

## API Standards

### URL Conventions

- Base path: feature name, lowercase, plural where applicable.
- No version prefix currently. Add `/api/v1/` prefix when introducing versioning.

| Current Endpoint | Convention |
|---|---|
| `/auth/register` | Feature/action |
| `/auth/login` | Feature/action |
| `/auth/me` | Feature/current-resource |
| `/transactions` | Resource (plural) |
| `/transactions/sms` | Resource/sub-resource |
| `/user/me` | Resource/current-resource |
| `/analytics/summary` | Feature/sub-resource |
| `/analytics/expenses/categories` | Feature/sub-resource/nested |
| `/ingestion/sms` | Feature/sub-resource |

### HTTP Methods

| Method | Purpose | Convention |
|---|---|---|
| `POST` | Create a resource | `POST /transactions` |
| `GET` | Read/list resources | `GET /transactions`, `GET /transactions/{id}` |
| `PUT` | Full update of a resource | `PUT /user/me` |
| `PATCH` | Partial update | (not currently used) |
| `DELETE` | Delete a resource | `DELETE /transactions/{id}` |

**Important:** `GET` requests must be idempotent and safe. Login (`POST /auth/login`) is a state-changing operation and **must** use `POST`, not `GET`.

### Request/Response Format

- All requests and responses use `Content-Type: application/json`.
- Response DTOs are wrapped implicitly by Spring (`@RestController` ensures `@ResponseBody`).
- Error responses follow the `ErrorResponseDTO` contract.

### Authentication Header

```
Authorization: Bearer <jwt-token>
```

---

## Naming Conventions

### Packages

- All lowercase: `com.pocketminder.auth.controller`.
- One word per level, no hyphens or underscores.

### Classes

| Type | Pattern | Examples |
|---|---|---|
| Entity | `<EntityName>` | `User`, `Transaction` |
| DTO Request | `<Action><EntityName>DTO` or `<EntityName>RequestDTO` | `RegisterRequestDTO`, `CreateTransactionDTO` |
| DTO Response | `<EntityName>ResponseDTO` | `TransactionResponseDTO`, `UserResponseDTO` |
| Internal DTO | `Internal<EntityName>Request` | `InternalTransactionRequest` |
| Controller | `<Feature>Controller` | `AuthController`, `TransactionController` |
| Service | `<Feature>Service` | `AuthService`, `TransactionService` |
| Repository | `<Entity>Repository` | `UserRepository`, `TransactionRepository` |
| Mapper | `<Entity>Mapper` | `TransactionMapper`, `UserMapper` |
| Config | `<Feature>Config` | `SecurityConfig`, `ApplicationConfig` |
| Filter | `<Purpose>Filter` | `JwtAuthenticationFilter` |
| Exception | `<Description>Exception` | `EmailAlreadyExistsException`, `UnsupportedBankException` |
| Enum | `<Description>` | `TransactionType`, `TransactionCategory` |
| Factory | `<Target>Factory` | `SmsParserFactory` |

### Methods

| Layer | Method Naming |
|---|---|
| Controller | HTTP-method-derived: `register()`, `login()`, `createTransaction()`, `getMyTransactions()` |
| Service | Domain-derived: `register()`, `login()`, `createTransaction()`, `getCurrentUser()` |
| Repository | Spring Data derived: `findByEmail()`, `findByUser()`, custom: `getTotalByType()` |
| Mapper | `toResponse()` |

### Fields

- camelCase for Java fields.
- Database columns mapped via `@Column(name = "snake_case_name")`.

---

## Code Review Checklist

### Architecture & Design

- [ ] Does the code follow the package-by-feature convention?
- [ ] Are there any circular dependencies between feature packages?
- [ ] Does the code introduce unnecessary coupling across bounded contexts?
- [ ] Are DTOs used for all controller request/response types? (No entities exposed.)
- [ ] Are mappers used where entity-to-DTO conversion is needed?
- [ ] Could this change benefit from a domain event instead of a direct service call?

### Correctness

- [ ] Are `@Valid` annotations present on all controller request body parameters?
- [ ] Are `jakarta.validation` constraints appropriate and complete on request DTOs?
- [ ] Are `@Transactional` boundaries correct for multi-write operations?
- [ ] Are optimistic locking or version fields needed for concurrent updates?
- [ ] Are enum values stored as `STRING` via `@Enumerated(EnumType.STRING)`?
- [ ] Is `FetchType.LAZY` used on all `@ManyToOne` and `@OneToMany` relationships?

### Security

- [ ] Are new endpoints properly secured in `SecurityConfig.java`?
- [ ] Are `@PreAuthorize` annotations used for role-based access if needed?
- [ ] Are passwords and secrets never logged or returned in responses?
- [ ] Are input validation constraints sufficient to prevent injection attacks?
- [ ] Is CORS configuration updated if new origins need access?

### Error Handling

- [ ] Are business exceptions defined in `common.exception` and handled in `GlobalExceptionHandler`?
- [ ] Do error handlers use `HttpStatus` constants rather than hardcoded integers?
- [ ] Are sensitive implementation details excluded from error messages?
- [ ] Is the generic `Exception` catch-all avoided for known error cases?

### Performance

- [ ] Are database queries optimized with appropriate indexes? (Check `@Query` JPQL.)
- [ ] Is `@ManyToOne` using `FetchType.LAZY`? (EAGER is never acceptable.)
- [ ] Are analytics queries cached or using a read model for repeated access patterns?
- [ ] Are N+1 query patterns avoided? (Use `@EntityGraph` or `JOIN FETCH` where needed.)

### Testing

- [ ] Are unit tests added for new services, mappers, and parsers?
- [ ] Are `@WebMvcTest` tests added for new controllers?
- [ ] Are `@DataJpaTest` tests added for new or modified repository queries?
- [ ] Does the existing `PocketminderApplicationTests` still pass?
- [ ] Are tests independent (no shared mutable state, no test ordering dependencies)?

### Code Style

- [ ] Are all imports organized and unused imports removed?
- [ ] Are Lombok annotations used correctly (`@Data` vs `@Getter`/`@Setter`)?
- [ ] Does the code use constructor injection via `@RequiredArgsConstructor`?
- [ ] Are magic strings/numbers extracted to constants or enums?
- [ ] Are exception messages descriptive and user-meaningful?

### Database

- [ ] Is `ddl-auto=update` still acceptable? (Development only — fail the review for production.)
- [ ] Are Flyway/Liquibase migration scripts added for schema changes?
- [ ] Are new columns nullable unless explicitly required?
- [ ] Are `@Column` annotations present for non-default column configurations?

### Documentation

- [ ] Are complex business rules accompanied by a comment explaining the rationale?
- [ ] Are API changes reflected in the README or API documentation?
- [ ] Are new environment properties documented in `application-example.properties`?
- [ ] Is the `Architecture.md` updated for significant structural changes?

---

*This guide reflects the conventions found in the PocketMinder codebase as of 2026-06-03. When in doubt, follow the patterns established in the existing code over external best practices.*
