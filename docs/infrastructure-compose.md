# Argus Cortex Infrastructure Compose

## Included services

- **MySQL** — primary relational store for identity, social graph, conversation metadata, and transaction records
- **Redis** — hot session/presence/routing state plus rate limits and locks
- **Kafka** — async fan-out, notifications, analytics, and future risk pipelines
- **Kafka UI** — operational visibility for Kafka topics/messages
- **MinIO** — S3-compatible object storage for voice/image/video assets and thumbnails

## Why these services belong in Docker now

- MySQL / Redis / Kafka are already named as core platform dependencies in `docs/project-plan.md`
- MinIO matches the documented object-storage need and lets the backend/media pipeline stay local-first in development and server-side staging
- Kafka UI is optional in production but extremely useful in development and debugging

## What is not included yet

- `argus-cortex` application containerization
- `argus-cortex` application runtime wiring to these services
- Netty IM gateway sidecar or separate gateway container
- model serving / AI provider containers
- payment/risk external adapters

Those belong to later phases once the current Spring Boot module actually starts consuming the infra.

## Usage

1. Copy env file:

   - `copy .env.example .env` on Windows
   - `cp .env.example .env` on Unix-like shells

2. Adjust passwords/secrets in `.env`

3. Start the stack:

   - `docker compose up -d`

4. Inspect services:

   - MySQL: `localhost:3306`
   - Redis: `localhost:6379`
   - Kafka: `localhost:9092`
   - Kafka UI: `http://localhost:8081`
   - MinIO API: `http://localhost:9000`
   - MinIO Console: `http://localhost:9001`

## Current deployment scope

This compose is intentionally **infra-only**.

You start the supporting services in Docker, but keep `argus-cortex` itself running directly from your normal Java/Spring development or server process.

## Recommended next infrastructure step

Once `argus-cortex` starts using these dependencies for real, add:

- Spring profiles / `application-local.yaml`
- Docker health-aware app startup dependency wiring
- database schema migration tooling (Flyway or Liquibase)
- topic/bootstrap scripts for Kafka
