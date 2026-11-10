# Verification report

> For: agent (writes), human (reads) · Written in: phase 6 (`/verify`; `/fix` fills only the resolution column) · Source: `general/quality/*`, `general/security/*`, `general/design/ui-baseline.md`, `project/*` · Procedure: `skills/verify` · Agent: writes

One section per `/verify` run (`## Run 1`, `## Run 2`, …), each a self-check in a separate session, not an independent review.

## Run 1

**This is a self-check by the agent in a separate session (S7, phase 6), not an independent review.** Commit under test: `d3b8e97` (HEAD; the hidden evaluation of the runner is for the same commit). Date: 2026-11-10. Result: **gate not passed**, 5 High and 6 Medium findings open (table "Findings").

Method in short: manifests re-hashed; Playwright full run (3 engines, 399 tests, 2 workers) plus a one-worker re-run of every failure outside US-020 to US-022; vitest with coverage; backend `mvn verify` (Maven image, Testcontainers); format, lint, static analysis, duplication, dependency, licence and secret scans; the production Compose set-up built and started with placeholder Firebase values and a self-signed certificate in a scratch folder (outside the repository, torn down afterwards); NFR scripts against the running Compose stack. Raw output: `out/logs/verify-*.log`, `out/logs/gitleaks-verify.json`, `out/logs/screenshots/`. Not run (a command needed an approval that was not available): `restart-test.sh`, `restore.sh`, `clean-host-test.sh`; their earlier logs are cited and marked as earlier evidence.

### Definition of Done

