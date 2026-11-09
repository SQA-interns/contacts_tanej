# Test strategy (as executed)

> For: agent (writes), human (reads) · Written in: phase 3 (`/acceptance-tests`), phase 5 (`/quality-gate`) · Source: `general/testing/writing-tests.md`, `general/testing/measurement.md`, `project/02_design/quality-requirements.md` · Agent: writes

## Test levels and harness

| Level | Component | Where | Harness | Through |
|---|---|---|---|---|
| Acceptance (API) | backend | `backend/src/test/java/app/contactgroups/acceptance/*IT.java` (failsafe, `mvn verify`) | `AcceptanceBase`: the whole backend runs in the test process on a throwaway PostgreSQL 17.6 (Testcontainers); people sign in with the unsigned tokens of the Firebase Authentication Emulator (the backend accepts them while `app.firebase-auth-emulator-host` is set, `id-token-claims.schema.json`); the injectable `Clock` of ADR-08 is replaced by a clock the tests move; the trash cleanup runs every second; every table is emptied before each test | HTTP only (`java.net.http`); the OpenAPI contract is read from `backend/src/test/resources/acceptance/openapi.yaml`, a byte-identical copy of `docs/02_contracts/openapi.yaml`; the database is read in a few tests only to prove an erasure (SR-15, BR-06) or to set two creation times equal (EX-022-03), by the tables named in the specification (section 3.2) |
| Acceptance (browser) | frontend | `frontend/acceptance/*.spec.ts` (Playwright) | `acceptance/support.ts`: the local stack of `environments.md` (`docker compose up`: web app, backend, PostgreSQL, Authentication Emulator); before each test the emulator accounts and every table are emptied; the scene is set through the API; elements are found by role, label or text of the wireframes, in English (`localStorage language=en`), never by CSS | the browser; the API and the emulator only to set the scene and to read results back |
| End-to-end | frontend | `frontend/e2e/e2e-0n-*.spec.ts`, exactly one test per scenario E2E-01 to E2E-08 | as the browser acceptance tests | the browser, the real stack |
| Hidden | backend, frontend | `hidden-tests/` (moved out of the project by the runner), see "Hidden suite" | the Java cases reuse `AcceptanceBase`; the browser cases reuse `acceptance/support.ts` | as above |

The skeleton check `frontend/checks/shell.spec.ts` (three widths, accessibility, screenshots) was moved from `e2e/` to `checks/`, so that `e2e/` holds only the scenarios of `e2e-scenarios.md`; `playwright.config.ts` runs the three folders and `tsconfig.json` includes them.

Run commands (from `02_output/`):

- backend acceptance: `docker run --rm -v "<backend folder>:/w" -v m2cache:/root/.m2 -v /var/run/docker.sock:/var/run/docker.sock -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal -w /w maven:3.9.11-eclipse-temurin-21 mvn -B -ntp verify -Dtest=NoSuchTest -Dsurefire.failIfNoSpecifiedTests=false "-Dit.test=app.contactgroups.acceptance.*IT"` (backend `README.md`; the `LargeGroupAcceptanceIT` builds a group of 10,001 contacts and takes about 1.5 minutes).
- browser acceptance and end-to-end: `docker compose --env-file ../.env up --build -d`, then in `frontend/`: `PLAYWRIGHT_BASE_URL=http://localhost:5173 npx playwright test --project=chromium acceptance e2e` (all three browsers: without `--project`).

Where a story states a time (14 days, 30 days, 5 minutes, a day boundary) the API tests move the clock; the browser tests do not wait for time. Two rules need a stand-in in the browser: a time shown in the History (AC-018-04, EX-018-03) is set by answering the History request with a fixed time (`page.route`), and an export with more than 10,000 contacts (AC-011-09) is shown with a stubbed refusal; the real limit is tested through the API (`LargeGroupAcceptanceIT`).

Choices where a story leaves the form of an answer open (recorded so that a reviewer can see they are readings, not invented requirements):

