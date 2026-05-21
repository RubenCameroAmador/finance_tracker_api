# PocketMinder Backend

PocketMinder is a modern personal finance platform focused on helping users easily track expenses, manage income, build saving habits, and receive AI-powered financial insights.

The goal of the project is to create a simple and intuitive experience for users who struggle to maintain consistency with traditional budgeting applications.

---

# Project Vision

Most financial apps fail because they require too much manual effort.

PocketMinder aims to solve that problem by combining:

- Expense tracking
- Income management
- Savings goals
- Smart categorization
- Financial analytics
- AI-powered recommendations
- Automation-first UX

Future versions will support automatic transaction recognition from mobile notifications/SMS and personalized financial guidance powered by AI.

---

# Current Backend Features

## Authentication Module

Implemented features:

- User registration
- User login
- JWT authentication
- Password hashing with BCrypt
- Protected endpoints
- Spring Security integration

---

# Tech Stack

## Backend

- Java 17
- Spring Boot 3
- Spring Security
- Spring Data JPA
- Hibernate
- PostgreSQL
- JWT Authentication
- Maven

---

# Architecture

The project follows a simplified clean architecture approach.

```text
com.pocketminder

├── auth
│   ├── controller
│   ├── service
│   ├── repository
│   ├── entity
│   ├── dto
│   ├── mapper
│   ├── security
│   └── config
│
├── common
│
└── PocketMinderApplication
```

---

# Architecture Layers

| Layer | Responsibility |
|---|---|
| Controller | Handles HTTP requests/responses |
| Service | Business logic |
| Repository | Database access |
| Entity | Database models |
| DTO | Request/response contracts |
| Mapper | Object transformations |
| Security | JWT & authentication |
| Config | Spring configuration |

---

# Database

Current database:

- PostgreSQL

ORM:

- JPA / Hibernate

---

# Security

Implemented security features:

- BCrypt password hashing
- JWT token generation
- Stateless authentication
- Protected routes using Spring Security

---

# API Endpoints

## Public Endpoints

### Register

```http
POST /auth/register
```

Request body:

```json
{
  "name": "Ruben",
  "email": "ruben@test.com",
  "password": "123456"
}
```

---

### Login

```http
POST /auth/login
```

Request body:

```json
{
  "email": "ruben@test.com",
  "password": "123456"
}
```

---

## Protected Endpoint Example

```http
GET /auth/me
```

Header:

```http
Authorization: Bearer YOUR_JWT_TOKEN
```

---

# Local Development Setup

## Requirements

- Java 17
- PostgreSQL
- IntelliJ IDEA (recommended)

---

# Clone Repository

```bash
git clone YOUR_REPOSITORY_URL
```

---

# Configure Environment

Create:

```text
src/main/resources/application.properties
```

based on:

```text
src/main/resources/application-example.properties
```

---

# Example Configuration

```properties
spring.application.name=PocketMinder

# DATABASE
spring.datasource.url=jdbc:postgresql://localhost:5432/pocketminder
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# JWT
jwt.secret=YOUR_SECRET_KEY
```

---

# Run Application

Using Maven Wrapper:

```bash
./mvnw spring-boot:run
```

---

# Future Roadmap

## Backend

- Refresh tokens
- Role-based authorization
- Global exception handling
- Docker support
- API documentation
- Unit and integration testing
- CI/CD pipelines

---

## Features

- Expense categories
- Savings goals
- Financial analytics dashboard
- AI financial assistant
- Smart transaction recognition
- Mobile app integration
- Notification parsing
- Budget predictions

---

# Project Status

Current phase:

- Authentication & Security Foundation

Next phase:

- UserDetailsService
- AuthenticationProvider
- Production-grade JWT validation
- Global exception handling

---

# Author

Developed by Ruben Camero as a learning-focused real-world backend project using modern Java and Spring Boot practices.