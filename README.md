# AI Workout Architect

> **Enterprise-Grade, Intelligent Fitness Orchestration & Workout Synthesis Platform**

[![Angular](https://img.shields.io/badge/Angular-17%2B-DD0031?style=for-the-badge&logo=angular&logoColor=white)](https://angular.dev/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3%2B-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring AI](https://img.shields.io/badge/Spring_AI-Gemini_API-4285F4?style=for-the-badge&logo=google&logoColor=white)](https://spring.io/projects/spring-ai)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Supabase-336791?style=for-the-badge&logo=postgresql&logoColor=white)](https://supabase.com/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-3%2B-38B2AC?style=for-the-badge&logo=tailwind-css&logoColor=white)](https://tailwindcss.com/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5%2B-3178C6?style=for-the-badge&logo=typescript&logoColor=white)](https://www.typescriptlang.org/)

---

## 📋 Table of Contents

- [Executive Summary](#-executive-summary)
- [System Architecture](#-system-architecture)
  - [Architectural Data Flow](#architectural-data-flow)
  - [Design Principles & Separation of Concerns](#design-principles--separation-of-concerns)
- [Core Features](#-core-features)
- [Tech Stack & Tooling](#-tech-stack--tooling)
- [Local Development Setup](#-local-development-setup)
  - [Prerequisites](#prerequisites)
  - [Environment Configuration](#environment-configuration)
  - [Running the Backend](#running-the-backend)
  - [Running the Frontend](#running-the-frontend)
- [Future Roadmap](#-future-roadmap)
- [License](#-license)

---

## 🏢 Executive Summary

In today's digital health ecosystem, most automated workout planners rely on rigid rule-based engines or generic templates that fail to account for biomechanical nuances, personal constraints, and progressive overload demands. Conversely, naive generative AI implementations frequently suffer from non-deterministic responses, hallucinations, schema drift, and an absence of domain-level validation.

**AI Workout Architect** bridges this gap by delivering an enterprise-ready, AI-driven fitness architecture. The platform harnesses **Google Gemini** through **Spring AI** to dynamically engineer, validate, and iteratively evolve customized training regimens tailored to individual biometrics, injury history, training goals, and available equipment.

### Why This Architecture?

- **Enterprise-Grade Separation of Concerns**: Clean isolation between presentation (Angular), core business orchestration (Spring Boot), persistence (PostgreSQL / Supabase via Hibernate), and generative intelligence (Spring AI).
- **Deterministic AI via Type-Safe Structured Outputs**: Eliminates unstructured markdown parsing by mapping LLM payloads directly into validated, immutable Java records and POJOs.
- **Horizontal Scalability & Statelessness**: A decoupled stateless backend ready for cloud-native deployment and independent horizontal scaling.
- **Strict Data Integrity**: Strong relational modeling and constraint enforcement at both the ORM (JPA/Hibernate) and database layers.

---

## 🏛️ System Architecture

### Architectural Data Flow

```
+-----------------------------------------------------------------------------------+
|                                 CLIENT LAYER                                      |
|  +-----------------------------------------------------------------------------+  |
|  |                 Angular 17+ Single-Page Application (SPA)                   |  |
|  |    (Standalone Components, Signals/RxJS, Reactive Forms, Tailwind CSS)      |  |
|  +---------------------------------------+-------------------------------------+  |
+------------------------------------------|----------------------------------------+
                                           | HTTPS / JSON REST
                                           v
+-----------------------------------------------------------------------------------+
|                             BACKEND SERVICE LAYER                                 |
|  +-----------------------------------------------------------------------------+  |
|  |                       Spring Boot 3 REST Controllers                        |  |
|  |                (Request Validation, DTO Mapping, Security)                  |  |
|  +---------------------------------------+-------------------------------------+  |
|                                          |                                        |
|                                          v                                        |
|  +-----------------------------------------------------------------------------+  |
|  |                      Core Domain & Service Layer                            |  |
|  |           (Workout Strategy, Biometric Validation, History Tracking)        |  |
|  +-------------------+-----------------------------------+---------------------+  |
|                      |                                   |                        |
|                      v (AI Orchestration)                v (Persistence)          |
|  +-------------------------------------+   +-----------------------------------+  |
|  |        Spring AI Engine             |   |        Spring Data JPA /          |  |
|  |   Structured Prompt Engineering     |   |          Hibernate ORM            |  |
|  |    & Output Converters (Records)    |   |    (Entity Lifecycles & Rel.)     |  |
|  +-------------------+-----------------+   +-----------------+-----------------+  |
+----------------------|---------------------------------------|--------------------+
                       |                                       |
                       | gRPC / REST (TLS)                     | JDBC Connection Pool
                       v                                       v
+------------------------------------+   +------------------------------------------+
|          AI PROVIDER               |   |            PERSISTENCE TIER              |
|  Google Gemini Large Language      |   |   PostgreSQL Database (Supabase Managed) |
|  Model (Structured Output Engine)  |   |   (User Biometrics, Workouts, Logs)      |
+------------------------------------+   +------------------------------------------+
```

### Design Principles & Separation of Concerns

1. **Angular Client**: Collects biometric parameters, user constraints, and target split preferences through reactive forms with client-side validation. Manages presentation state cleanly using modern Angular features (Signals, standalone architecture).
2. **Spring Boot Orchestrator**:
   - Sanitizes and validates incoming payloads.
   - Enriches domain prompts with historical workout volume and progressive overload context.
   - Dispatches structured prompts to the Gemini model using `ChatClient` from Spring AI.
   - Serializes generated output directly into immutable Java Records (`WorkoutPlanRecord`, `ExerciseRoutineRecord`, `SetSchemeRecord`).
3. **Relational Persistence**: Maps domain entities through Spring Data JPA into PostgreSQL hosted on Supabase, establishing auditable change logs, versioned workout iterations, and historical progress tracking.

---

## ⚡ Core Features

- **Dynamic LLM Workout Generation**: Synthesizes custom, multi-week meso-cycles calibrated to target volume, training splits, muscle groups, and movement patterns.
- **Biometric & Environmental Constraint Adaptation**: Accounts for physical limitations, previous injuries, target heart-rate zones, and available equipment (e.g., commercial gym vs. home dumbbells).
- **Structured Output Guarantees**: Employs Spring AI's `BeanOutputConverter` and low-temperature prompting to guarantee schema conformity and prevent runtime deserialization failures.
- **Progressive Overload Adaptation Engine**: Ingests completed workout logs and feedback to programmatically calculate fatigue indicators and adjust subsequent volume and intensity.
- **Robust Persistence & Auditing**: Stores point-in-time workout plans, individual exercise sets, and biometric baselines in PostgreSQL with foreign-key constraints and index optimization.
- **Modular, Reactive UI**: Designed with Angular 17+ and Tailwind CSS to provide a fast, accessible, and responsive user experience across desktop and mobile devices.

---

## 🛠️ Tech Stack & Tooling

| Domain | Technology | Purpose |
| :--- | :--- | :--- |
| **Frontend** | [Angular 17+](https://angular.dev/) | Enterprise SPA framework, Standalone Components & Signals |
| | [TypeScript 5+](https://www.typescriptlang.org/) | Strongly-typed client application logic |
| | [Tailwind CSS](https://tailwindcss.com/) | Utility-first CSS framework for custom responsive design |
| | [RxJS](https://rxjs.dev/) | Reactive streams and asynchronous event handling |
| **Backend** | [Java 17 LTS](https://www.oracle.com/java/) | Modern LTS Java runtime (Records, Pattern Matching) |
| | [Spring Boot 3+](https://spring.io/projects/spring-boot) | Core enterprise service framework |
| | [Spring Web](https://docs.spring.io/spring-framework/reference/web/webmvc.html) | RESTful API endpoints and HTTP orchestration |
| | [Spring Security](https://spring.io/projects/spring-security) | Application layer security and authorization |
| | [Spring Data JPA](https://spring.io/projects/spring-data-jpa) | Declarative data repositories and transaction management |
| | [Hibernate ORM](https://hibernate.org/orm/) | Object-Relational Mapping and SQL generation |
| **AI Integration**| [Spring AI](https://spring.io/projects/spring-ai) | LLM abstraction, prompt templates, and structured output |
| | [Google Gemini API](https://ai.google.dev/) | High-performance multimodal LLM engine |
| **Database** | [PostgreSQL](https://www.postgresql.org/) | ACID-compliant relational persistence |
| | [Supabase](https://supabase.com/) | Cloud-hosted managed PostgreSQL platform |
| **Tooling & Build**| [Apache Maven](https://maven.apache.org/) | Backend build, dependency, and lifecycle management |
| | [Node.js & npm](https://nodejs.org/) | Frontend package management and build tooling |
| | [Angular CLI](https://angular.dev/tools/cli) | Client development server and production bundler |

---

## 💻 Local Development Setup

### Prerequisites

Ensure you have the following installed in your local development environment:

- **Java Development Kit (JDK)**: Version 17 or higher
- **Apache Maven**: Version 3.8+ (or use the provided `mvnw` wrapper)
- **Node.js**: Version 18.x or 20.x (LTS recommended)
- **npm**: Version 9.x or higher
- **Angular CLI**: Version 17+ (`npm install -g @angular/cli`)
- **Supabase / PostgreSQL Account**: Accessible connection string
- **Google AI Studio API Key**: Active API key for Gemini access

---

### Environment Configuration

#### Backend (`backend/src/main/resources/application.properties` or environment variables)

Configure your database and AI provider credentials:

```properties
# ==============================================================================
# SERVER CONFIGURATION
# ==============================================================================
server.port=8080
server.servlet.context-path=/api/v1

# ==============================================================================
# DATASOURCE CONFIGURATION (PostgreSQL / Supabase)
# ==============================================================================
spring.datasource.url=jdbc:postgresql://${DB_HOST:db.your-supabase-project.supabase.co}:${DB_PORT:5432}/${DB_NAME:postgres}?sslmode=require
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:your_secure_password}
spring.datasource.driver-class-name=org.postgresql.Driver

# ==============================================================================
# JPA / HIBERNATE CONFIGURATION
# ==============================================================================
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true

# ==============================================================================
# SPRING AI & GOOGLE GEMINI CONFIGURATION
# ==============================================================================
spring.ai.gemini.api-key=${GEMINI_API_KEY:your_gemini_api_key_here}
spring.ai.gemini.chat.options.model=gemini-1.5-pro
spring.ai.gemini.chat.options.temperature=0.2

# ==============================================================================
# CORS CONFIGURATION (Local Development)
# ==============================================================================
application.cors.allowed-origins=http://localhost:4200
```

> **Security Note**: Never commit actual credentials, passwords, or production API keys to source control. Use environment variables or a `.env` file for local development.

---

### Running the Backend

1. Navigate to the backend directory:
   ```bash
   cd backend
   ```
2. Build the application and download dependencies:
   ```bash
   ./mvnw clean install
   # For Windows Command Prompt / PowerShell:
   # .\mvnw.cmd clean install
   ```
3. Start the Spring Boot application:
   ```bash
   ./mvnw spring-boot:run
   # For Windows Command Prompt / PowerShell:
   # .\mvnw.cmd spring-boot:run
   ```
4. Verify the backend health and API availability:
   ```bash
   curl http://localhost:8080/api/v1/health
   ```

---

### Running the Frontend

1. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```
2. Install frontend dependencies:
   ```bash
   npm install
   ```
3. Start the Angular development server:
   ```bash
   ng serve --open
   ```
4. The client will be accessible at `http://localhost:4200`. API requests will proxy to `http://localhost:8080/api/v1`.

---

## 🗺️ Future Roadmap

This repository follows an iterative enterprise engineering lifecycle. Upcoming architectural enhancements include:

- [ ] **Containerization & Orchestration**:
  - Multi-stage `Dockerfile` definitions for backend and frontend.
  - `docker-compose.yml` for unified local orchestration including local PostgreSQL test containers.
- [ ] **Identity & Access Management (IAM)**:
  - Integration of Spring Security with OAuth2 / OpenID Connect (OIDC) and JWT authentication.
  - Role-Based Access Control (RBAC) supporting Athlete and Trainer profiles.
- [ ] **Robust CI/CD Pipelines**:
  - GitHub Actions automated workflows for linting, build validation, and vulnerability scanning.
  - Automated deployment pipelines targeting AWS (ECS / Fargate) or Google Cloud Run.
- [ ] **Comprehensive Test Suites**:
  - Unit tests utilizing JUnit 5, Mockito, and AssertJ.
  - Integration testing with `@SpringBootTest` and [Testcontainers](https://testcontainers.com/) for ephemeral PostgreSQL testing.
  - End-to-end (E2E) testing for Angular utilizing Cypress or Playwright.
- [ ] **Performance & Caching Layer**:
  - Distributed caching with Redis via Spring Cache for frequently generated exercise templates and static reference data.
  - Asynchronous background job processing for long-running workout generation tasks using Spring Batch or RabbitMQ.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
