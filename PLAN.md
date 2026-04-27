# Argus Cortex Active Plan

## Project Goal

Argus Cortex provides the reliable cloud control plane for Argus identity, messaging, media, payment, policy, and future AI-wearable orchestration.

## Core Feature Breakdown

- [x] Spring Boot baseline with auth, validation, shared API errors, and module-first package layout.
- [x] Token-backed auth/session restoration with fail-fast JWT secret validation and TTL defaults.
- [x] Conversation/message endpoints with recent-window and cursor continuation semantics.
- [x] Idempotent message send and structured attachment metadata baseline.
- [x] Media upload session creation, streaming upload handling, server-owned caps, and filename sanitization.
- [x] Auth rate-limit baseline and Actuator health/info operations support.
- [ ] Durable structured persistence for conversation metadata, messages, receipts, and read-state transitions.
- [ ] Server-driven delivery receipt and read-state sync.
- [ ] Friend-request workflow and direct-chat ownership rules.
- [ ] WebRTC signaling APIs for 1v1 audio/video sessions.
- [ ] Durable media retention and cleanup policy.

## Technology Stack and Architecture Decisions

- **Java 26 + Spring Boot 4**: convention-driven backend foundation.
- **Spring Security**: centralized bearer-token parsing and authenticated account resolution.
- **Flyway**: database changes are explicit and reproducible.
- **MyBatis Plus**: structured relational persistence with known naming conventions.
- **MySQL default runtime**: production-like durable persistence by default.
- **Redis/Kafka/MinIO**: hot state, async fan-out, and object storage boundaries.

## Project Structure Plan

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
├── src/test/
└── docs/
```

## Development Phases and Milestones

### Phase 1 — MVCV: Backend Core
- [x] App starts with Spring Boot and test profile support.
- [x] Auth/session endpoints exist.
- [x] Conversation/message skeleton APIs exist.
- [x] Test suite verifies the baseline.

### Phase 2 — Stage 1 Messaging and Media Baseline
- [x] Cursor-aware message list semantics.
- [x] Idempotent message send.
- [x] Structured attachment references.
- [x] Upload session and media object flow.
- [ ] Durable message/read-state/friend persistence.

### Phase 3 — Stage 1 Reliability and Realtime
- [x] SSE event ids, heartbeat frames, and Last-Event-ID resume support.
- [x] JWT fail-fast and auth rate-limit baseline.
- [ ] RTC signaling schema and session lifecycle events.
- [ ] Offline-window and retention policy.

### Phase 4 — Stage 2 AI-Wearable Orchestration
- [ ] Multimodal ingest protocol.
- [ ] Typed action schema and policy gates.
- [ ] Retina trusted-artifact ingestion and audit model.

## Mandatory M-R-E-A Cycle

1. **Modify**: implement the smallest backend-scope change.
2. **Review/Evaluate**: check API contract, persistence, security, error handling, and module placement.
3. **Document**: update this plan plus README/docs when endpoints, config, schemas, or commands change.
4. **Cleanup**: remove dead code/artifacts and run the relevant Maven checks.

## Verification Gates

| Change type | Required verification |
|---|---|
| Routine Java/backend code | `./mvnw test` |
| Schema/persistence changes | `./mvnw test` with H2/Flyway test profile; local DB reset if the active v1 baseline changes |
| Runtime config/JWT/infra behavior | `./mvnw test`; manual startup after `.env` changes when runtime behavior is affected |
| Local infrastructure docs | Check `docker-compose.yml`, `.env.example`, and `docs/infrastructure-compose.md` together |
| Docs-only | Read changed docs and verify commands against project files |

## Risk Register

- Placeholder or missing `ARGUS_JWT_SECRET` intentionally prevents app startup.
- Active-development Flyway v1 baseline changes require local database reset.
- In-memory test/local fakes must not become production/default persistence.
- Contract drift with Lens DTOs or Retina trusted artifacts.
- Local Docker infra can mask app-level failures if the app is not tested separately.

## Documentation Rules

- `README.md` explains how to onboard, configure, test, and run Cortex.
- `PLAN.md` tracks active backend work and verification gates.
- `docs/project-plan.md` remains the long-form product/architecture blueprint.
- `docs/infrastructure-compose.md` remains the local infra reference.
- `HELP.md` is narrow generated/help context; do not duplicate onboarding there.

## Verification Log

| Date | Scope | Command / method | Result |
|---|---|---|---|
| 2026-04-27 | Documentation workflow refresh | Read `AGENTS.md`, `docs/project-plan.md`, `docs/infrastructure-compose.md`, `.env.example`, `pom.xml`, and verified referenced paths exist | PASS for this documentation pass |