- The field name of an error is checked only where the specification fixes it (`name`, `email`, `phone`, `country`, `notes`, `firstName` for `REQUIRED_ONE_OF`); for tags and attributes the tests check that the field starts with `tags` or `attributes`, for `TOO_MANY_CONTACTS` and `INVALID_DATE_RANGE` only the code.
- A group in Trash answers 404 to its Owner for everything except restore and the list of deleted groups (decision D-17, specification 4.15), also for the "refuse" of AC-015-07.
- Two Owners deleting one group at once: one 204 and the other 404 or 409 `GROUP_IN_TRASH`, depending on whether the second request meets the lock or the committed state (EX-015-05).

## Measurement exclusions

Decided before any test was written. Everything else under `backend/src/main` and `frontend/src` is production code and counts.

| Path | Reason |
|---|---|
| backend `app/contactgroups/ContactGroupsApplication.java` | application entry point (`main`), no logic |
| backend `**/package-info.java` | package documentation and the layer markers of `ArchitectureTest`, no code |
| frontend `src/main.tsx` | application entry point, no logic (already excluded in `vite.config.ts`) |
| frontend `src/lib/api/schema.d.ts` | generated from `docs/02_contracts/openapi.yaml` by `openapi-typescript`, types only |
| frontend `src/styles/*.css` (`index.css`, `tokens.wireframe.css`, `tokens.final.css`) | style and design-token files; presentation, no behaviour (measurement constraints) |
| frontend `src/locales/*.json` | language files; texts only, their completeness is checked by `locales.test.ts` |
| backend `src/main/resources/**` (YAML, SQL migrations) | configuration and schema, not code; the migration is exercised by every integration test |

Test code never counts. No other exclusion is made now; `quality-requirements.md` lists no exclusion for mutation either.

## Hidden suite

<!-- Only the run command and the number of hidden tests per story; never the cases. -->

| Story | Hidden tests |
|---|---|
| US-001 | 9 |
| US-002 | 9 |
| US-003 | 12 |
| US-004 | 9 |
| US-005 | 10 |
| US-006 | 10 |
| US-007 | 8 |
| US-008 | 8 |
| US-009 | 14 |
| US-010 | 13 |
| US-011 | 15 |
| US-012 | 9 |
| US-013 | 14 |
| US-014 | 10 |
| US-015 | 10 |
| US-016 | 7 |
| US-017 | 16 |
| US-018 | 8 |
| US-019 | 8 |
| US-020 | 11 |
| US-021 | 14 |
| US-022 | 18 |
| Shared rules BR-01 to BR-07 | 9 |

Run command (from the project root; the file `hidden-tests/hidden.json` holds it): `node 02_output/hidden-tests/run-hidden.mjs`. It copies the Java cases into the backend's test folder of the copy it runs in, runs them with Maven in the pinned image, removes them again, runs the browser cases against the local stack (it starts and stops the stack when none answers at `PLAYWRIGHT_BASE_URL`) and writes `02_output/hidden-tests/report/junit.xml`.

## First run of the frozen tests (before any implementation)

Run on the skeleton (components and contracts of phase 2, no story built). Logs: `out/logs/s4-backend-frozen-run.log` (backend, 326 tests), `out/logs/s4-browser-first-run.md` (browser, 104 tests). A test counts as failed for a behavioural reason when it stops at an assertion or at a missing element or endpoint of the story, never at a build or harness error; the harness itself was proved on the skeleton first (a valid emulator token is accepted: the unmapped endpoints answer 500 `INTERNAL_ERROR`, not 401; `/health`, the 401 for a missing token and the security headers of `SkeletonIT` pass).

