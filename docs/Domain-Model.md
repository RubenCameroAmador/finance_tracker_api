# PocketMinder — Domain Model

> **Version:** 1.0  
> **Last updated:** 2026-06-03  
> **Scope:** Backend monolith — all entities, value objects, aggregates, and domain services discovered in the codebase.

---

## Table of Contents

1. [Domain Overview](#domain-overview)
2. [Aggregates](#aggregates)
   - [User Aggregate](#user-aggregate)
   - [Transaction Aggregate](#transaction-aggregate)
3. [Value Objects](#value-objects)
   - [TransactionCategory](#transactioncategory)
   - [TransactionType](#transactiontype)
   - [TransactionSource](#transactionsource)
4. [Domain Services (Current)](#domain-services-current)
5. [Relationships](#relationships)
6. [Missing Domain Concepts](#missing-domain-concepts)
7. [Domain Inconsistencies](#domain-inconsistencies)
8. [Potential Aggregate Boundaries](#potential-aggregate-boundaries)

---

## Domain Overview

The PocketMinder domain spans personal financial management. The codebase currently defines **two JPA entities** and **three enums** serving as value objects. The domain is organized across two bounded contexts: **Identity** (user registration, authentication) and **Transaction** (income/expense recording, categorization, analytics).

### Domain Map

```
┌─────────────────────────────────────────────────────────────┐
│                   Identity Context                           │
│  ┌──────────┐                                                │
│  │   User   │  (Aggregate Root)                              │
│  │  (auth)  │                                                │
│  └────┬─────┘                                                │
│       │ owns                                                  │
│       ▼                                                      │
│  ┌──────────┐                                                │
│  │Transaction│ (Aggregate Root)                              │
│  │(transaction)                                              │
│  └──────────┘                                                │
│       │ uses                                                  │
│       ├── TransactionCategory  (Value Object / Enum)          │
│       ├── TransactionType      (Value Object / Enum)          │
│       └── TransactionSource    (Value Object / Enum)          │
└─────────────────────────────────────────────────────────────┘
```

---

## Aggregates

### User Aggregate

**File:** `src/main/java/com/pocketminder/auth/entity/User.java`  
**Package:** `com.pocketminder.auth.entity`  
**Table:** `users`

#### Purpose

Represents a registered user of the PocketMinder platform. The User is the central identity that owns all transactions and is the subject of authentication and authorization.

#### Attributes

| Attribute | Type | Constraints | Domain Meaning |
|---|---|---|---|
| `id` | `Long` | PK, auto-generated | Unique identifier |
| `name` | `String` | Not null | User's display name |
| `email` | `String` | Unique, not null | User's email address (also acts as username for authentication) |
| `password` | `String` | Not null | BCrypt-hashed password |

#### Business Rules

| Rule | Enforcement | Evidence |
|---|---|---|
| Email must be unique | `@Column(unique = true)` + `EmailAlreadyExistsException` in `AuthService.register()` | `User.java:26-27`, `AuthService.java:25-29` |
| Password must be hashed | `PasswordEncoder.encode()` called before persist | `AuthService.java:35` |
| Email is the username for auth | `getUsername()` returns `email` | `User.java:39-41` |
| No roles/authorities yet | `getAuthorities()` returns `List.of()` | `User.java:32-36` |
| All UserDetails flags are always true | `isAccountNonExpired()`, `isAccountNonLocked()`, `isCredentialsNonExpired()`, `isEnabled()` all return `true` | `User.java:44-66` |

#### Relationships

| Relationship | Type | Target | Mapped By |
|---|---|---|---|
| User → Transactions | 1:N (one-to-many, implicit) | `Transaction` | `Transaction.user` (`@ManyToOne`) |

#### Identity

The `User` entity is identified by `id` (database identity). Uniqueness is also enforced on `email` at the database level.

#### Current Issues

1. **Entity implements `UserDetails`** — The `User` aggregate root implements Spring Security's `UserDetails` interface (`User.java:18`), mixing framework concerns with domain identity. Four of the six `UserDetails` methods return hardcoded `true`, indicating they are not domain-relevant.
2. **`name` is mutable via `@Setter`** — `UserService.updateProfile()` (`UserService.java:27-33`) modifies the name directly through `@Setter`, which is acceptable for current requirements but bypasses domain encapsulation.
3. **No email verification status** — No `emailVerified` or `emailVerifiedAt` flag exists, so the domain cannot distinguish between registered and verified users.
4. **No timestamps** — No `createdAt` or `updatedAt` fields on the `users` table, making it impossible to audit when accounts were created.

---

### Transaction Aggregate

**File:** `src/main/java/com/pocketminder/transaction/entity/Transaction.java`  
**Package:** `com.pocketminder.transaction.entity`  
**Table:** `transactions`

#### Purpose

Records a single financial movement — an income, expense, or transfer. The Transaction is the core data entity around which analytics, categorization, SMS ingestion, and future AI insights revolve.

#### Attributes

| Attribute | Type | Constraints | Domain Meaning |
|---|---|---|---|
| `id` | `Long` | PK, auto-generated | Unique identifier |
| `title` | `String` | Not null | Short description (e.g., merchant name) |
| `description` | `String` | Nullable | Longer description or memo |
| `amount` | `BigDecimal` | Not null | Monetary value (always positive in persistence; the `type` field determines direction) |
| `type` | `TransactionType` (enum) | Not null | INCOME, EXPENSE, or TRANSFER |
| `category` | `TransactionCategory` (enum) | Not null | Classification for analytics (FOOD, TRANSPORT, etc.) |
| `source` | `TransactionSource` (enum) | Not null | Origination channel (MANUAL, SMS, BANK_API, etc.) |
| `rawMessage` | `String` | Nullable | Original SMS or import text (for auto-detected transactions) |
| `detectedMerchant` | `String` | Nullable | Extracted merchant name (for auto-detected transactions) |
| `autoDetected` | `Boolean` | Nullable | Flag indicating if transaction was created automatically |
| `transactionDate` | `LocalDateTime` | Nullable | When the transaction occurred (distinct from `createdAt`) |
| `createdAt` | `LocalDateTime` | Nullable | When the record was created in the system |
| `user` | `User` (entity ref) | Not null (FK) | The owning user |

#### Business Rules

| Rule | Enforcement | Evidence |
|---|---|---|
| A transaction belongs to exactly one user | `@ManyToOne` with nullable=false on `user` | `Transaction.java:48-50` |
| Auto-detected transactions carry the raw source message | `rawMessage` set during SMS ingestion | `BancolombiaSmsParser.java:76` |
| Auto-detected transactions record the parsed merchant | `detectedMerchant` set during SMS ingestion | `BancolombiaSmsParser.java:77` |
| Manual transactions default to `source=MANUAL`, `autoDetected=false` | Set in `TransactionController.java:35-36` | `TransactionController.java:35-36` |
| Manual transactions use current timestamp as transaction date | `transactionDate = LocalDateTime.now()` | `TransactionController.java:37` |
| `createdAt` is set to current time on creation | `createdAt(LocalDateTime.now())` in `TransactionService` | `TransactionService.java:45` |
| SMS-sourced transactions always receive `category=OTHER` | Hardcoded in `BancolombiaSmsParser.java:74` | `BancolombiaSmsParser.java:74` |

#### Relationships

| Relationship | Type | Target | Mapped By |
|---|---|---|---|
| Transaction → User | N:1 (many-to-one) | `User` | `user` field |

#### Identity

The `Transaction` entity is identified by `id` (database identity). No business key exists.

#### Current Issues

1. **Direct object reference to `User` aggregate** — `Transaction.java:48-50` holds a `User user` reference via `@ManyToOne`. This means loading a Transaction forces a relationship to another aggregate root, creating tight coupling. DDD recommends referencing other aggregates by identity (`Long userId`) rather than by object reference.
2. **`amount` has no currency** — `BigDecimal` represents the value with no associated currency. The domain implicitly assumes a single currency (likely COP based on Bancolombia SMS format). This prevents multi-currency support.
3. **`rawMessage` column too short** — `String` defaults to `VARCHAR(255)`, which may truncate longer SMS messages.
4. **No `updatedAt` field** — The entity records `createdAt` but has no `updatedAt` for tracking modifications.
5. **`amount` could be negative** — No validation ensures `amount` is positive. The `type` field (INCOME/EXPENSE/TRANSFER) is intended to carry the direction, but nothing prevents storing a negative `amount`.

---

## Value Objects

### TransactionCategory

**File:** `src/main/java/com/pocketminder/transaction/entity/TransactionCategory.java`  
**Package:** `com.pocketminder.transaction.entity`  
**Type:** Enum

#### Purpose

Classifies a transaction into a spending or income category for analytics and budgeting.

#### Values

| Value | Meaning |
|---|---|
| `FOOD` | Groceries, restaurants, food delivery |
| `TRANSPORT` | Public transit, fuel, ride-sharing |
| `ENTERTAINMENT` | Movies, games, streaming services |
| `HEALTH` | Medical, pharmacy, wellness |
| `SHOPPING` | Retail, online purchases, clothing |
| `EDUCATION` | Tuition, courses, books |
| `BILLS` | Utilities, rent, subscriptions |
| `SALARY` | Employment income |
| `INVESTMENT` | Stocks, crypto, savings interest |
| `OTHER` | Catch-all for unclassified transactions |

#### Business Rules

| Rule | Evidence |
|---|---|
| SMS-ingested transactions always default to `OTHER` | `BancolombiaSmsParser.java:74` |
| Manual transactions require explicit category selection | `CreateTransactionDTO.java:24-25` (`@NotNull TransactionCategory category`) |

#### Current Issues

1. **Hardcoded enum** — Users cannot create custom categories or sub-categories. This is a significant limitation for a personal finance application where categorization is highly personal.
2. **`OTHER` is a dumping ground** — Without a categorization pipeline (rules or ML), all auto-detected transactions land in `OTHER`, making the category useless for analytics.
3. **No `UNCATEGORIZED` state** — There is no way to represent "not yet categorized" vs. "intentionally categorized as OTHER."

---

### TransactionType

**File:** `src/main/java/com/pocketminder/transaction/entity/TransactionType.java`  
**Package:** `com.pocketminder.transaction.entity`  
**Type:** Enum

#### Purpose

Determines the financial direction of a transaction.

#### Values

| Value | Meaning |
|---|---|
| `INCOME` | Money received (salary, transfer-in, cash-in) |
| `EXPENSE` | Money spent (purchase, bill payment, withdrawal) |
| `TRANSFER` | Movement between user's own accounts (currently unused in business logic) |

#### Business Rules

| Rule | Evidence |
|---|---|
| INCOME/EXPENSE are inferred from SMS text | `BancolombiaSmsParser.java:42-51` checks for `"recibiste"` → INCOME |
| Manual transactions require explicit type selection | `CreateTransactionDTO.java:21-22` (`@NotNull TransactionType type`) |
| Analytics SUM queries use type to compute income vs. expenses | `TransactionRepository.java:24-33` |

#### Current Issues

1. **`TRANSFER` defined but unused** — The `TRANSFER` enum value exists (`TransactionType.java:6`) but no code reads or processes it. Transfers would need both a source and destination (e.g., "checking → savings"), which the current Transaction entity cannot represent.
2. **Type is stored as STRING** — Using `@Enumerated(EnumType.STRING)` is correct and allows safe addition of new values.

---

### TransactionSource

**File:** `src/main/java/com/pocketminder/transaction/entity/TransactionSource.java`  
**Package:** `com.pocketminder.transaction.entity`  
**Type:** Enum

#### Purpose

Identifies how a transaction entered the system.

#### Values

| Value | Meaning | Status |
|---|---|---|
| `MANUAL` | User-entered via the API | Active (`TransactionController.java:35`) |
| `SMS` | Parsed from an SMS message | Active (`BancolombiaSmsParser.java:75`) |
| `BANK_API` | Pulled from a bank API integration | Defined, not implemented |
| `CSV_IMPORT` | Imported from a CSV file | Defined, not implemented |
| `AI_PARSER` | Parsed by an AI/ML service | Defined, not implemented |

#### Business Rules

| Rule | Evidence |
|---|---|
| Manual transactions always set `source=MANUAL` | `TransactionController.java:35` |
| SMS transactions always set `source=SMS` | `BancolombiaSmsParser.java:75` |

#### Current Issues

1. **Three unused values** — `BANK_API`, `CSV_IMPORT`, and `AI_PARSER` are defined but no implementation exists to produce transactions with these sources. They are placeholder values for future features.

---

## Domain Services (Current)

The codebase does not define explicit domain services in the DDD sense. Current "services" are Spring `@Service` classes that mix application orchestration with business logic.

| Class | Package | Classification | Responsibility |
|---|---|---|---|
| `AuthService` | `auth.service` | Application Service | Orchestrates registration (validation → encoding → persist) and login (authenticate → token generation) |
| `UserService` | `user.service` | Application Service | Resolves the current authenticated user from `SecurityContextHolder`, updates profile name |
| `TransactionService` | `transaction.service` | Application Service | Creates transactions (builds entity from request, sets `createdAt`, saves) and retrieves user's transactions |
| `SmsIngestionService` | `ingestion.sms.service` | Application Service | Coordinates SMS parsing pipeline: get parser → parse → create transaction |
| `AnalyticsService` | `transaction.analytics.service` | Application Service | Computes financial summary and category breakdown by querying repository aggregates |
| `JwtService` | `auth.security` | Infrastructure Service | Generates and validates JWT tokens (not domain logic, infrastructure concern) |
| `CustomUserDetailsService` | `auth.service` | Infrastructure Service | Loads User entity by email for Spring Security authentication |
| `SmsParserFactory` | `ingestion.sms.parser` | Factory | Selects the appropriate `SmsParser` implementation for a given message |

---

## Relationships

### Entity-Relationship Diagram (Code-Derived)

```
┌───────────────────────────────────────────┐
│                 User                       │
│  id          (Long, PK)                   │
│  name        (String)                     │
│  email       (String, UNIQUE)             │
│  password    (String)                     │
└────────────────────┬──────────────────────┘
                     │ 1
                     │ owns
                     │ *
┌────────────────────▼──────────────────────┐
│              Transaction                    │
│  id               (Long, PK)               │
│  user_id          (Long, FK → users.id)    │
│  title            (String)                 │
│  description      (String)                 │
│  amount           (BigDecimal)             │
│  type             (TransactionType:ENUM)   │
│  category         (TransactionCategory:ENUM)│
│  source           (TransactionSource:ENUM) │
│  raw_message      (String)                 │
│  detected_merchant (String)                │
│  auto_detected    (Boolean)                │
│  transaction_date (LocalDateTime)          │
│  created_at       (LocalDateTime)          │
└────────────────────────────────────────────┘
```

### Relationship Rules

| Rule | Evidence |
|---|---|
| A User can have zero or more Transactions | `Transaction` has `@ManyToOne → User` with no uniqueness constraint |
| A Transaction belongs to exactly one User | `Transaction.java:48-50` — `@ManyToOne` with foreign key |
| Transactions are not shared between users | No `sharedWith` or `splitBetween` fields exist |
| Deleting a User cascades to Transactions | Not explicitly configured (Hibernate default: no cascade) |
| TransactionCategory, TransactionType, TransactionSource are owned by the Transaction | Stored as enum strings within the `transactions` table |

---

## Missing Domain Concepts

These concepts are absent from the current codebase but are either implied by the README roadmap, the `TransactionSource` enum, or typical PFM domain requirements.

| Missing Concept | Reason Needed | Evidence of Intent |
|---|---|---|
| **Money** (Value Object) | `BigDecimal` with no currency is used everywhere. A `Money` VO wrapping `amount + currency` is needed for multi-currency support. | `Transaction.java:27` |
| **Email** (Value Object) | Email is stored as a plain `String`. A VO would encapsulate format validation and normalization. | `User.java:27` |
| **Password** (Value Object) | Password is a `String`. A VO could enforce strength rules and encapsulate hashing. | `User.java:29` |
| **RawMessage** (Value Object) | SMS raw text is a `String`. A VO would enforce encoding and size constraints. | `Transaction.java:38` |
| **Budget** (Aggregate) | No budget or spending limit entity exists. The README roadmap mentions "savings goals" and "budget predictions." | `README.md` roadmap |
| **SavingsGoal** (Aggregate) | No savings goal tracking. README mentions "build saving habits." | `README.md` roadmap |
| **FinancialAccount** (Aggregate) | No bank account, credit card, or wallet entity. Transactions are not associated with any account. | Missing from all entities |
| **Merchant** (Value Object) | Merchant is stored as a free-text `String`. A VO would normalize merchant names (e.g., "MCDONALD'S" → "McDonald's"). | `Transaction.java:40` |
| **Tags** | A single category enum is insufficient for flexible classification. Tags allow cross-cutting labels (e.g., "tax-deductible," "recurring"). | Missing |
| **RecurringTransaction** | No support for recurring bills or subscriptions. The Transaction entity has no recurrence pattern. | Missing |
| **TransactionSplit** | No ability to split a transaction across multiple categories. | Missing |
| **UserSettings** (Value Object) | No user preferences (currency preference, notification preferences, category customization). | Missing |
| **AuditLog** (Entity) | No record of who changed what and when. | `createdAt` exists on `Transaction` but no `updatedAt` on any entity |
| **RefreshToken** (Entity) | No refresh token mechanism exists to complement JWT access tokens. | JWT has 1-hour expiry with no refresh flow |
| **Role / Permission** (Value Object) | No roles or authorities beyond empty `List.of()` in `getAuthorities()`. | `User.java:35` |

---

## Domain Inconsistencies

### 1. `User` Entity Lives in `auth` Package but Serves Multiple Contexts

The `User` entity is defined in `com.pocketminder.auth.entity` but is used by:
- `user.service.UserService` (profile management)
- `transaction.service.TransactionService` (transaction ownership)
- `transaction.analytics.service.AnalyticsService` (analytics scoping)
- `transaction.repository.TransactionRepository` (JPQL queries)

This creates a situation where the Identity context's entity becomes a dependency for Transaction and User profile contexts. In DDD terms, the `User` would ideally live in its own bounded context (`identity`) and be referenced by other contexts only by identity (`userId: Long`).

### 2. `InternalTransactionRequest` is a Leaky Abstraction

`InternalTransactionRequest.java` lives in `transaction.dto` but is constructed by:
- `TransactionController.java:28-38` (for manual transactions)
- `BancolombiaSmsParser.java:68-79` (for SMS transactions)

It serves as an ad-hoc command object but lacks clear ownership. Neither the controller nor the parser should construct an "internal" DTO. Both should create proper domain objects or application-layer commands.

### 3. Transaction Amount Direction is Ambiguous

The `amount` field is `BigDecimal` with no sign validation. The `type` field (INCOME/EXPENSE) determines direction. This means:
- An income of $100 is stored as `amount=100, type=INCOME`
- An expense of $50 is stored as `amount=50, type=EXPENSE`

This is correct, but nothing prevents storing `amount=-50, type=EXPENSE` (double negative). No validation enforces that `amount` must be positive.

### 4. SMS-Categorized Transactions Always Get `OTHER`

`BancolombiaSmsParser.java:74` hardcodes `category(TransactionCategory.OTHER)` for all parsed SMS transactions. This means every SMS-ingested transaction lands in the uncategorized bucket regardless of merchant or content. The `TransactionCategory` enum has rich categorization (FOOD, TRANSPORT, etc.) but it is never applied to auto-detected transactions.

### 5. Manual Transactions Have No Date Selection

`CreateTransactionDTO.java` has no `transactionDate` field. The `TransactionController.java:37` hardcodes `transactionDate = LocalDateTime.now()`. A user creating a transaction for a past date cannot specify it.

### 6. No Domain Events

Currently, no domain events are defined or published. Key domain events that are absent:
- `UserRegistered` — would allow triggering welcome email, analytics setup
- `TransactionCreated` — would trigger categorization, analytics update, AI insight generation
- `TransactionCategorized` — would allow learning algorithms to improve

### 7. Two Domain Objects Are Misplaced as Infrastructure DTOs

`FinancialSummaryDTO` and `CategorySummaryDTO` live in `transaction.analytics.dto` but represent domain concepts:
- `FinancialSummary` (income total, expense total, balance) is a **domain read model**
- `CategorySummary` (category, total) is a **domain read model**

These are currently constructed by JPQL queries in the repository layer (`TransactionRepository.java:36-38`), which is an infrastructure concern leaking into the domain query.

### 8. `TransactionSource.AI_PARSER` is Defined but Useless

The enum value `AI_PARSER` exists but no implementation produces it. The only auto-detection source currently is `SMS`. This suggests the enum was designed for future expansion but has created dead code.

---

## Potential Aggregate Boundaries

As the domain evolves, the following aggregate boundaries should be considered:

### Current Aggregates

| Aggregate | Root Entity | Repository | Owned Value Objects |
|---|---|---|---|
| **User** | `User` | `UserRepository` | (none currently) |
| **Transaction** | `Transaction` | `TransactionRepository` | `TransactionCategory`, `TransactionType`, `TransactionSource` |

### Future Aggregates

#### 1. Budget Aggregate

```
Budget (Aggregate Root)
├── budgetId: Long
├── userId: UserId (reference by identity)
├── period: BudgetPeriod (MONTHLY, WEEKLY, YEARLY)
├── categoryLimits: List<CategoryLimit>
│   ├── category: TransactionCategory
│   └── limit: Money
└── spent: Money  (computed or stored)
```

**Reason:** Budgeting is a core roadmap feature with its own consistency boundary — a Budget manages its own spending limits independently of individual Transactions.

#### 2. SavingsGoal Aggregate

```
SavingsGoal (Aggregate Root)
├── savingsGoalId: Long
├── userId: UserId (reference by identity)
├── targetAmount: Money
├── currentAmount: Money
├── targetDate: LocalDate
├── name: String
└── contributions: List<Contribution>
    ├── amount: Money
    └── date: LocalDateTime
```

**Reason:** A savings goal manages its own progress independently. Contributions are immutable historical records within the goal.

#### 3. FinancialAccount Aggregate

```
FinancialAccount (Aggregate Root)
├── accountId: Long
├── userId: UserId
├── name: String (e.g., "Checking", "Credit Card")
├── type: AccountType (CHECKING, SAVINGS, CREDIT_CARD, CASH)
├── balance: Money
├── institution: String (e.g., "Bancolombia")
└── lastSyncedAt: LocalDateTime
```

**Reason:** Transactions should belong to an account, not directly to a user. This allows for balance tracking, account-level analytics, and multi-account support. The current `Transaction → User` relationship would become `Transaction → FinancialAccount → User`.

#### 4. RecurringTransaction Aggregate

```
RecurringTransaction (Aggregate Root)
├── recurringId: Long
├── userId: UserId
├── template: TransactionTemplate (description, amount, category)
├── frequency: Frequency (DAILY, WEEKLY, MONTHLY, YEARLY)
├── nextOccurrence: LocalDate
└── active: Boolean
```

**Reason:** Recurring transactions have their own lifecycle (create, pause, resume, archive) distinct from individual Transaction records they generate.

### Aggregate Relationship Diagram (Future State)

```
User
 ┣━━ FinancialAccount (N)
 ┃    ┗━━ Transaction (N)
 ┣━━ Budget (N)
 ┣━━ SavingsGoal (N)
 ┗━━ RecurringTransaction (N)

Relationships are by identity (userId: Long), not by object reference.
```

---

*This domain model is derived from analysis of 44 Java source files in the PocketMinder repository as of 2026-06-03. Every attribute, rule, and relationship is directly evidenced from the codebase.*
