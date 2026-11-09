# Environments

> For: agent (reads), human (reviews) · Owner: Architect · Agent: read-only

## Environments

Three environments, no staging (Q-17b).

| Environment | Purpose | How it runs |
|---|---|---|
| test | Automated tests and CI | Containers started by the tests: a throwaway PostgreSQL (Testcontainers) and the Firebase Authentication Emulator with test users with fixed e-mail addresses, passwords and verified e-mails. No internet, no real accounts, no secrets. Plain HTTP on localhost. |
| local | Development and manual checks; the development environment of Q-17b (local Docker, or the team's internal server) | `docker compose` with backend, frontend, PostgreSQL and either the Firebase emulator or one real development Firebase project whose settings are in the developer's `.env` (Q-14a). HTTP on localhost. |
| production | The customer's installation, run and operated by the customer (cloud or own infrastructure); the project does not deploy there (Q-17a) | The delivered container images and the Compose setup without the emulator, behind the customer's HTTPS front proxy (nginx in the delivered setup or their own). Settings from environment variables. |

PostgreSQL data lives in a named Docker volume (or the customer's managed database) and survives container recreation and restarts; tests use throwaway databases and keep nothing.

## Configuration

Settings that differ between environments (ES-01). Secrets are listed in `project/00_setup/secrets.env.example`, not here.

| Setting | Environments | Default allowed? |
|---|---|---|
| Database address, name, user (`DB_URL`, `DB_USER`) | test, local, production | Yes for local and test (local container); no for production |
| Firebase project ID (`FIREBASE_PROJECT_ID`) | test, local, production | Yes for test and local (`demo-contacts`, the emulator's demo project); no for production |
| Firebase web app settings (`FIREBASE_WEB_API_KEY`, `FIREBASE_AUTH_DOMAIN`) | local, production | No (not needed in test with the emulator, which accepts a fake key) |
| Firebase emulator address (`FIREBASE_AUTH_EMULATOR_HOST`) | test, local | Yes; must be empty in production |
| Allowed origin of the web app (`APP_ALLOWED_ORIGIN`) | test, local, production | Yes for local and test (`http://localhost:5173`); no for production |
| Trash cleanup time (`TRASH_CLEANUP_CRON`) | test, local, production | Yes (once a day) |
| Rate limit per minute (`RATE_LIMIT_PER_MINUTE`) | test, local, production | Yes (100) |
| Log level (`LOG_LEVEL`) | test, local, production | Yes (INFO) |

## External services

| Service | Production | Local/test substitute | Must be tested manually by a human? |
|---|---|---|---|
| Firebase Authentication (e-mail/password, Google) | The customer's real Firebase project (not yet decided, Q-14b) | Firebase Authentication Emulator with fixed test users | Yes, once before delivery, against one real development Firebase project (Q-14a); "Sign in with Google" needs a real project |
| Google public keys for token verification | Google's published keys, reached by the backend | The emulator's unsigned test tokens, accepted only when the emulator host is set | No |
| Microsoft Excel (opens the export) | not a service | A library reads the file back in tests | Yes, one opening in Microsoft Excel at the demo (DoD-P04) |
| E-mail | None (the product sends none); only Firebase's own e-mails | Emulator prints links | No |