| Story or group | Tests | Failed | Reason (one line per group) |
|---|---|---|---|
| US-002 groups | 15 | 15 | `POST /groups` does not exist (500 `INTERNAL_ERROR` on an unmapped path) |
| US-003 invitations | 18 | 18 | no group endpoints, so the scene cannot be set; invitations do not exist |
| US-004 members | 13 | 13 | as US-003 |
| US-005 contacts | 22 | 22 | as US-003 (group and contact endpoints missing) |
| US-006 tags | 16 | 16 | as US-003 |
| US-007 tag administration | 13 | 13 | as US-003 |
| US-008 attributes | 13 | 13 | as US-003 |
| US-009 list and search | 19 | 19 | as US-003 |
| US-010 filters | 17 | 17 | as US-003 |
| US-011 export | 17 | 17 | as US-003 |
| US-012 Trash | 16 | 16 | as US-003 |
| US-013 History | 20 | 20 | as US-003 |
| US-014 group-wide History | 13 | 13 | as US-003 |
| US-015 group Trash | 14 | 14 | as US-003 |
| US-016 account | 10 | 10 | `DELETE /me` and the group endpoints do not exist |
| US-017 secured API | 21 | 21 | group and contract endpoints missing; `/v3/api-docs` lacks `/health`; the health check does not answer 503 with the database paused (the request times out) |
| US-017 rate limit | 2 | 2 | no rate limiter: no 429 and no `Retry-After` |
| US-019 versions | 12 | 12 | as US-003 |
| US-020 version restore | 14 | 14 | as US-003 |
| US-021 duplicates | 21 | 21 | as US-003 |
| US-022 merge | 19 | 19 | as US-003 |
| Large group (EX-009-08, EX-010-07, AC-011-09, EX-011-01) | 4 | 4 | the shared set-up (a group of 10,001 contacts) fails at `POST /groups`; the runner reports it as one error for the class |
| Backend acceptance, total | 326 (+3 not run singly) | 326 (+3) | |
| Browser: US-001 sign in and register | 15 | 15 | no sign-in, register or verify screens: the shell shows only "Dobrodošli" (14 stop at the missing form fields or buttons, 1 at the Google popup) |
| Browser: US-018 language | 10 | 10 | AC-018-01 and EX-018-01 passed on the skeleton because the shell already opens in Slovenian; the test was strengthened (the first visit must be the sign-in screen with one e-mail field) and now fails at "expected 1 text box, found 0"; the others need screens that do not exist |
| Browser: groups, members, account | 18 | 18 | the scene needs `POST /groups` (500) |
| Browser: contact form, tags, attributes | 13 | 13 | as above |
| Browser: list, filter, export | 14 | 14 | as above |
| Browser: Trash and History | 13 | 13 | as above |
| Browser: versions, duplicates, merge | 13 | 13 | as above |
| End-to-end E2E-01 to E2E-08 | 8 | 8 | E2E-01 stops at the missing register form; the others at the scene (`POST /groups` 500) |
| Browser, total (Chromium) | 104 | 104 (after the fix of AC-018-01) | first run: 103 failed, 1 passed |

Tests that passed on the skeleton before they were strengthened: AC-018-01 / EX-018-01 (browser), EX-017-01 and EX-017-09 (backend: a Basic header gives 401 and the health check needs no token, both already true in the skeleton). The two backend tests now also call `GET /groups` with a valid token, which fails until US-002 exists, so that they fail for the behaviour of the story. The hidden suite: see below.

Hidden suite, run once through `hidden.json`: 251 cases (216 backend, 35 browser), 251 failed, 0 passed; the JUnit report `hidden-tests/report/junit.xml` was written. Categories: boundary 35, combination 83, invalid 14, other 94, role 25. The failures are the same behavioural ones as above (215 backend cases stop at `POST /groups`, one at the `/v3/api-docs` paths; the browser cases at the missing screens or at the scene). Three browser cases (R-018-01 to R-018-03) first passed on the skeleton's language switch; they were strengthened to need the sign-in screen and failed on the second run. The browser part of the first run could not start (a module-format error of the hidden Playwright configuration); that harness defect was fixed and the whole suite was run again, which is the run recorded here.

## Test measures (phase 5)

<!-- Level rows: tests passed / failed. unit = unit tests only; non-unit = integration, acceptance, architecture and end-to-end tests under the coverage tool; all levels = one merged report. Mutation score only on the all-levels row, with scope, tool, killed of total and the tests run. -->

| Component | Level | Tests passed / failed | Line coverage | Branch coverage | Mutation score (raw / adjusted, scope) | Tools |
|---|---|---|---|---|---|---|

## Quality gate (phase 5)

| Component | Line threshold met | Mutation threshold met | Shortfall (points) | Survivor rounds | Tests at first mutation run | Decision |
|---|---|---|---|---|---|---|

## Equivalent mutants (phase 5)

| Component | Class and method | Mutation | Why it cannot change behaviour |
|---|---|---|---|
