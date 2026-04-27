# ARGUS CORTEX KNOWLEDGE BASE

## OVERVIEW
`argus-cortex` is the Spring Boot backend. It owns cloud-side auth, social/messaging state, payment orchestration, and server-side integrations.

## STRUCTURE
```text
argus-cortex/
├── pom.xml
├── docker-compose.yml
├── docs/
├── src/main/java/com/kzzz3/argus/cortex/
├── src/main/resources/application.yaml
└── src/main/resources/db/migration/
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Application entry | `src/main/java/com/kzzz3/argus/cortex/ArgusCortexApplication.java` | Spring Boot startup root |
| Feature code | `src/main/java/com/kzzz3/argus/cortex/` | Current top-level domains: `auth`, `config`, `conversation`, `friend`, `media`, `payment`, `shared` |
| Runtime config | `src/main/resources/application.yaml` | Env-driven backend defaults |
| Schema changes | `src/main/resources/db/migration/` | Flyway migration files |
| Tests | `src/test/java/`, `src/test/resources/application.yaml` | H2 + in-memory test baseline |
| Project intent | `docs/project-plan.md`, `docs/retina-cortex-contract.md`, `HELP.md` | Domain ownership and constraints |

## COMMANDS
```bash
./mvnw test
docker compose up -d
./mvnw spring-boot:run
```

## CONVENTIONS
- Build with the Maven wrapper, not an ad hoc local Maven invocation.
- Configuration is environment-variable first. Preserve `${ARGUS_*}` driven settings instead of hardcoding environment-specific values.
- Default runtime persistence is MySQL: keep `argus.persistence.mode` defaulting to `${ARGUS_PERSISTENCE_MODE:mysql}`. Do not switch production/default runtime to in-memory storage.
- Start the local dependency stack, or otherwise provide MySQL/Redis equivalents, before using the default `spring-boot:run` path.
- Flyway migration location is `classpath:db/migration`.
- MyBatis Plus uses underscore-to-camel-case mapping; keep database/entity naming compatible with that behavior.
- Tests use `src/test/resources/application.yaml` with H2, in-memory persistence defaults, and an explicit test-only JWT secret.
- `HELP.md` defines `V1__create_text_im_schema.sql` as the canonical evolving baseline during active development.

## ANTI-PATTERNS
- Do not add Android/UI/device logic here.
- Do not add JNI bridge logic or native engine implementation here.
- Do not add heavy media cleanup or direct sensor/camera processing here.
- Do not generate secure signatures from raw local secrets here.
- Do not bypass Flyway by making undocumented schema changes outside `db/migration`.
- Do not treat build/runtime directories like `target/`, `.tmp/`, or `var/` as source areas.

## TESTING
- Primary test tree mirrors production packages under `src/test/java`.
- Common backend test style uses handwritten fakes/in-memory implementations. Mockito is acceptable for narrow interaction tests where a fake would obscure the contract being verified.
- Keep persistence-sensitive tests aligned with the H2 + Flyway test profile.

## NOTES
- `docker-compose.yml` defines the expected local dependency stack: MySQL, Redis, Kafka, Kafka UI, and MinIO.
- `ARGUS_JWT_SECRET` is required for application startup. Blank values, example placeholders, and the old local fallback are intentionally rejected; copy `.env.example` and replace the placeholder before running the app.
- `docs/project-plan.md` explicitly says Cortex is not the place for heavy media processing, direct sensor access, UI logic, or low-level secret-bearing crypto handling.
- The largest backend hotspots are currently `conversation`, `auth`, and `payment`; keep module boundaries clear there.