| DoD | Result | Evidence |
|---|---|---|
| DoD-R-01 full suite passes; frozen hashes match | **Not met** | Acceptance manifest: 43 of 43 hashes match. Input manifest: 73 of 73 match after normalising CRLF to LF (38 files differ only by Windows line endings, F-V12). Vitest 207 of 207 pass. Backend ITs 491 run, 52 fail (US-017 two checks, US-020 11, US-021 21, US-022 18; all because US-020 to US-022 are not built, F-V02). Playwright 399 run: 310 pass first time; of the 89 failures 38 are US-020 to US-022 or E2E-06, 47 passed in the one-worker re-run (load flakiness, F-V07), 4 still fail (F-V04, F-V05, F-V06). |
| DoD-R-02 format, lint, type check, static analysis | **Not met** | prettier, eslint, tsc clean; jscpd 3.46 % duplicated lines, no threshold failure. Backend: PMD and CPD pass; `spotless:check` fails on `src/test/java/app/contactgroups/it/MigrationIT.java` (F-V08). Log `verify-backend-check.log`, `verify-backend-pmd.log`. |
| DoD-R-03 quality gate per component | **Not met** | Frontend line coverage 94.61 % (gate 95 %; statements 92.11 %, branches 87.64 %). Backend coverage cannot be measured: `jacoco:report` fails with "malformed input around byte 2" also with `-Dfile.encoding=UTF-8` (`verify-backend-jacoco.log`, F-V09). Mutation not re-run (no component change since phase 5 except the frontend fixes of F-01 to F-04). |
| DoD-R-04 architecture constraints | Met | `ArchitectureTest` 7 of 7 pass (`verify-backend-verify.log`). |
| DoD-R-05 no open Critical or High | **Not met** | F-V01 to F-V04 and F-V13 are High and open. |
| DoD-R-06 runtime and core flows | Met for INC-1 | See "Runtime demonstration". |
| DoD-R-07 AC, EX, R trace to a test and a commit | Met by tooling, not by result | `check-traceability.mjs`: 638 ids defined, 638 mapped; `check-tasks.mjs`: every R, AC, EX covered by a task. US-020 to US-022 have tests and no commits of production code. |
| DoD-R-08 final theme, three widths, no Critical or Serious a11y violation | **Not met** | The app still loads `tokens.wireframe.css` (`index.css:2`, `index.html` `data-theme="wireframe"`); `tokens.final.css` exists but is not imported (F-V03). Axe passes (see UI table). |
| DoD-R-09 READMEs work from a clean checkout | Not verified | No clone test run in this session; `clean-host-test.log` (earlier, 20:20 UTC) built the stack from the committed files plus `.env` and reached E2E-01. |
| DoD-R-10 release notes | Not verified | `docs/release-notes.md` exists; it is a phase 7 deliverable (`/release`). |
| DoD-R-11 decisions resolved; inputs unchanged | Met, two pending | D-01 to D-54 have a `Resolution`; D-55 (INC-2 scope) and D-56 (dependency advisories) are pending the human; manifests as above. |
| DoD-R-12 commit rules | **Not met**, known | F-06 and F-07 of `04_review-INC-1.md` stay as release-notes items (history cannot be rewritten). |
| DoD-R-13 hidden suite passes; manifest matches | **Not met** | `06_hidden-evaluation.md` Run 1, commit `d3b8e97` = HEAD, manifest ok: 251 tests, 189 passed, 62 failed (F-V01). |
| DoD-R-14 every NFR checked and met | **Not met** | See NFR table: NFR-07, NFR-09 (WebKit), NFR-10 (WebKit) not met; NFR-12 not re-run. |
| DoD-R-15 CI passes | **Not met** | Pipeline `.github/workflows/ci.yml` has no runner here. Equivalent local steps: backend check fails (spotless), backend `verify` fails (US-020 to US-022), frontend steps pass except coverage, browser tests fail; gitleaks and images pass. |
| DoD-R-16 production files build and start; every threat has a checked control | Met with one finding | Built and started with production configuration (below); proxy runs as root (F-V10). |
| DoD-P01 E2E-01 to E2E-08 | **Not met** | E2E-06 (US-021, US-022) not built. E2E-07 fails in WebKit only in the first run and passed in the one-worker re-run; E2E-02, E2E-04 likewise. All others pass in all three engines after the re-run. |
| DoD-P02 first contact under 120 s | Met (earlier evidence) | NFR-11 timing in `nfr-specs-chromium*.log` (S5); E2E-01 passes in Chromium (first run flaky, re-run passed). Walkthrough by a person who has not seen the product: not done. |
| DoD-P03 search and filter under 1 s | Met | NFR-01 below. |
| DoD-P04 export opens in Excel with all columns filled | Met by test, manual step open | AC-011-02, AC-011-11 pass; `export-time.mjs` reads the sheet back (1001 and 10001 rows). Manual opening in Excel: release notes item. |
| DoD-P05 isolation, non-member gets 404 | Met | AC-017-07 and E2E-04 pass (E2E-04 after the re-run). |
| DoD-P06 NFR-01 to NFR-09 on the deployed system | **Not met** | NFR-07 and NFR-09 not met in WebKit. |
| DoD-P07 OWASP ASVS 5.0 L2, no Critical or High open | Not fully evidenced | No ASVS checklist was produced in this session; SB, SR and threat tables below; the dependency-check result is in the CI section. |
| DoD-P08 delivery exists | Phase 7 | Install and backup guide are `/release` items; restore tested once (`backup-restore-test.log`, earlier). |
| DoD-P09 demo J1 to J5 from the delivered files | Partly | J1 to J5 pass against the dev stack, not against the production Compose set-up (it has no Firebase project here). |
| DoD-P10 second increment finished, J6 passes | **Not met** | US-020, US-021, US-022 are "ready" in `00_progress.md`; no production code (F-V02). |
| DoD-P11 verification report exists | Met | This file. |

### UI and accessibility

Source: Playwright specs `e2e/responsive.spec.ts`, `e2e/accessibility.spec.ts`, `e2e/language.spec.ts`, `checks/shell.spec.ts` in Chromium, Firefox, WebKit; screenshots re-created in this run (`out/logs/screenshots/responsive/`, `states/`, `shell-*`). I looked at `contacts-chromium-1280.png` against the wireframe theme; I did not compare every screen at every width with the wireframes in this session (limit of this self-check; F-10 of the review is unchanged).

| Screen | Widths and modes | Baseline result | Accessibility violations |
|---|---|---|---|
| Sign-in, verify, groups, members, contacts, filter, contact new and edit, history, Trash, tags | 320, 768, 1280; the spec also runs sl and dark | No horizontal scroll at 320 px in Chromium and WebKit; Firefox passed in the re-run (`NFR-08` flaked once under load). Theme is the wireframe theme, not the final theme (F-V03) | Axe (WCAG 2.1 AA tags): no Critical or Serious violation in the three engines after the re-run |
| Keyboard-only flow (sign in, list, create contact) | Chromium, Firefox pass | Focus visible | WebKit: no visible focus at tab stop 4 (F-V05, same as F-05 of the review) |
| Language switch, all screens of the main flows | sl, en | Chromium, Firefox pass | WebKit: after clicking `EN` the English heading is not shown (`checks/shell.spec.ts:66`, `e2e/language.spec.ts:60`, `:79`) (F-V04) |

