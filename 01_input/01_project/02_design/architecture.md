# Architecture

> For: agent (reads), human (reviews) · Owner: Architect · Agent: read-only

## Components

Each component gets its own folder and README in `02_output/`. Versions come only from `tech-stack.md`; reference its ids, never repeat a version here.

```yaml
components:
  - name: backend
    type: api
    folder: 02_output/backend
    platform: java
    build: maven
    bootstrap: Spring Boot project generated with the Spring Initializr dependency set listed in tech-stack.md (or a hand-written pom.xml with exactly those entries); root package app.contactgroups. [ASSUMPTION] package name
    commands:          # ES-05; filled in phase 2 if left empty
      build:
      test:
      check:
      run:
    depends_on: []
    deploys_as: container image (eclipse-temurin base)
  - name: frontend
    type: web-app
    folder: 02_output/frontend
    platform: node
    build: vite
    bootstrap: Vite React TypeScript project with Tailwind CSS and shadcn/ui components (UI-01), source in src/, language files in src/locales/ (sl, en)
    commands:          # ES-05; filled in phase 2 if left empty
      build:
      test:
      check:
      run:
    depends_on: [backend]
    deploys_as: container image (nginx serving the built files; runtime configuration from environment variables at container start)
```

The local stack (backend, frontend, PostgreSQL, Firebase Authentication Emulator) is started by `02_output/docker-compose.yml`; the delivered Compose setup has no emulator and uses the customer's real Firebase project settings from `.env`. The install and backup guide lives in `02_output/docs/` (phase 6).

## Constraints

Checkable rules the code must respect (layering, allowed dependencies, patterns). Checked automatically in phase 6 where the stack allows (DoD-04).

| ID | Constraint |
|---|---|
| AR-01 | The backend has the layers `api` (controllers, DTOs), `service` (business rules), `repository` (Spring JDBC) and `domain`; `api` does not call `repository` directly, and `repository` does not call `api` or `service`. Check: architecture test (ArchUnit-style rule in the test suite). |
| AR-02 | The database layer uses Spring JDBC (JdbcTemplate or NamedParameterJdbcTemplate) with parameterised queries only; no Hibernate and no JPA; no SQL built by string concatenation with user input. Check: dependency list, static analysis, SR-07. |
| AR-03 | Every repository method that reads or writes group data takes the group id and the user id or role context as a parameter and filters by it in the query (SR-02). Check: architecture test and AC-017-07. |
| AR-04 | The group id is read only from the request path; no controller reads a group id from a body or query. Check: architecture test and AC-017-05. |
| AR-05 | The API is stateless: no server session; identity comes from the Firebase ID token on every request. |
| AR-06 | Database changes only through Flyway migrations (forward only); the application runs them at start-up. |
| AR-07 | The frontend talks to the backend only through the REST API described in the OpenAPI description; it uses Firebase only for Authentication (sign-in, session, ID token, account deletion). No other Firebase or Google service (Q-16d). |
| AR-08 | Frontend: components contain logic and structure; styles live in token and style files (UI-03); every text comes from the language files (NFR-10); no raw colour value in a component (UI-04). Check: lint rules and a test. |
| AR-09 | History entries are written in the same database transaction as the change they record; a failed change writes no entry. Check: tests with forced failures (EX-013-08). |
| AR-10 | A scheduled cleanup in the backend erases contacts and groups whose 30 days in Trash are over (BR-04) and runs at least once a day. It is configured by environment variables and can also be started by a test with a controllable clock. |
| AR-11 | Time is read through one injectable clock; the database stores UTC; the project time zone is applied only for display and for day boundaries (business-rules.md, Project-wide facts). |
| AR-12 | All configuration comes from environment variables (NFR-12); the API refuses to start when a required variable is missing. |
| AR-13 | The OpenAPI description is generated from the code and committed or served; the web app's API client types are generated from it, so UI and API cannot drift. |
| AR-14 | Optimistic concurrency: every save of a contact carries the contact's current version number, and the repository rejects it when the stored number differs (US-005 R-005-08). |
| AR-15 | Searching and sorting use the database with a Slovenian collation and accent-insensitive matching; text comparison rules follow BR-07. Check: tests with Škerlj, Čuk, Šuster. |

## Interfaces

Every interface between components or with external systems is defined as a contract in `docs/02_contracts/` in phase 2.

| Interface | Between | Style (e.g. REST, gRPC, events, file, UI) |
|---|---|---|
| Web app to API | frontend → backend | REST, JSON, OpenAPI; `Authorization: Bearer` Firebase ID token |
| Sign-in | frontend → Firebase Authentication | Firebase client SDK (e-mail/password, Google) |
| Token verification | backend → Firebase's public keys | JWT verification against Google's published keys (no service account) |
| Database | backend → PostgreSQL | JDBC (Spring JDBC), Flyway migrations |
| Excel export | backend → user's browser | File download (`.xlsx`, `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`) |
| Health check | operator or container platform → backend | HTTP, no authentication |
| User interface | user → frontend | Browser UI (Slovenian and English) |
