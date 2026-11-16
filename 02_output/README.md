# Contact Groups

> For: agent (writes), human (reads) · Written in: phase 7 · Source: `project/02_design/architecture.md`, ES-06 · Agent: writes

## Overview

A web application for keeping shared contact lists. People sign in (e-mail and password, or Google), create **groups**, invite other people with a role (Owner, Editor, Viewer) and keep **contacts** in the group: 11 predefined fields, tags and free attributes. The app searches and filters (accent-insensitive, Slovenian collation), exports to Excel (`.xlsx`), keeps a Trash (30 days), a History of changes and a version list per contact with compare and restore, finds duplicates and merges them. The interface is in Slovenian (default) and English. Sign-in is done by Firebase Authentication; all contact data lives in the product's own PostgreSQL database.

The product name is still a neutral working name (OQ-P-08, `docs/release-notes.md`).

## Components

| Component | Folder | What it is |
|---|---|---|
| Backend | [`backend/`](backend/README.md) | REST API: Java 21, Spring Boot 3.5, Spring JDBC, Flyway, PostgreSQL 17.6 |
| Web app | [`frontend/`](frontend/README.md) | React 19, TypeScript, Vite, Tailwind, shadcn/ui; served by nginx in the container |
| Database | container `postgres:17.6` | Schema by Flyway migrations of the backend, data in the volume `contacts-db` |
| Authentication Emulator | `docker/emulator/` | Local and test sign-in only; not part of the delivered stack |
| Proxy | `docker/proxy/` | nginx with TLS in front of the web app and the API (delivered stack only) |

## Quick start (local, with the Firebase emulator)

Needs Docker with Compose v2 and Git. Run from `02_output/`.

1. Copy `01_input/01_project/00_setup/secrets.env.example` to `.env` in the repository root and fill `DB_PASSWORD` (any value for a local stack) and `FIREBASE_WEB_API_KEY` (`fake-api-key` works with the emulator). Never commit `.env`.
2. `docker compose --env-file ../.env up --build -d`
3. `./scripts/seed.sh` (creates four test users in the emulator, for example `ana.novak@example.com`, password `Geslo1234`, and their sample data).
4. Open `http://localhost:5173`. The API answers at `http://localhost:8080` (health: `/health`).
5. Stop: `docker compose --env-file ../.env down` (keeps the data; add `-v` to delete it).

Build, test and check commands of each component are in its README (ES-05). Summary:

| Component | build | test | check | run |
|---|---|---|---|---|
| backend | `./mvnw -B -ntp package -DskipTests` | `./mvnw -B -ntp verify` | `./mvnw -B -ntp spotless:check pmd:check pmd:cpd-check` | `./mvnw spring-boot:run -Dspring-boot.run.profiles=local` |
| frontend | `npm run build` | `npm test` | `npm run check` | `npm run dev` |

Browser tests: `npm run test:e2e` in `frontend/` with the stack from the quick start running (one worker; the tests reset the database and the emulator). CI (`.github/workflows/ci.yml`) runs every build, test, check and scan on each push.

## Deploy (the customer's installation)

The project does not deploy; the customer installs and runs the delivered set (`docker-compose.prod.yml`, environments `production`, Q-17a). Plan about an hour for the first install, plus the Firebase project.

**Before you start**
- A server with Docker and Compose v2, ports 80 and 443 free, and a DNS name.
- A **real Firebase project** with Authentication switched on (e-mail/password, and Google if wanted) and the DNS name among its authorised domains. From it: project id, web API key, auth domain.
- A TLS certificate and key for the DNS name.

**Configuration.** Copy `secrets.env.example` to `.env` (repository root) and set the five required values. The compose file refuses to start when one is missing.