### NFR

Measured on the dev Compose stack (backend, db, emulator, frontend) with the 10,000-contact seed.

| NFR | Measured | Pass value | Result |
|---|---|---|---|
| NFR-01 | list, search, filter p95 111 ms over all searches; worst single request (text with accents) p95 772 ms (`load-test.mjs --duration 60`, 20 users) | under 1,000 ms | Pass |
| NFR-02 | open a contact p95 28 ms | under 500 ms | Pass |
| NFR-03 | export of 1,000 contacts: slowest of 3 runs 289 ms, 1001 rows in the sheet | under 10 s | Pass |
| NFR-04 | 0 errors in 1,768 requests; capacity run with 100 users, no 5xx, no 429 | none | Pass |
| NFR-05 | earlier evidence, not re-run: data intact after restart, `/health` 200 (`restart-test.log`, 19:16 UTC, before F-01 to F-04 fixes) | data intact | Pass (earlier) |
| NFR-06 | earlier evidence, not re-run: 15 tables identical row counts and checksums (`backup-restore-test.log`, 20:18 UTC). The install guide text for daily backup and 14-day retention is a phase 7 item | identical | Pass (earlier) |
| NFR-07 | Chromium and Firefox pass after the re-run; WebKit: 4 tests fail (F-V04, F-V05). Manual Safari and Edge check not done | all pass | **Not met** |
| NFR-08 | no horizontal scroll at 320 px in the three engines after the re-run | none | Pass |
| NFR-09 | axe clean; WebKit keyboard focus fails at tab stop 4 | no Critical or Serious violation; visible focus | **Not met** (WebKit) |
| NFR-10 | `locales.test.ts` key parity passes (vitest); the language switch fails in WebKit | same keys, no key shown | **Not met** (WebKit) |
| NFR-11 | earlier evidence (S5) | every screen has the states; first contact under 120 s | Pass (earlier) |
| NFR-12 | `clean-host-test.log` (20:20 UTC, before the F-01 fix) reached E2E-01, which then failed at the saved message; not re-run | passes | Not evidenced for this commit |
| NFR-13 | earlier; `MigrationIT` passes in this run's `verify` (empty database and previous release) | both start, forward only | Pass |
| NFR-14 | `licence-check.mjs`: nothing forbidden; 17 direct Maven dependencies listed; 320 files searched | none forbidden | Pass |
| NFR-15 | export of 10,000 contacts: 552 ms, no error, backend not restarted | no error, no OOM | Pass |

### Threats and controls

Where a control is checked by an automated test, the test result is from this run's `mvn verify` or Playwright run.

