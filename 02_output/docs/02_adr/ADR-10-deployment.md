# ADR-10: Container images, Compose set-ups and TLS

- Status: accepted · Phase 2 · Drivers: NFR-05, NFR-06, NFR-12, SB-04, SB-11, SR-04, SR-06, SR-12, DoD-P08, ES-09, environments.md

## Context

The product is delivered as container images and a Docker Compose set-up the customer runs (Q-17a). The local stack adds the Firebase Authentication Emulator; the delivered one does not and uses the customer's Firebase project from `.env`. Plain HTTP is allowed only on localhost; the API must accept HTTPS only elsewhere and set HSTS (SR-04). Containers should run with least privilege (SB-11).

## Options

TLS: (a) in the backend; (b) in a front proxy container (nginx) in front of web app and API; (c) leave it entirely to the customer. Images: (d) one image with both parts; (e) one image per component as architecture.md says.

## Decision

(b) and (e).

- **`backend` image** (`02_output/backend/Dockerfile`): build stage `maven:3.9.11-eclipse-temurin-21` (dev-only builder, D-10), runtime `eclipse-temurin:21.0.8_9-jre`, a non-root user, `EXPOSE 8080`, `HEALTHCHECK` calling `/health` (ES-09). All settings by environment variable (AR-12).
- **`frontend` image** (`02_output/frontend/Dockerfile`): build stage `node:24.13.0-slim` (D-01, D-24), runtime `nginx:1.29.1`. nginx runs as user 101, listens on 8080, serves the built files with the headers of SR-06, proxies `/api/` to `http://backend:8080/` (prefix stripped), and writes `/config.js` from the environment at start (`docker/40-config.sh`, ADR-07).
- **`docker-compose.yml`** (local): `db` (`postgres:17.6`, named volume `contacts-db`), `emulator` (a small image with `firebase-tools` for the Authentication Emulator only), `backend` (profile `local`), `frontend` published on `localhost:5173`, health checks and `depends_on: condition: service_healthy`. Values come from `.env` (`docker compose --env-file ../.env up`); no secret is in the file.
- **`docker-compose.prod.yml`**: the same without `emulator` and without the `local` profile, plus `proxy` (nginx 1.29.1) that terminates TLS with the certificate and key mounted from `./certs/`, redirects HTTP to HTTPS and sets HSTS; `db`, `backend`, `frontend` are on an internal network only; only `proxy` publishes ports 80 and 443. A customer with their own front proxy deletes `proxy` and publishes `frontend` behind theirs; the install guide says what that proxy must do (HTTPS, HSTS, forward `/`).
- **Database data** lives in the named volume `contacts-db`; the backup is `pg_dump` run daily by the operator (a documented command and a script `scripts/backup.sh`), kept 14 days; `scripts/restore.sh` restores into a clean stack and is tested once (NFR-06).
- **Secrets.** `DB_PASSWORD` and `FIREBASE_WEB_API_KEY` only in `.env` (SR-12); `.env.example` lists keys without values. Images contain none; `gitleaks` scans the repository and the build contexts (CI).
- **Seed data** for local use: `scripts/seed.sh` loads `scripts/seed/seed.sql` (no real person) and creates the matching emulator users with fixed passwords, in one command.

## Consequences

- Delivery builds with `docker compose build` and starts with one command from a clean host that has only the `.env` (NFR-12).
- TLS certificates are the customer's job; the guide gives the steps. The test environment uses HTTP on localhost, which SR-04 allows.
- The builder images (`node:24.13.0-slim`, `maven:3.9.11-eclipse-temurin-21`) are added to the container image list by decision D-24; they are not shipped in the runtime layers.
