# Argus Cortex

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-6db33f)
![Java](https://img.shields.io/badge/Java-26-orange)
![Maven](https://img.shields.io/badge/build-Maven-c71a36)

Argus Cortex is the cloud hub for Argus. It owns identity, sessions, social graph, conversation state, media upload orchestration, payment workflows, server-side policy, and integrations.

## Architecture at a Glance

Cortex is a Spring Boot backend with domain-first packages and environment-driven runtime configuration.

```text
argus-cortex/
├── src/main/java/com/kzzz3/argus/cortex/
│   ├── auth/
│   ├── conversation/
│   ├── friend/
│   ├── media/
│   ├── payment/
│   └── shared/
├── src/main/resources/application.yaml
├── src/main/resources/db/migration/
├── docker-compose.yml
└── docs/
```

## Getting Started

### Requirements

- JDK 26-compatible toolchain for this project configuration.
- Maven wrapper from this repository.
- Docker Compose for local MySQL, Redis, Kafka, Kafka UI, and MinIO.

### Configure Local Infrastructure

Docker Compose loads `.env` for containers. The Spring Boot process still needs its own process environment when you run it outside Docker.

```bash
copy .env.example .env
```

On Unix-like shells:

```bash
cp .env.example .env
```

Replace `ARGUS_JWT_SECRET` with a private value of at least 32 characters. Placeholder/example secrets are intentionally rejected at startup.

Start dependencies:

```bash
docker compose up -d
```

Local service endpoints:

- MySQL: `localhost:3306`
- Redis: `localhost:6379`
- Kafka: `localhost:9092`
- Kafka UI: `http://localhost:8081`
- MinIO API: `http://localhost:9000`
- MinIO Console: `http://localhost:9001`

### Test and Run

Before running the app directly, export the application environment so it matches the local Compose services.

PowerShell:

```powershell
$env:ARGUS_MYSQL_URL = "jdbc:mysql://localhost:3306/argus_cortex?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
$env:ARGUS_MYSQL_USERNAME = "argus"
$env:ARGUS_MYSQL_PASSWORD = "argus_change_me"
$env:ARGUS_JWT_SECRET = "replace-with-a-private-value-at-least-32-characters"
```

Unix-like shells:

```bash
export ARGUS_MYSQL_URL='jdbc:mysql://localhost:3306/argus_cortex?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'
export ARGUS_MYSQL_USERNAME='argus'
export ARGUS_MYSQL_PASSWORD='argus_change_me'
export ARGUS_JWT_SECRET='replace-with-a-private-value-at-least-32-characters'
```

```bash
./mvnw test
./mvnw spring-boot:run
```

## Project Navigation

| Need | File / directory |
|---|---|
| Active work plan | `PLAN.md` |
| Local agent rules | `AGENTS.md` |
| Product/architecture plan | `docs/project-plan.md` |
| Local infra guide | `docs/infrastructure-compose.md` |
| Retina/Cortex contract | `docs/retina-cortex-contract.md` |
| Runtime configuration | `src/main/resources/application.yaml` |
| Database baseline | `src/main/resources/db/migration/V1__create_text_im_schema.sql` |
| Tests | `src/test/java/`, `src/test/resources/application.yaml` |

## Technology Stack

- **Spring Boot 4** for convention-driven application structure.
- **Spring Security + OAuth2 Resource Server** for bearer-token boundaries.
- **Flyway** for schema baseline management.
- **MyBatis Plus** for database mapping with underscore-to-camel-case conventions.
- **MySQL, Redis, Kafka, MinIO** for local platform dependencies.
- **Actuator** for health/info operations baseline.

## Contribution Rules

- Read `PLAN.md` before multi-step backend work and update it when scope or verification changes.
- Keep Android UI/device logic and native engine implementation out of Cortex.
- Keep production/default persistence on MySQL unless an explicit plan changes that architecture.
- Use Flyway for documented schema changes.
- Update docs in the same change when APIs, runtime config, infra, or verification commands change.
- Never commit `target/`, `.tmp/`, `var/`, local `.env`, database dumps, or generated runtime artifacts.