| Threat | Control | How checked | Result |
|---|---|---|---|
| Forged or foreign token | SR-01 `FirebaseJwtDecoders`; emulator host guard | AC-017-01, -02, `EmulatorHostGuardTest`; live call with a bad token gets 401 | Pass (AC-017-01 fails in this run only because the contract now lists INC-2 operations, F-V02) |
| Token of a deleted account | `deleted_accounts` check | AC-016 API tests pass; browser AC-016-02 passed in the re-run | Pass |
| Unverified e-mail claims an invitation | `VerifiedEmailFilter`, BR-03 | AC-001-02, AC-003 tests | Pass |
| Group id or role in the body | SR-02, SR-16 | permission-matrix ITs; E2E-04 | Pass |
| SQL injection | SR-07 | EX-009-03, EX-010-06; PMD | Pass |
| Change or delete of History | SR-14 triggers | AC-013-09 | Pass |
| Lost update | AR-14 revision check | AC-005-07, E2E-05 | Pass |
| Formula in exported cell | SR-08 | AC-011-08 | Pass |
| Reading another group's data, probing existence | SR-02, SR-03 | AC-017-04, -07, EX-017-02, E2E-04, AC-002-03 | Pass |
| Personal data in logs, errors, URLs | SR-10, SR-11, SR-20 | AC-017-10; log test; live 401 body has no internals; actuator path needs a token | Pass |
| Invited e-mail seen by Editors | D-16 | Members and History tests | Pass |
| Erased data remains | SR-15 | AC-012-03 fails once in two full runs, passes 3 of 3 alone: the contact still answered 200 at the moment the UI had removed the row (F-V06) | **Finding** |
| Export by a Viewer | R-011-10 | AC-011-10 passes | Pass |
| Token or contact data in browser storage | SR-20 | code search at S5; not repeated | Not re-checked |
| Request flood, large bodies | SR-09 | AC-017-08, -09 pass | Pass |
| Elevation Editor to Owner; last Owner removal; removed member | SR-16, row lock, per-request membership | AC-004-*, EX-004-03, E2E-07 | Pass |
| Container breakout through a root process | SB-11 | `docker exec id`: backend uid 10001, frontend uid 101, proxy **uid 0** | **Finding** (F-V10) |
| Emulator reachable in production | production Compose | prod config has no emulator; backend `FIREBASE_AUTH_EMULATOR_HOST` empty; served `config.js` has no emulator URL; CSP has no `localhost:9099` | Pass |
| Health check reveals internals | `show-details: never` | live: `{"status":"UP"}` | Pass |
| XSS, clickjacking | React encoding, CSP, `frame-ancestors 'none'` | live headers on API and web app; CSP keeps `style-src 'unsafe-inline'` (F-V11) | Pass (Low note) |
| CORS to one origin (SR-05) | `SecurityConfig.cors` | live: `Origin: http://evil.example` gets 403 and no CORS header | Pass |
| TLS, HSTS (SB-04, SR-04) | production proxy | HTTP answers 301 to HTTPS; HTTPS answers with `Strict-Transport-Security: max-age=31536000; includeSubDomains` | Pass |
| Secrets (SB-03, SR-12) | `.gitignore`, `${VAR:?}` | gitleaks 8.28.0 over 194 commits: no leaks; `.env` value search over all tracked and untracked files: no file; over all commit messages: 0 | Pass |
| Dependencies (SB-08, SR-19) | `npm audit` allow-list D-08; dependency-check | `scripts/audit.mjs`: no open High or Critical, one allowed by D-08. Backend `dependency-check-maven` 12.1.6 (NVD, no key) fails the gate (CVSS >= 7.0): `spring-core` and `spring-web` 6.2.19 (12 CVEs, up to 9.8), `spring-security-core`, `-web`, `-oauth2-resource-server` 6.5.11 (CVE-2026-59270 at 9.1, two at 7.4), `swagger-ui` 5.28.1 bundled DOMPurify 3.2.4 (CVE-2026-116803 at 10.0); `commons-lang3` and `log4j-api` below 7. D-13 recorded other advisories (GHSA ids) and its resolution does not name these CVEs | **Finding** (F-V13) |
| Personal data in the repository, retention (SB-13) | `TrashCleanupService`; ADR-08 | cleanup of old `deleted_accounts` rows still open (F-08 of the review) | Open, Medium |
| SB-12, SB-14 | data minimisation, consent | code search at S5; not repeated | Not re-checked |

### CI and deployment

- Local equivalent of the pipeline jobs: `frontend` (check pass, contracts, tests pass with coverage below gate, build pass inside the image, audit pass, licences pass, browser tests fail as above), `backend` (check fail on spotless, verify fail on INC-2 ITs, dependency scan fails: 8 artifacts with CVSS 7.0 or higher, F-V13; log `verify-backend-depcheck.log`, 38 min without an NVD key), `secrets` (pass), `images` (pass: both images built in the production Compose build).
- Production deployment: scratch copy of `docker-compose.prod.yml` (absolute build contexts, host ports 18080/18443, other project name; placeholder values, self-signed certificate; nothing in the repository was changed). `up --build --wait`: db, backend, frontend, proxy all healthy. HTTP to HTTPS redirect 301; HSTS and security headers present; `/api/health` 200 through the proxy; unauthenticated `/api/groups` 401. The stack was removed with `down -v`.
- Limit: no real Firebase project, so sign-in against production configuration could not be exercised.

### Runtime demonstration

| Component | Environment | Flows exercised | Result |
|---|---|---|---|
| backend, db, emulator, frontend | Compose dev stack, seed data | E2E-01 to E2E-05, E2E-07, E2E-08 and all INC-1 acceptance specs in Chromium, Firefox, WebKit; 10,000-contact load and export scripts | INC-1 flows work; failures as in the findings |
| all four, production configuration | scratch Compose copy | start, health, redirect, headers, 401 | Works; no sign-in flow (no Firebase project) |

