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
| F-V04 | High | NFR-07, NFR-10; R-018-02, R-018-03 | In WebKit the language switch does not show the English heading after clicking `EN` on the sign-in screen (`checks/shell.spec.ts:66`, `e2e/language.spec.ts:60`, `:79`); reproducible with one worker. Matches the failing hidden cases R-018-02 and R-018-03 | not reproduced (S8): on the rebuilt stack at HEAD the three tests and `us018-language.spec.ts` pass in WebKit, 42 of 42 with 3 repeats and 1 worker (`fix-fv04-webkit-repeat.log`); no code change. The cause of the run 1 failure is unknown (a stale image or load); the hidden R-018 cases are re-measured in run 2. Stays open until then |
| F-V05 | Medium | NFR-09 | WebKit: no visible focus at tab stop 4 (`e2e/accessibility.spec.ts:49`); same as F-05 of the review | fixed `ce02792`: not an app defect; WebKit leaves links out of the Tab order and tab stop 4 was the body (focus on the browser's controls). The non-frozen test now skips body stops in WebKit and checks the focus ring of the group link directly; passes in the three engines (`fix-fv05-webkit-after.log`) |
| F-V06 | Medium | SR-15, AC-012-03 | After "Delete forever" the contact answers 200 for a moment although the row is gone from the UI; failed in 2 of 2 full runs, passed 3 of 3 alone (`acceptance/trash-history.spec.ts:48`). Either the UI hides the row before the server has erased it, or the erase is not synchronous | cause found, decision D-57 (needs the developer): the erase is synchronous; the open modal dialog hides the page from role queries, so the frozen test's `toHaveCount(0)` passes before the DELETE ends. App unchanged. Stays open until D-57 is answered | fixed (D-57: 1): the frozen test AC-012-03 waits for the dialog to close before the row check; manifest updated and verified. Re-measured in `/verify` run 2 |
| F-V07 | Medium | tests | At 2 workers 47 of 51 INC-1 failures pass when re-run alone: timing sensitivity of the frozen tests or slow responses under load; the evidence of a green run needs one worker | open |
| F-V08 | Medium | DoD-R-02 | `spotless:check` fails on `MigrationIT.java` (two over-long lines) | fixed `44f6e8e`; `spotless:check` passes |
| F-V09 | Medium | DoD-R-03 | Backend coverage cannot be measured (JaCoCo "malformed input"); frontend line coverage 94.61 % is below the gate of 95 % | open |
| F-V10 | Medium | SB-11 | The production `proxy` container (stock `nginx:1.29.1`) runs as uid 0; the other images run non-root | fixed `e42dbeb`: `user: "101:101"` and temp paths in `/tmp`; started as 101 with a test certificate, 301, HTTPS and HSTS work. The customer's `tls.key` must be readable by uid 101 (note for the install guide, phase 7). The whole production set was not rebuilt in this session |
| F-V11 | Low | SR-06 | The web CSP allows `style-src 'unsafe-inline'` | open |
| F-V13 | High | SB-08, SR-19, DoD-R-05 | Backend dependency scan reports Critical and High advisories on Spring Framework 6.2.19, Spring Security 6.5.11 and the DOMPurify copy inside swagger-ui 5.28.1 (CVSS up to 10.0; matches are by CPE and may include false positives; two CVE records could not be stored by the scanner because of a database column length, so the NVD data is incomplete). Not covered by an approved decision. Needs: check reachability and fixed versions, then an approval decision (D-13 option 1 path) or a version change by the human | partly assessed, D-56: Spring Framework Criticals and swagger-ui/DOMPurify not reachable (no SSE, no XSLT view; swagger-ui disabled, backend not published in production); Spring Security 6.5.11 **not assessed** (no advisory data available in this session); no version changed. Needs the developer. Open | second loop (S8, D-56): (c) the three named Spring Security CVEs (59270, 41707, 47841) accepted as a known limitation by the developer, none reachable (no LDAP, no WebAuthn, DPoP not active; new test `SecurityHeadersIT.aDpopAuthorizationHeaderIsRefused`); next-version item in the release notes. (d) `springdoc-...-webmvc-ui` replaced by `-webmvc-api`: swagger-ui and DOMPurify (CVE-2026-116803) leave the build. Two further Spring Security CVEs below 7.0 and the 12 `spring-core`/`spring-web` CPE matches are still not assessed one by one. Re-scan in `/verify` run 2; closed there if the scan shows nothing else at 7.0 or higher that is not covered by a decision |
| F-V12 | Low | manifest | 38 of 73 input files are checked out with CRLF line endings; the hashes match only after normalising; the content is unchanged. `.gitattributes` exists; the clone setting is the cause | open (human) |
| (carried) | Medium | review F-06, F-07, F-08 | Missing `Red:` lines, commit size and subject length, `deleted_accounts` cleanup: unchanged | open |

### Fix loops

| Loop | Findings | Change | Re-verification |
|---|---|---|---|
| 1 | F-V04, F-V05, F-V06, F-V08, F-V10, F-V13 | S8: F-V05, F-V08, F-V10 fixed; F-V04 not reproduced; F-V06 and F-V13 wait for the developer (D-57, D-56). F-V01 to F-V03, F-V07, F-V09 to F-V12 not in this loop | `/verify` run 2 after INC-2, theme, quality gate |
| 2 | F-V13, F-V06 | S8: F-V06 fixed by D-57 (frozen test, manifest updated); F-V13 (c) accepted per CVE by D-56, DPoP refusal test added, springdoc `-ui` replaced by `-api` | `/verify` run 2 (dependency re-scan, AC-012-03) |

## Run 2

**This is a self-check by the agent in a separate session (S7, phase 6), not an independent review.** Commit under test: `2246948` (HEAD; the runner's hidden evaluation Run 2 is for the same commit). Date: 2026-11-16. Result: **gate not passed**, 4 High findings open (F-V01, F-V13, F-V14, F-V16). Developer answer applied: D-66 option 1 (accept the frontend mutation shortfall for this run).

**Mutation threshold not met (D-66, accepted for this run).** The frontend mutation score is below the 90 % threshold. The 77.9 % figure is an **estimate** from per-file Stryker runs (full-run base 2302 of 3643 killed plus the kills of the three survivor rounds), **not a full run**. It was not re-measured here (a full run is a developer task outside the agent, D-66 option 3). Backend mutation (PIT, developer's run, read in S6): 91.1 %, threshold met.

Method: manifests re-hashed (CRLF normalised as in run 1); vitest with coverage; Playwright full run, 1 worker, 3 engines, 399 tests against the dev Compose stack (images built in the design session; `git diff d6ffe07 HEAD` shows only test files changed in `frontend/src` and `backend/src/main` unchanged, so the images equal HEAD production code; a rebuild at HEAD fails, F-V14); backend `mvn verify spotless:check pmd:check pmd:cpd-check` in the Maven image; NFR scripts; gitleaks 8.28.0; `.env` value search; `audit.mjs`, `licence-check.mjs`, `check-traceability.mjs`, `check-tasks.mjs`. Raw output: `out/logs/verify2-*.log`, `gitleaks-verify2.json`. **Not run:** backend `dependency-check` (38 min), `restart-test.sh`, `restore.sh`, `clean-host-test.sh` (would stop at the same build failure), production Compose build and start (same `./frontend` build, same failure), manual Safari/Edge check, a person's first-contact walkthrough.

### Definition of Done

| DoD | Result | Evidence |
|---|---|---|
| DoD-R-01 full suite passes; frozen hashes match | Met except flakes | Input manifest 73 of 73 (38 only after CRLF to LF, F-V12); acceptance 43 of 43 (includes the AC-012-03 line); hidden manifest ok per the runner. Vitest: first run 370 of 377 (7 timing failures, all pass re-run alone), second run 377 of 377. Backend: 118 unit and 492 integration tests, 0 failures. Playwright 398 of 399; the one failure (WebKit `e2e/language.spec.ts:60`, 30 s timeout waiting for `main`) passed 9 of 9 on repeat (F-V07). E2E-06 and AC-012-03 pass. |
| DoD-R-02 format, lint, type check, static analysis | **Not met** | eslint clean; spotless, PMD, CPD pass; `tsc --noEmit` fails (F-V14); prettier fails on 8 test files (F-V15). |
| DoD-R-03 quality gate per component | **Not met** | Lines: frontend 97.03 % (statements 95.33 %, branches 93.37 %), backend 97.12 % (branches 89.07 %); JaCoCo works now. Mutation: backend 91.1 % met; **frontend below 90 %, estimate 77.9 %, not a full run; accepted for this run by D-66**. |
| DoD-R-04 architecture constraints | Met | `ArchitectureTest` is in the 118 unit tests, 0 failures. |
| DoD-R-05 no open Critical or High | **Not met** | F-V01, F-V13, F-V14, F-V16. |
| DoD-R-06 runtime and core flows | Met | E2E-01 to E2E-08 pass in three engines. |
| DoD-R-07 AC, EX, R trace to a test and a commit | Met | 638 ids mapped; 66 tasks cover every R, AC, EX. |
| DoD-R-08 final theme, three widths, a11y | Met | Final theme applied; contacts screen at 1280 px looked at; responsive and accessibility specs pass in 3 engines. Not every screen compared with its wireframe in this session. |
| DoD-R-09 READMEs work from a clean checkout | **Not met** | A clean build fails at `npm run build` (F-V14). |
| DoD-R-10 release notes | Not verified | Phase 7. |
| DoD-R-11 decisions resolved; inputs unchanged | Met, some pending | Manifests match; D-66 answered here; D-55, D-56, D-64 and others "pending review". |
| DoD-R-12 commit rules | **Not met** | F-06, F-07 carried; `backend/pom.xml`, `acceptance/trash-history.spec.ts`, its manifest line and `release-notes.md` from the fix loops are uncommitted (F-V17). |
| DoD-R-13 hidden suite passes | **Not met** | Run 2, commit `2246948` = HEAD, manifest ok: 251 tests, 204 passed, 47 failed (F-V01); Run 1 had 62 failures. |
| DoD-R-14 every NFR checked and met | **Not met** | NFR-12 not met; NFR-05, NFR-06 not re-evidenced; NFR-14 finding. |
| DoD-R-15 CI passes | **Not met** | Frontend job fails at tsc and prettier; audit (F-V16) and licence check (F-V17) fail; backend job passes locally. |
| DoD-R-16 production build and start; threats controlled | **Not met** | Production frontend image cannot be built (F-V14); not started. |
| DoD-P01 E2E-01 to E2E-08 | Met | All pass in three engines, E2E-06 included. |
| DoD-P02 first contact under 120 s | Met | WebKit 7.8 s; no walkthrough by a person. |
| DoD-P03 search and filter under 1 s | Met | NFR-01. |
| DoD-P04 export opens in Excel | Met by test | Manual opening in Excel is a release-notes item. |
| DoD-P05 isolation, non-member 404 | Met | AC-017-07, E2E-04. |
| DoD-P06 NFR-01 to NFR-09 on the deployed system | **Not met** | Not run against the production set-up. |
| DoD-P07 OWASP ASVS 5.0 L2, no Critical or High open | **Not met** | F-V13, F-V16 open; no ASVS checklist produced. |
| DoD-P08 delivery exists | Phase 7 | |
| DoD-P09 demo J1 to J5 | Partly | Dev stack only. |
| DoD-P10 second increment finished, J6 passes | Met | US-020 to US-022 built; E2E-06 and their acceptance tests pass. |
| DoD-P11 verification report exists | Met | This file. |

### UI and accessibility

Playwright `checks/`, `e2e/responsive`, `e2e/accessibility`, `e2e/language` in Chromium, Firefox, WebKit at 320, 768, 1280 px: all pass except the one WebKit timeout (passed on repeat). No horizontal scroll at 320 px; no Critical or Serious axe violation; WebKit keyboard and language tests pass. Screenshots re-created in `out/logs/screenshots/`.

### NFR

| NFR | Measured | Pass value | Result |
|---|---|---|---|
| NFR-01 | `load-test.mjs --duration 60`, 20 users, 10,000 contacts: all searches p95 565 ms; worst (text with accents) p95 796 ms | under 1,000 ms | Pass (watch item D-46) |
| NFR-02 | open a contact p95 24 ms | under 500 ms | Pass |
| NFR-03 | 1,000 contacts: slowest of 3 runs 245 ms, 1001 rows | under 10 s | Pass |
| NFR-04 | 0 errors of 1,740 requests | none | Pass |
| NFR-05, NFR-06 | not re-run; earlier logs | | Not re-evidenced |
| NFR-07 | three engines pass (one flake, 9 of 9 on repeat); manual Safari and Edge not done | all pass | Pass for the engines, manual open |
| NFR-08 | no horizontal scroll at 320 px | none | Pass |
| NFR-09 | axe clean; WebKit focus test passes | no Critical or Serious | Pass |
| NFR-10 | key parity and language specs pass | same keys | Pass |
| NFR-11 | empty and error state specs pass | | Pass |
| NFR-12 | clean build fails at `tsc` (F-V14) | passes | **Not met** |
| NFR-13 | `MigrationIT` passes | both start | Pass |
| NFR-14 | licences fine; `springdoc-openapi-starter-webmvc-api` is in neither `tech-stack.md` nor `decisions-log.md` by that id (F-V17) | none forbidden | **Finding** |
| NFR-15 | 10,000 contacts export 613 ms, backend not restarted | no error | Pass |

### Threats and controls

Rows covered by tests pass again (492 backend integration tests incl. AC-017-* and `SecurityHeadersIT`; E2E-04, E2E-07). Changes against run 1:

| Threat | Result in run 2 |
|---|---|
| Erased data remains (SR-15) | Pass: AC-012-03 passes (F-V06 closed). |
| Root process in the proxy (SB-11) | Fix of F-V10 not re-started: the production set cannot be built (F-V14). |
| Secrets (SB-03, SR-12) | Pass: gitleaks over 215 commits, no leaks; `.env` value search: no file listed, 0 commit-message hits. |
| Dependencies (SB-08, SR-19) | **Finding**: `audit.mjs` fails on High advisories without a decision: `image-size` (GHSA-5p2g-fcmc-qvqq, GHSA-w3rx-r6r6-pgpr) and `nodemailer` (five ids). `npm audit --omit=dev` shows only the `@grpc/grpc-js` chain allowed by D-08, so the new ones sit in build and test tooling (F-V16). Backend `dependency-check` not re-run; the swagger-ui removal in `pom.xml` is uncommitted, so HEAD still has it (F-V13). |
| Others (injection, forged tokens, History immutability, lost update, formula injection, CORS) | Pass by tests; live header and TLS checks of run 1 not repeated (no production start). |
| SB-12, SB-14, SR-20 | Not re-checked. |

### CI and deployment

Backend `verify`, `spotless:check`, `pmd:check`, `pmd:cpd-check`: BUILD SUCCESS (8 min 33 s). Frontend: eslint, vitest pass; `tsc`, prettier, `audit.mjs`, `licence-check.mjs` fail. Gitleaks passes. **Production deployment not built:** `docker compose up --build` stops at the frontend image because `npm run build` runs `tsc --noEmit`: error TS6133 at `src/features/contacts/contact-page-more.test.tsx(304,44)`, `'m'` is declared but never read; `docker-compose.prod.yml` builds the same `./frontend`.

### Runtime demonstration

| Component | Environment | Flows exercised | Result |
|---|---|---|---|
| backend, db, emulator, frontend | dev Compose stack from earlier images (equal to HEAD production code), seed data | E2E-01 to E2E-08 and all acceptance specs in three engines (399 tests, 1 worker, 24.7 min); load and export scripts | Works; one WebKit timeout, 9 of 9 on repeat |
| production configuration | not started | | Blocked by F-V14 |

### Traceability

`check-traceability.mjs`: 638 ids defined, 638 mapped. `check-tasks.mjs`: 22 stories, 66 tasks, every R, AC, EX covered. Results per id: `verify2-playwright.log`, `verify2-backend-verify.log`.

### Findings, run 2

| F | Severity | Source | Finding | Resolution |
|---|---|---|---|---|
| F-V01 | High | hidden evaluation Run 2 | 47 held-out cases fail (Run 1: 62). Named: R-001-01 to R-001-09 (other, boundary, boundary, other, combination, other, combination, combination, other); R-018-01 to R-018-08 (other, combination, boundary, boundary, other x4); R-009-05 (other), R-009-06 (role), R-009-08, R-009-09 (other); R-010-10, R-010-11 (other); R-011-01 (role), R-011-02 (other), R-011-03 (boundary); R-014-06 (other); R-020-03, R-020-09; R-021-07, R-021-12; R-022-01 (other), R-022-02 (combination), R-022-08, R-022-11 (other); and 12 "unknown rule" (4 combination, 2 invalid, 2 boundary, 4 other). All R-001 and R-018 cases fail together while the local acceptance tests of both pass, so a common cause is possible; `/fix` must analyse from the category only | open, release notes (D-68: 1, Slovenian default kept; groups: 17 R-001/R-018, 12 unknown rule, 18 scattered; the hypothesis does not explain the last 18, the unknown 12, nor Run 1 to Run 2 getting worse); no code change |
| F-V14 | High | DoD-R-02, R-09, R-15, R-16; NFR-12 | `tsc --noEmit` fails on the committed test file `contact-page-more.test.tsx:304` (unused `m`); the frontend image, compose stack, production deployment, clean-host test and CI frontend job fail at HEAD (commit `2246948`, survivor rounds 2 and 3) | fixed in 38aa018 (`tsc` clean, vitest 377 of 377); image build not re-run |
| F-V15 | Medium | DoD-R-02 | `prettier --check` fails on 8 test files of the same commit (contact-form, contact-page-more, contact-table, contact-values, filter-dialog, merge-page, export-hooks, versions-page) | fixed in 38aa018 (`prettier --check .` clean) |
| F-V16 | High | SB-08, SR-19 | `audit.mjs` fails: High advisories without a decision on `image-size` (2) and `nodemailer` (5), new since run 1, in build and test tooling only. Needs an approval decision (D-08 path) or a tooling update | not reproduced: `audit.mjs` passes, neither package is in the lockfile (D-69) |
| F-V17 | Medium | NFR-14, DoD-R-12 | `licence-check.mjs` fails on `springdoc-openapi-starter-webmvc-api` (not named by id in `tech-stack.md` or `decisions-log.md`); the fix-loop changes (`pom.xml`, `trash-history.spec.ts` with its manifest line, `release-notes.md`) are uncommitted, so HEAD differs from the working tree | fixed in f552ccf (D-67; `licence-check.mjs` PASS) |
| F-V13 | High | SB-08, SR-19 | Backend dependency scan not re-run this session; the `pom.xml` change is uncommitted; Spring Framework advisories not assessed one by one | re-scanned (`logs/verify3-backend-depcheck.log`), `pom.xml` committed; swagger-ui gone; 12 Spring Framework 6.2.19 CVEs: CVE-2026-47884, -59283, -59313 not reachable (accepted, D-70: 3); the other 9 accepted as known limitations with the developer's preconditions and evidence (D-70 round 2: dependency tree `logs/verify4-backend-dependency-tree.log` has no webflux, aalto, rsocket, jetty; no SpEL, no SSE; CVE-2026-59282 checked in every controller, no object binding of query or form params). All 12 accepted; release note: upgrade to Spring Boot 4 / Framework 7.0.9. Closed as accepted, for `/verify` to confirm |
| F-V07 | Medium | tests | Timing flakes at 1 worker: vitest 7 failures in one full run (377 of 377 in the next), Playwright 1 WebKit timeout (9 of 9 on repeat) | open item for the release notes (D-69) |
| F-V04 | Medium | NFR-10, R-018-* | Not reproduced locally (language tests pass in three engines); hidden R-018-01 to -08 still fail | open (see F-V01) |
| F-V09 | Medium | DoD-R-03 | JaCoCo reports; backend lines 97.12 %, frontend lines 97.03 % | closed |
| F-V06 | Medium | SR-15 | AC-012-03 passes (D-57) | closed |
| F-V05, F-V08 | Medium | | keyboard test and spotless pass | closed |
| F-V10 | Medium | SB-11 | proxy as uid 101; not re-started | closed in run 1, not re-run |
| F-V02, F-V03 | High | | INC-2 built; final theme applied; tests pass | closed |
| D-66 | Low | DoD-R-03 | Frontend mutation score below the 90 % threshold, estimate 77.9 % from per-file runs, not a full run; also the FilterDialog stale `aria-invalid` flag | accepted (D-66: 1) |
| F-V11, F-V12, F-06, F-07, F-08 | Low, Medium | | unchanged | open |

Run 2 of at most 3 rounds; no finding has been open for three runs. Next: `/fix`.
