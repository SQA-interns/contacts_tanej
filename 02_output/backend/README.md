# Backend (REST API)

Java 21 (Temurin), Spring Boot 3.5.16, Spring JDBC, Flyway, PostgreSQL 17.6. Package root `app.contactgroups`. Design: `../docs/02_specification.md`, `../docs/02_adr/`, contract `../docs/02_contracts/openapi.yaml`.

## Prerequisites

Docker (the tests start PostgreSQL with Testcontainers) and a JDK 21 for `./mvnw`. Where no JDK or Maven is installed, the same commands run in the pinned image: `docker run --rm -v "<this folder>://w" -v m2cache://root/.m2 -v //var/run/docker.sock://var/run/docker.sock -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal -w //w maven:3.9.11-eclipse-temurin-21 mvn <goals>` (this is how the skeleton was verified).

## Commands (ES-05)

| Task | Command |
|---|---|
| build | `./mvnw -B -ntp package -DskipTests` |
| test (unit, architecture, integration) | `./mvnw -B -ntp verify` |
| check (format, lint, duplication) | `./mvnw -B -ntp spotless:check pmd:check pmd:cpd-check` |
| format | `./mvnw spotless:apply` |
| run (local profile) | `./mvnw spring-boot:run -Dspring-boot.run.profiles=local` with the variables below |
| coverage report | `target/site/jacoco/index.html` after `verify` |
| mutation (phase 5) | `./mvnw org.pitest:pitest-maven:mutationCoverage` |
| dependency scan | `./mvnw org.owasp:dependency-check-maven:12.1.6:check` |

## Configuration

All settings are environment variables (`../docs/02_contracts/configuration.schema.json`). Required, no default: `DB_URL`, `DB_USER`, `DB_PASSWORD` (secret, `.env`), `FIREBASE_PROJECT_ID`, `APP_ALLOWED_ORIGIN`. Optional: `FIREBASE_AUTH_EMULATOR_HOST` (empty in production), `TRASH_CLEANUP_CRON`, `RATE_LIMIT_PER_MINUTE`, `LOG_LEVEL`. The profile `local` supplies defaults for a developer machine. A missing required variable stops the start and names it.

## Run the whole stack

From `02_output/`: `docker compose --env-file ../.env up --build -d`, then `./scripts/seed.sh`. The API is at `http://localhost:8080` (health: `/health`), the web app at `http://localhost:5173`.

## Troubleshooting

- `Missing required configuration: ...`: set the named variables.
- Integration tests fail to start a container: Docker is not running or its socket is not reachable from the test process (in the Maven image mount `/var/run/docker.sock` and set `TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal`).
- Flyway cannot create `unaccent` or `pg_trgm`: the database user needs the right to create extensions (the `postgres:17.6` image's user has it).