### Traceability

`scripts/check-traceability.mjs` (638 ids, all mapped in specification section 10) and `scripts/check-tasks.mjs` (every R, AC, EX covered by a task) pass. Test results per id are in the Playwright and `verify-backend-verify.log` outputs; ids of US-020 to US-022 have frozen tests that fail because the code does not exist.

### Findings

| F | Severity | Source | Finding | Resolution |
|---|---|---|---|---|
| F-V01 | High | hidden evaluation Run 1 | Held-out cases fail: R-001-06 (other), R-010-10 (other), R-018-01 (other), R-018-02 (combination), R-018-03 (boundary), each in INC-1; R-020-03, R-020-09, R-021-07, R-021-12, R-022-01, R-022-02, R-022-08, R-022-11 (INC-2); and 49 failures the runner reports as "unknown rule" (15 other, 10 combination, 5 invalid, 4 boundary, 2 role in the INC-1 range cannot be told apart from INC-2; the categories are all the runner gives). Likely cause of the INC-2 part is F-V02; the INC-1 part and the unknown part need `/fix` to analyse from the category only | open |
| F-V02 | High | US-020, US-021, US-022; DoD-P10 | The second increment is not built: 52 backend ITs (US-020 11, US-021 21, US-022 18, US-017 two checks that count operations of the contract) and 38 browser tests fail with 404 or missing UI; E2E-06 fails | open (needs `/implement` for the three stories; the progress file lists them as "ready", so this is a scope question for the human: build INC-2 now or accept it as out of scope) |
| F-V03 | High | DoD-R-08 | The final theme is not applied: `src/styles/index.css` imports `tokens.wireframe.css`; `tokens.final.css` is not imported | open |
| F-V04 | High | NFR-07, NFR-10; R-018-02, R-018-03 | In WebKit the language switch does not show the English heading after clicking `EN` on the sign-in screen (`checks/shell.spec.ts:66`, `e2e/language.spec.ts:60`, `:79`); reproducible with one worker. Matches the failing hidden cases R-018-02 and R-018-03 | open |
| F-V05 | Medium | NFR-09 | WebKit: no visible focus at tab stop 4 (`e2e/accessibility.spec.ts:49`); same as F-05 of the review | open |
| F-V06 | Medium | SR-15, AC-012-03 | After "Delete forever" the contact answers 200 for a moment although the row is gone from the UI; failed in 2 of 2 full runs, passed 3 of 3 alone (`acceptance/trash-history.spec.ts:48`). Either the UI hides the row before the server has erased it, or the erase is not synchronous | open |
| F-V07 | Medium | tests | At 2 workers 47 of 51 INC-1 failures pass when re-run alone: timing sensitivity of the frozen tests or slow responses under load; the evidence of a green run needs one worker | open |
| F-V08 | Medium | DoD-R-02 | `spotless:check` fails on `MigrationIT.java` (two over-long lines) | open |
| F-V09 | Medium | DoD-R-03 | Backend coverage cannot be measured (JaCoCo "malformed input"); frontend line coverage 94.61 % is below the gate of 95 % | open |
| F-V10 | Medium | SB-11 | The production `proxy` container (stock `nginx:1.29.1`) runs as uid 0; the other images run non-root | open |
| F-V11 | Low | SR-06 | The web CSP allows `style-src 'unsafe-inline'` | open |
| F-V13 | High | SB-08, SR-19, DoD-R-05 | Backend dependency scan reports Critical and High advisories on Spring Framework 6.2.19, Spring Security 6.5.11 and the DOMPurify copy inside swagger-ui 5.28.1 (CVSS up to 10.0; matches are by CPE and may include false positives; two CVE records could not be stored by the scanner because of a database column length, so the NVD data is incomplete). Not covered by an approved decision. Needs: check reachability and fixed versions, then an approval decision (D-13 option 1 path) or a version change by the human | open |
| F-V12 | Low | manifest | 38 of 73 input files are checked out with CRLF line endings; the hashes match only after normalising; the content is unchanged. `.gitattributes` exists; the clone setting is the cause | open (human) |
| (carried) | Medium | review F-06, F-07, F-08 | Missing `Red:` lines, commit size and subject length, `deleted_accounts` cleanup: unchanged | open |

### Fix loops

| Loop | Findings | Change | Re-verification |
|---|---|---|---|
| 1 | F-V01 to F-V12 | not started | |
