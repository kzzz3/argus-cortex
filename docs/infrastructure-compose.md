# Argus Cortex Infrastructure Compose

## Included services

- **MySQL** — primary relational store for identity, social graph, conversation metadata, and transaction records
- **Redis** — hot upload-session state plus future presence/routing/rate-limit coordination
- **Kafka** — async fan-out, notifications, analytics, and future risk pipelines
- **Kafka UI** — operational visibility for Kafka topics/messages
- **MinIO** — S3-compatible object storage for voice/image/video assets and thumbnails

## Why these services belong in Docker now

- MySQL / Redis / Kafka are already named as core platform dependencies in `docs/project-plan.md`
- MinIO matches the documented object-storage need and lets the backend/media pipeline stay local-first in development and server-side staging
- Kafka UI is optional in production but extremely useful in development and debugging

## What is not included yet

- `argus-cortex` application containerization
- Netty IM gateway sidecar or separate gateway container
- model serving / AI provider containers
- payment/risk external adapters

The Spring Boot app already uses MySQL, Redis, Flyway, and local media storage through normal application configuration. This compose file remains infra-only: start these services in Docker, then run the app from the usual Java/Spring process.

## Usage

1. Copy env file:

   - `copy .env.example .env` on Windows
   - `cp .env.example .env` on Unix-like shells

2. Adjust passwords/secrets in `.env`

   Required Compose values include:

   - `MYSQL_ROOT_PASSWORD`
   - `MYSQL_DATABASE`
   - `MYSQL_USER`
   - `MYSQL_PASSWORD`
   - `ARGUS_JWT_SECRET` (keep this aligned with the value exported to the Spring Boot process; application startup intentionally fails without a real private value)

3. Start the stack:

   - `docker compose up -d`

4. Inspect services:

   - MySQL: `localhost:3306`
   - Redis: `localhost:6379`
   - Kafka: `localhost:9092`
   - Kafka UI: `http://localhost:8081`
   - MinIO API: `http://localhost:9000`
   - MinIO Console: `http://localhost:9001`

5. If you change the canonical Flyway baseline in active development (for example by editing `V1__create_text_im_schema.sql`), clear the local MySQL database before restarting the app so Flyway can rebuild from the current baseline.

   Example dev-only reset flow:

   - stop the app
   - drop and recreate the local `argus_cortex` schema, or remove the local MySQL volume
   - restart `argus-cortex` so Flyway reapplies the current baseline

## Current deployment scope

This compose is intentionally **infra-only**.

You start the supporting services in Docker, but keep `argus-cortex` itself running directly from your normal Java/Spring development or server process. Kafka UI is exposed at `http://localhost:8081` so the backend can continue to use its default `8080` port.

Docker Compose loads `.env` for containers only. Before `./mvnw spring-boot:run`, export matching app-process values, for example:

```powershell
$env:ARGUS_MYSQL_URL = "jdbc:mysql://localhost:3306/argus_cortex?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
$env:ARGUS_MYSQL_USERNAME = "argus"
$env:ARGUS_MYSQL_PASSWORD = "argus_change_me"
$env:ARGUS_JWT_SECRET = "replace-with-a-private-value-at-least-32-characters"
```

## Recommended next infrastructure step

Next, add the missing operational polish around this infra:

- Spring profiles / `application-local.yaml`
- Docker health-aware app startup dependency wiring
- topic/bootstrap scripts for Kafka