| Variable | Meaning |
|---|---|
| `DB_PASSWORD` | Password of the database user (secret; choose a long random one) |
| `FIREBASE_PROJECT_ID` | Firebase project id (not `demo-contacts`) |
| `FIREBASE_WEB_API_KEY` | Web API key of the Firebase web app |
| `FIREBASE_AUTH_DOMAIN` | Firebase auth domain, normally `<project-id>.firebaseapp.com` |
| `APP_ALLOWED_ORIGIN` | Public address of the web app, for example `https://contacts.example.com` |
| optional | `TRASH_CLEANUP_CRON` (default `0 30 3 * * *`), `RATE_LIMIT_PER_MINUTE` (100), `LOG_LEVEL` (INFO) |

**Certificate.** Put `tls.crt` and `tls.key` into `02_output/certs/`. The proxy runs as the unprivileged user 101, so **both files must be readable by it**: `chmod 644 certs/tls.key` (with mode 0600 owned by root the proxy crashes with "cannot load certificate key: Permission denied"). Keep the key out of Git (`certs/` holds secrets; do not commit it). A customer with an own HTTPS front proxy deletes the `proxy` service and publishes `frontend` behind theirs.

**Start.** From `02_output/`:

```
docker compose -f docker-compose.prod.yml --env-file ../.env up --build -d --wait
```

All of db, backend, frontend and proxy become healthy; Flyway creates the schema on the first start. Check `https://<your name>/api/health` (answers 200) and sign in. The first person who registers can create a group and becomes its Owner.

**Update.** Pull the new sources, run the same `up --build -d --wait`. Migrations run at start-up and go forward only: take a backup first.

**Backup and restore (14-day retention).**
- Daily backup: a cron entry on the host, for example `15 2 * * * cd /opt/contacts/02_output && COMPOSE_FILE=docker-compose.prod.yml ./scripts/backup.sh /var/backups/contacts`. It writes `contacts-<UTC time>.dump` and deletes dumps older than 14 days (`RETENTION_DAYS`). Only the database needs a backup; the images are rebuilt from the sources and the accounts live in Firebase.
- A dump holds personal data: store it with the same care as the database, and note that a contact erased in the app stays in older dumps until they expire.
- Restore test (into a throw-away stack, compares every table): `COMPOSE_FILE=docker-compose.prod.yml ./scripts/restore.sh`.
- Real restore: stop the backend, `docker compose -f docker-compose.prod.yml exec -T db pg_restore --clean --if-exists -U contacts -d contacts < <dump file>`, start the backend.

**Operate.** Logs: `docker compose -f docker-compose.prod.yml logs -f backend` (no passwords, tokens or e-mail addresses are written). The nightly cleanup erases expired Trash (see "Data retention duties" in `docs/release-notes.md`).

## Verification scripts

`scripts/restart-test.sh` (data survives a restart, NFR-05), `scripts/restore.sh` (NFR-06), `scripts/clean-host-test.sh` (the delivered set builds from the committed files and runs, NFR-12), `scripts/perf/load-test.mjs` (NFR-01 to NFR-04), `scripts/licence-check.mjs`, `scripts/check-traceability.mjs`. Logs go to `logs/`.

## Documentation

| File | Content |
|---|---|
| `docs/release-notes.md` | What was delivered, known limitations, manual tests, decisions pending review, retention duties |
| `docs/02_specification.md` | Specification: modules, rules, screens, traceability to the inputs |
| `docs/02_adr/` | Architecture decisions (ADR-01 to ADR-12) |
| `docs/02_contracts/` | OpenAPI description, schemas, configuration schema |
| `docs/wireframes/` | Approved wireframes |
| `docs/03_test-strategy.md` | Test strategy, mutation rounds |
| `docs/04_review-INC-1.md`, `04_review-INC-2.md` | Reviews of the two increments |
| `docs/06_verification-report.md`, `06_hidden-evaluation.md` | Verification runs 1 to 5, hidden suite results |
| `docs/decisions-log.md` | Every decision and reading made during the run |
| `docs/07_retrospective.md` | What went well and what to improve |
| `logs/` | Raw output of builds, tests, scans and scripts |
