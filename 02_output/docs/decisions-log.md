# Decisions log

> For: agent (writes), human (reads) · Written in: every session · Format: `general/rules/decisions.md` ("Decision record") · Agent: appends only

## D-01: Host Node.js and npm are other major versions than the pins
- Timestamp: 2026-11-09
- Session: S1 preflight
- Class: block
- Type: blocking
- Trigger: `tech-stack.md` pins Node 22.20.0 and npm 10.9.3; the host runs Node 24.13.0 and npm 11.6.2 (`node --version`, `npm --version`). The agent must not install or upgrade software on the host.
- Options: 1. (proposed) Install Node 22.20.0 (for example with nvm-windows or a portable copy) so the pins hold; npm 10.9.3 comes with it. 2. Approve Node 24.13.0 / npm 11.6.2 as the project's versions (replaces the `tech-stack.md` entries for this run). 3. Run frontend tooling in a `node:22.20.0` container.
- Human response: "2" (via `/preflight D-01: 2`, 2026-11-09)
- Resolution: option 2. Node 24.13.0 and npm 11.6.2 are the project's versions for this run. See D-09 for a side effect (firebase-tools engine range).
- Overrides: tech-stack.md platforms `node`, tooling `npm` (option 2 chosen; they now read 24.13.0 and 11.6.2)

## D-02: Host Java and Docker report other versions than the pins
- Timestamp: 2026-11-09
- Session: S1 preflight
- Class: open
- Type: non-blocking
- Trigger: pin Temurin 21.0.8, host Oracle Java 21.0.11 (same major, newer patch, other vendor); pin Docker 28.4.0, host 29.9.3. Both run. The listed pins stay authoritative (`tech-stack.md` Rules); the container image `eclipse-temurin:21.0.8_9-jre` is unaffected.
- Options: 1. (proposed) Accept the host versions for local builds; the delivered images use the pinned tags. 2. Install the exact pins.
- Human response: "1" (via `/preflight D-02: 1`, 2026-11-09)
- Resolution: option 1. Host versions accepted for local builds; delivered images use the pinned tags.
- Overrides: none

## D-03: PostgreSQL host binary and dependency scan
- Timestamp: 2026-11-09
- Session: S1 preflight
- Class: open
- Type: non-blocking
- Trigger: `postgres --version` is not available on the host (PostgreSQL runs as the `postgres:17.6` container, which resolves); the dependency scan (OWASP dependency-check, `npm audit`) cannot run because `02_output/` holds no `pom.xml` or `package.json` yet, and no scan tool is installed.
- Options: 1. (proposed) Check the PostgreSQL version through `docker run --rm postgres:17.6 postgres --version`, and scan the pinned set from a temporary manifest generated from `tech-stack.md` in `out/logs/` in the next `/preflight`. 2. Move the dependency scan to the first `/implement` (not allowed by the phase 0 gate).
- Human response: "1" (via `/preflight D-03: 1`, 2026-11-09)
- Resolution: option 1. PostgreSQL confirmed as `PostgreSQL 17.6 (Debian 17.6-2.pgdg13+1)` with `docker run --rm postgres:17.6 postgres --version`. The scan ran from a temporary manifest in `out/logs/preflight-scan/` (npm) and OSV queries (npm and Maven); the Maven transitive scan could not run, see D-10.
- Overrides: none

## D-04: Same-minor patch replacements for pins with known vulnerabilities
- Timestamp: 2026-11-09
- Session: S2 preflight (`/preflight D-01: 2, D-02: 1, D-03: 1`)
- Class: allow
- Type: non-blocking
- Trigger: OSV and `npm audit` report High or Critical findings for four pins (`out/logs/preflight-resolve.json`, `preflight-npm-audit-all.json`). Each has a fix in the same minor line (`general/rules/decisions.md`, non-blocking: latest stable patch of the same minor with no known Critical or High):
  - Spring Boot 3.5.6 (`spring-boot-starter-actuator` GHSA-8hfc-fq58-r658 and GHSA-mgvc-8q2h-5pgc, High; fixed in 3.5.12) -> 3.5.16, no known findings; applies to all `spring-boot-starter-*` entries.
  - `org.postgresql:postgresql` 42.7.8 (GHSA-98qh-xjc8-98pq and GHSA-j92g-9f8w-j867, High; fixed in 42.7.12) -> 42.7.14, no known findings.
  - `vitest` 3.2.4 (GHSA-5xrq-8626-4rwp, Critical; fixed in 3.2.6) -> 3.2.7; one Moderate remains (GHSA-82fw-gwwq-j7x9, fixed only in 4.x).
  - `@vitest/coverage-v8` 3.2.4 -> 3.2.7 (kept equal to vitest; `npm audit` fix target).
- Options: 1. (proposed, applied) Use these patch versions in the manifests; `/plan` and `/implement` take them from here. 2. Keep the old pins (not possible: High/Critical open).
- Human response: "1 (developer has no objection)" (via `/clarify D-04: 1`, 2026-11-09)
- Resolution: option 1 confirmed. The patch versions stay (Spring Boot 3.5.16, postgresql 42.7.14); the vitest entries are superseded by D-12 (4.1.11).
- Overrides: tech-stack.md dependencies `spring-boot-starter-*` (3.5.6 -> 3.5.16), `org.postgresql:postgresql` (42.7.8 -> 42.7.14), `vitest` and `@vitest/coverage-v8` (3.2.4 -> 3.2.7)

## D-05: react-router 7.9.3 has High findings; the fix is another minor line
- Timestamp: 2026-11-09
- Session: S2 preflight
- Class: block
- Type: blocking
- Trigger: `react-router@7.9.3` has 13 advisories, 7 High (for example GHSA-2w69-qvjg-hvjx XSS via open redirects, GHSA-chx6-hx7r-mcp5 unauthenticated DoS in route matching); `npm audit --omit=dev` reports it as High. No 7.9.x patch fixes them (7.9.6 still has them); the fixes start at 7.18.0 and `npm audit` proposes 7.18.5. A different minor is blocking.
- Options: 1. (proposed) Replace with `react-router` 7.18.5 (checked again in the next preflight). 2. Another exact version you name. 3. Keep 7.9.3 with written evidence (a client-only SPA does not use the SSR, RSC or `__manifest` paths); lowering a High is a blocking decision and does not cover the open-redirect and XSS findings.
- Human response: "1" (via `/preflight D-05: 1`, 2026-11-09)
- Resolution: option 1. `react-router` 7.18.5 replaces 7.9.3; it resolves and `npm audit` (all and `--omit=dev`) reports no finding for it (`out/logs/preflight-npm-audit-*-2.json`).
- Overrides: tech-stack.md dependency `react-router` (7.9.3 -> 7.18.5)

## D-06: vite 7.1.7 has High findings; the fix is another minor line
- Timestamp: 2026-11-09
- Session: S2 preflight
- Class: block
- Type: blocking
- Trigger: `vite@7.1.7` has 11 advisories, 3 High (GHSA-p9ff-h696-f583 arbitrary file read via dev-server WebSocket, GHSA-v2wj-q39q-566r `server.fs.deny` bypass, GHSA-587h-7wmj-h5pm). The 7.1.x line has no fix (7.1.12 still affected); fixed in 7.3.x (`npm audit`: 7.3.8). The affected code is the dev server, not the built app.
- Options: 1. (proposed) Replace with `vite` 7.3.8; `@tailwindcss/vite` 4.1.13 and `@vitejs/plugin-react` 5.0.4 stay (compatibility is checked in `/implement`). 2. Another exact version you name. 3. Keep 7.1.7 with written evidence that the dev server is bound to localhost only (blocking decision to lower).
- Human response: "1" (via `/preflight D-06: 1`, 2026-11-09)
- Resolution: option 1. `vite` 7.3.8 replaces 7.1.7 (dependency and tooling); it resolves and `npm audit` reports no finding for it. `@tailwindcss/vite` 4.1.13 and `@vitejs/plugin-react` 5.0.4 stay; peer compatibility is checked in `/implement`.
- Overrides: tech-stack.md dependency and tooling `vite` (7.1.7 -> 7.3.8)

## D-07: @stryker-mutator/core 9.1.1 has High findings; the fix is another minor line
- Timestamp: 2026-11-09
- Session: S2 preflight
- Class: block
- Type: blocking
- Trigger: `@stryker-mutator/core@9.1.1` is reported High by `npm audit` (via `minimatch`, `ajv`, `mutation-server-protocol`); the fix target is 9.6.1 (non-major, but another minor). The tooling note also names `@stryker-mutator/vitest-runner` 9.1.1, which is not a `dependencies` entry.
- Options: 1. (proposed) Replace `@stryker-mutator/core` and `@stryker-mutator/vitest-runner` with 9.6.1. 2. Keep 9.1.1 (dev-only mutation tool, no runtime exposure), with written evidence; blocking decision to lower.
- Human response: "1" (via `/preflight D-07: 1`, 2026-11-09)
- Resolution: option 1. `@stryker-mutator/core` and `@stryker-mutator/vitest-runner` 9.6.1 replace 9.1.1; both resolve and `npm audit` reports no finding for them.
- Overrides: tech-stack.md tooling `@stryker-mutator/core` and its option text (9.1.1 -> 9.6.1)

## D-08: firebase 12.3.0 has a High finding in the unused Firestore client
- Timestamp: 2026-11-09
- Session: S2 preflight
- Class: block
- Type: blocking
- Trigger: `npm audit --omit=dev` reports `firebase@12.3.0` High through `@firebase/firestore` -> `@grpc/grpc-js` (GHSA-m9gg-hp2v-232j, High, CVSS 7.4: unauthorised certificates accepted in certain configurations). `npm audit`'s suggested fix (`firebase@9.14.0`) is an older major and not usable. The project uses only Firebase Authentication (`tech-stack.md`); the Firestore code is installed but not imported.
- Options: 1. (proposed) Keep `firebase` 12.3.0 and record the finding as Medium on written evidence: Firestore is never imported, gRPC runs server-side or on Node only, and the browser build tree-shakes it (checked again by `/verify` on the built bundle). 2. Move to a newer 12.x release that resolves it, if one exists (the agent brings the exact version to the next preflight). 3. Keep as High (blocks release).
- Human response: "2 - newest 12.x release that resolves the finding; if none exists use option 1 (keep 12.3.0, Medium, Firestore never imported, rechecked by /verify)" (via `/preflight D-08`, 2026-11-09)
- Resolution: no 12.x release resolves it. `firebase` 12.19.0 (newest 12.x; npm lists 12.3.0 to 12.19.0) was scanned in the scratch lockfile and still reports `@firebase/firestore` -> `@grpc/grpc-js` High (GHSA-m9gg-hp2v-232j); the only `npm audit` fix is 9.14.0, an older major. The human's stated fallback applies: keep `firebase` 12.3.0, record the finding as **Medium** on the evidence in the trigger (Firestore never imported; the browser build tree-shakes it). `/verify` re-checks it on the built bundle (the production bundle must contain no `@firebase/firestore` or `@grpc/grpc-js` code).
- Overrides: none (severity lowering of one High finding, approved by the human's fallback instruction)

## D-09: firebase-tools 14.19.1 has Critical and High findings and does not support Node 24
- Timestamp: 2026-11-09
- Session: S2 preflight
- Class: block
- Type: blocking
- Trigger: `npm audit` reports `firebase-tools@14.19.1` High with transitive Critical `tar` and High `minimatch`, `zod`, `basic-ftp` and others (`out/logs/preflight-npm-audit-all.json`); the fix is `firebase-tools@15.35.0` (another major). `superstatic@9.2.0`, a dependency, declares `node 18 || 20 || 22` and warns on the host's Node 24.13.0 (D-01 option 2). The package is test-only (Authentication Emulator on localhost, no deploy).
- Options: 1. (proposed) Replace with `firebase-tools` 15.35.0; the agent checks its Node range and findings in the next preflight. 2. Run the emulator in a `node:22` container so the Node range holds, and keep 14.19.1 as test-only with written evidence (lowering Critical/High, blocking). 3. Another exact version you name.
- Human response: "1" (via `/preflight D-09: 1`, 2026-11-09)
- Resolution: option 1. `firebase-tools` 15.35.0 replaces 14.19.1 (dependency and tooling). It resolves, declares `engines.node >=20 || >=22 || >=24` (so Node 24.13.0 is supported) and `npm audit` reports no High or Critical finding for it or its dependencies.
- Overrides: tech-stack.md dependency and tooling `firebase-tools` (14.19.1 -> 15.35.0)

## D-10: Maven dependency scan cannot run on the host
- Timestamp: 2026-11-09
- Session: S2 preflight
- Class: block
- Type: blocking
- Trigger: `mvn` is not on the host and no `pom.xml` or Maven wrapper exists, so `org.owasp:dependency-check-maven` (the listed dependency-scan tool for the backend) cannot run; the agent must not install software. Only the direct pins were checked, with OSV (all resolved; findings in D-04); transitive Maven dependencies are not scanned. The phase 0 gate needs a complete scan.
- Options: 1. (proposed) Allow running the scan inside a `maven:3.9.11-eclipse-temurin-21` container (docker only, nothing installed on the host; adds one image as dev-only tooling, non-blocking record at the time). 2. You install Maven 3.9.11 on the host, then re-run `/preflight`. 3. Accept the OSV direct-pin scan as the gate evidence and run the full OWASP scan in the quality gate (weaker; blocking decision).
- Human response: "1" (via `/preflight D-10: 1`, 2026-11-09)
- Resolution: option 1 applied, scan not complete. `maven:3.9.11-eclipse-temurin-21` (dev-only tooling, container, nothing installed on the host) ran `org.owasp:dependency-check-maven:12.1.6:check` on a scratch pom of the 16 backend pins (D-04 versions) in `out/logs/preflight-scan-maven/`. The NVD update ended with `NvdApiException: status 524` and a database error ("Value too long for column URL", CVE-2026-6785 and -6786), so the analyser produced no report. Second method: `mvn dependency:list` (130 resolved artifacts, `deps.txt`) queried against OSV (`osv.mjs`). Result: transitive Critical and High findings, see D-13.
- Overrides: none

## D-11: Playwright browsers must be downloaded to the host
- Timestamp: 2026-11-09
- Session: S2 preflight
- Class: block
- Type: blocking
- Trigger: Preflight step 2 needs Playwright to start its browser and save `out/logs/preflight-screenshot.png`; `@playwright/test@1.55.1` resolves, but its Chromium, Firefox and WebKit downloads (`npx playwright install`) go to the host's user profile (`AGENTS.md`: no installs on the host without approval). Not run.
- Options: 1. (proposed) Approve `npx playwright install chromium firefox webkit` for the run (the pinned 1.55.1 build). 2. You run the command yourself. 3. Run Playwright in the pinned-version Microsoft container (adds an image; separate decision).
- Human response: "1" (via `/preflight D-11: 1`, 2026-11-09)
- Resolution: option 1. `npx playwright install chromium firefox webkit` ran with Playwright 1.55.1 (Chromium 140.0.7339.186, Firefox 141.0, WebKit 26.0). Each browser started and saved a blank-page screenshot: `out/logs/preflight-screenshot.png` (Chromium), `preflight-screenshot-firefox.png`, `preflight-screenshot-webkit.png`. Check passed.
- Overrides: none

## D-12: vitest 3.2.7 still has a Critical finding through tinypool
- Timestamp: 2026-11-09
- Session: S1 preflight (`/preflight D-05 to D-11`)
- Class: block
- Type: blocking
- Trigger: the scratch lockfile with the D-04 to D-09 versions (`out/logs/preflight-scan2/`, `preflight-npm-audit-all-2.json`) reports `vitest` 3.2.7 and `@vitest/coverage-v8` 3.2.7 as Critical through `tinypool` 1.1.1 (GHSA-5gmw-xhrv-c9v3, GHSA-85c8-ppgw-ccpr: prototype pollution to remote code execution through worker options; CVSS not scored) and Moderate GHSA-82fw-gwwq-j7x9. D-04 had counted 3.2.7 as clean. The tinypool fix is 2.1.2 (a major); `npm audit` offers `vitest` 5.1.0 (two majors up, Node `^22.12 || ^24 || >=26`, fits Node 24.13.0). The affected code runs only in test workers on a developer or CI machine, never in the shipped app.
- Options: 1. (proposed) Replace `vitest` and `@vitest/coverage-v8` with 5.1.0 (the agent checks resolution and findings in the next preflight; compatibility with `@stryker-mutator/vitest-runner` 9.6.1 and `@vitejs/plugin-react` 5.0.4 is checked in `/implement`). 2. Another exact version you name (for example a 4.x with tinypool 2.1.2 or newer). 3. Keep 3.2.7 as a test-only tool with written evidence (lowering a Critical, approval needed).
- Human response: "2 - vitest and @vitest/coverage-v8 4.1.11, and @stryker-mutator/core and @stryker-mutator/vitest-runner 10.0.0 (replaces D-07); vitest 5.x does not work with the Stryker vitest runner, 4.1.11 with Stryker 10.0.0 is a known working pair" (via `/preflight D-12: 2 ...`, 2026-11-09)
- Resolution: option 2 applied. `vitest` and `@vitest/coverage-v8` 4.1.11 and `@stryker-mutator/core` and `@stryker-mutator/vitest-runner` 10.0.0 resolve (scratch lockfile, `out/logs/preflight-scan2/`). `npm audit` (`preflight-scan2`, all and `--omit=dev`): 0 Critical; tinypool no longer in the tree (the Critical is gone); vitest, `@vitest/*` and Stryker only Moderate (dev). Remaining High: the firebase chain, recorded as Medium in D-08. This replaces D-07 (Stryker 9.6.1) and D-04 for vitest.
- Overrides: tech-stack.md dependency and tooling `vitest`, `@vitest/coverage-v8` (3.2.4 -> 4.1.11), `@stryker-mutator/core` and `@stryker-mutator/vitest-runner` (9.1.1 -> 10.0.0)

## D-13: Transitive Maven dependencies have Critical and High findings
- Timestamp: 2026-11-09
- Session: S1 preflight (`/preflight D-05 to D-11`)
- Class: block
- Type: blocking
- Trigger: OSV query of the 130 artifacts that the 16 backend pins (D-04 versions) resolve to (`out/logs/preflight-scan-maven/deps.txt`; OWASP failed, see D-10). Six artifacts have findings; the three below are Critical or High and open:
  - `org.apache.tomcat.embed:tomcat-embed-core` 10.1.55 (from `spring-boot-starter-web`): 5 Critical (GHSA-9xv2-5v5q-p794, -f525-44xv-f2qj, -gcx9-497g-6cp6, -h3x4-894j-xpx5, -mqg3-r7h5-24x4) and 1 High (GHSA-h792-v28v-ppgr); fixed in 10.1.59 (same minor line).
  - `org.springframework:spring-webmvc` 6.2.19: 2 Critical (GHSA-j9f9-w8pj-32f8 SSE stream corruption, GHSA-pc63-qcmh-9cmg XsltView path limitation); OSV lists fixes only in 7.0.9, no 6.2.x fix. The app uses neither server-sent events nor `XsltView`.
  - `com.fasterxml.jackson.core:jackson-databind` 2.19.1 (6 High) and `jackson-core` 2.19.1 (4 High): fixed in 2.21.7 or 2.18.11; the 2.19 line has no fix.
  - Moderate only: `commons-lang3` 3.16.0, `log4j-api` 2.24.3.
  Overriding a Boot-managed transitive version is a change outside `tech-stack.md`.
- Options: 1. (proposed) Pin in the backend pom, as dependency overrides (Spring Boot BOM properties): Tomcat 10.1.59, Jackson 2.21.7 (databind and core); for Spring Framework keep 6.2.19 and record GHSA-j9f9-w8pj-32f8 and GHSA-pc63-qcmh-9cmg as not reachable (no SSE fragment rendering, no `XsltView`; checked in `/verify`, needs your approval to lower a Critical); the agent re-scans in the next preflight. 2. Move Spring Boot to a newer 3.5.x or 4.x that brings fixed versions (blocking; you name the version). 3. Keep everything and run the full OWASP scan in the quality gate (weaker; the gate cannot pass with open Critical).
- Human response: "1 - override Tomcat 10.1.59 and Jackson 2.21.7; keep Spring Framework 6.2.19 with the two Critical findings recorded as not reachable (no server-sent events, no XsltView), rechecked by /verify; if a 6.2.x release that fixes them exists, use it instead" (via `/preflight D-13: 1 ...`, 2026-11-09)
- Resolution: option 1 applied. No 6.2.x release after 6.2.19 exists on Maven Central (latest `spring-webmvc` 6.2.19), so Spring stays at 6.2.19. Scratch pom (`out/logs/preflight-scan-maven/pom.xml`, Boot parent 3.5.16 with `tomcat.version` 10.1.59 and `jackson-bom.version` 2.21.7) resolves 130 artifacts (`deps2.txt`) with Tomcat 10.1.59 and Jackson databind and core 2.21.7. OSV re-scan: no finding left on Tomcat or Jackson. Open: `spring-webmvc` 6.2.19 GHSA-j9f9-w8pj-32f8 and GHSA-pc63-qcmh-9cmg, Critical, recorded as not reachable on the human's approval (no SSE, no `XsltView`; `/verify` re-checks, and `/implement` must not add either). Moderate, not blocking: `commons-lang3` 3.17.0, `log4j-api` 2.24.3. The product pom must set `tomcat.version` 10.1.59 and `jackson-bom.version` 2.21.7.
- Overrides: tech-stack.md dependencies (adds exact-version transitive overrides: tomcat-embed-core 10.1.59, jackson-databind and jackson-core 2.21.7)

## D-14: Customer confirmations still outstanding
- Timestamp: 2026-11-09
- Session: S2 clarify (`/clarify D-04: 1`)
- Class: open
- Type: non-blocking
- Trigger: many story questions read "OPEN — confirmed by the developer; the customer still has to confirm" (OQ-P-03, OQ-P-09, OQ-002-01, OQ-002-02, OQ-003-01, OQ-004-01 (D-15), OQ-004-02, OQ-016-01 (D-15), OQ-005-01, OQ-006-01, OQ-010-01, OQ-011-01, OQ-012-01, OQ-013-01, OQ-015-01, OQ-020-01, OQ-022-01). Others are "needs customer" and due before delivery (OQ-001-02, OQ-005-03, OQ-012-03, OQ-017-01, OQ-P-04 to OQ-P-08, OQ-P-10, OQ-P-11), and the numbers of `quality-requirements.md` and the ASVS level of `security-requirements.md` are developer proposals. `OQ-P-03` and the stories name the developer's answer as the working reading; `security-requirements.md` (Personal data) leaves legal matters to the customer outside the software.
- Options: 1. (proposed) Build on the developer-confirmed readings; carry this list into `docs/06_verification-report.md` and `release-notes.md` as "to be confirmed by the customer before delivery"; design uses a neutral working name until OQ-P-08 is answered. 2. Wait for the customer before `/plan` (blocks the run).
- Human response: none
- Resolution: option 1, pending review
- Overrides: none

## D-15: Two Block topics still marked "needs customer"
- Timestamp: 2026-11-09
- Session: S2 clarify
- Class: block
- Type: blocking
- Trigger: `OQ-004-01` (US-004: roles and member rules, rights) and `OQ-016-01` (US-016: keeping the user's name in the History after account deletion, personal data and legal basis) read "OPEN — needs customer", while every other Block question in the stories is "confirmed by the developer". DoR-S-07 allows no OPEN item on a Block topic. The permission matrix (BR-02) and R-016-03 already state one reading each.
- Options: 1. (proposed) You confirm both as written (the matrix and member rules of BR-02 and US-004; name kept and e-mail removed, R-016-03), like the other Block topics; the customer's confirmation goes on the D-14 list. 2. You name a change (which rule, which new reading). 3. Hold US-004 and US-016 until the customer answers.
- Human response: "1 - confirmed by the developer like the other Block questions; add both to the D-14 list for the customer to confirm" (via `/clarify D-15: 1 ...`, 2026-11-09)
- Resolution: option 1. OQ-004-01 (matrix and member rules of BR-02 and US-004) and OQ-016-01 (name kept, e-mail removed, R-016-03) are confirmed by the developer as written; both are added to the D-14 list "to be confirmed by the customer before delivery". US-004 and US-016 no longer fail DoR-S-07.
- Overrides: none

## D-16: What group-level History entries hold about other people
- Timestamp: 2026-11-09
- Session: S2 clarify
- Class: block
- Type: blocking
- Trigger: R-013-05 writes a group-level entry for an invitation, a cancellation, a role change and a removal, and R-013-07 names only the actor's name and e-mail. No rule says what the entry holds about the other person (the invited e-mail address; the name and e-mail of the member whose role changed or who was removed). Three inputs pull apart: (a) `security-requirements.md` (Personal data) keeps the invitation e-mail only until accepted, cancelled or expired, and shows it to Owners only, while the group-wide History (R-014-01) is shown to Editors too and kept as long as the group exists; (b) R-016-03 anonymises only entries the deleted user caused, so an entry "Ana removed Marko" or "Ana invited marko@example.com" keeps Marko's e-mail after his account is deleted; (c) BR-06 anonymises contact data only. Personal data of people who are not contacts would stay in the History after they ask for deletion.
- Options: 1. (proposed) The entry holds the event type, the actor (name, e-mail) and the subject's role; the subject is named by name for a member and by e-mail address for an invitation; when an account is deleted, its e-mail is removed from every entry where it is the actor or the subject (the name stays), and the invited e-mail stays in the entry for the life of the group (a documented exception to the retention table, visible to Owners and Editors). 2. As option 1, but entries about an invitation hold only the role and the event, never the invited e-mail (Owners see the e-mail only in the pending list). 3. Entries never name the subject; only actor, event type and role (weakest audit).
- Human response: "2 - invitation entries hold only the event and the role, never the invited e-mail; members are named by name; when an account is deleted its e-mail is removed from every entry where it is the actor or the subject and the name stays" (via `/clarify D-16: 2 ...`, 2026-11-09)
- Resolution: option 2. A group-level entry holds the event type, the actor (name, e-mail) and the role. An invitation entry never holds the invited e-mail (Owners see it only in the pending list, so the retention table holds). A member who is the subject (role change, removal) is named by name only. When an account is deleted, its e-mail is removed from every entry where it is the actor or the subject; the name stays. No documented exception to the retention table is needed.
- Overrides: none

## D-17: What an Owner may see and do in a group in Trash
- Timestamp: 2026-11-09
- Session: S2 clarify
- Class: block
- Type: blocking
- Trigger: BR-01 and R-015-03 treat a group in Trash as not existing for members who are not Owners; R-015-04 and R-015-05 give Owners "Deleted groups" and restore; R-015-08 refuses changes. No rule says what an Owner's other requests get (open the group, list contacts, read History, export). SR-03 and EX-017-06 define the 404 only for non-Owners. Reading it wrongly gives access to personal data of a group that was deleted, or breaks the restore screen.
- Options: 1. (proposed) An Owner of a group in Trash can see only its name, deletion time and days left in "Deleted groups" and can restore it; every other request about the group (contacts, History, members, export, invitations) answers 404 until it is restored. 2. An Owner can open the group read-only (contacts, History, members), no export, until restore or purge. 3. Another rule you name.
- Human response: "1" (via `/clarify D-17: 1 ...`, 2026-11-09)
- Resolution: option 1. An Owner of a group in Trash sees only its name, deletion time and days left in "Deleted groups" and can restore it; every other request about the group (contacts, History, members, export, invitations) answers 404 until it is restored.
- Overrides: none

## D-18: US-007 is in no end-to-end scenario (DoR-R-04)
- Timestamp: 2026-11-09
- Session: S2 clarify
- Class: block
- Type: blocking
- Trigger: DoR-R-04 needs every MVP story in at least one scenario. `e2e-scenarios.md` leaves out US-007 (rename and delete tags); US-018 is left out by the file's own note (language switch covered by acceptance tests, Q-35h). The file is an input and fixes "exactly one test per scenario, none merged or split", so only you can change it.
- Options: 1. (proposed) Approve US-007 as verified by its acceptance tests (AC-007-01 to AC-007-08), like US-018; the file stays unchanged and this is recorded as an approved exception to DoR-R-04 for US-007 and US-018. 2. You add US-007 to a scenario (for example the tag rename to E2E-03) in `01_input/`, then start `/clarify` again.
- Human response: "1 - approved exception for US-007 and US-018, verified by their acceptance tests" (via `/clarify D-18: 1 ...`, 2026-11-09)
- Resolution: option 1. US-007 and US-018 are approved as verified by their acceptance tests (AC-007-01 to AC-007-08, AC-018-01 to AC-018-07); `e2e-scenarios.md` stays unchanged. Approved exception to DoR-R-04 for these two stories.
- Overrides: DoR-R-04 (US-007, US-018; approved by the human)

## D-19: A stale save after a group-wide tag rename or delete
- Timestamp: 2026-11-09
- Session: S2 clarify
- Class: open
- Type: non-blocking
- Trigger: R-005-08 refuses a save when the contact "was changed by someone else"; the contact number is its version number (US-005 Definitions). A tag rename or delete changes the contact's tags but records no version (R-007-06, R-019-03). A user who opened a contact before the rename and saves afterwards would put the old tag name back on that contact (R-006-01 creates the tag again) and no conflict would show. PP-02: nothing is silently overwritten.
- Options: 1. (proposed, most conservative) A group-wide tag rename or delete counts as a change of the contact for the conflict check (a stale save is refused with the conflict rule), although it records no version; the plan chooses the mechanism (for example a separate change counter). 2. Version number only; the old tag name comes back on that contact.
- Human response: none
- Resolution: option 1, pending review
- Overrides: none

## D-20: Format and boundary readings (e-mail, Google name, trashed group rename, invitation matching)
- Timestamp: 2026-11-09
- Session: S2 clarify
- Class: open
- Type: non-blocking
- Trigger: (1) "valid e-mail format" (R-003-02, R-005-03, data model) is defined nowhere, and an invitation e-mail has no length limit (the AC examples are only `ana.novak@` and `marko@@example`). (2) R-001-02: a Google account may have no name or one longer than 100 characters, while User.name is required, 1 to 100. (3) R-002-08 and R-015-08 do not say whether an Owner may rename a group that is in Trash. (4) R-003-05 matches an invitation "when a user signs in"; the backend sees only requests with a token, and a user who is already signed in when invited sees the group at some request.
- Options: 1. (proposed, most conservative) (1) valid = one `@`, a non-empty local part, a domain with at least one "." and no empty label, no spaces, at most 255 characters (also for invitations); (2) Google name: use the part of the e-mail before `@` if the name is empty, cut to 100 characters if longer; (3) refuse the rename of a group in Trash; (4) match pending invitations whenever the user requests the groups list (this includes the request after sign-in), only with a verified e-mail. 2. You name other readings.
- Human response: none
- Resolution: option 1, pending review
- Overrides: none

## D-21: Export file details
- Timestamp: 2026-11-09
- Session: S2 clarify
- Class: open
- Type: non-blocking
- Trigger: R-011-05 and R-011-06 leave open: (1) which characters are "not allowed in file names"; (2) the header spelling of an attribute column when keys differ only in case (EX-011-03); (3) the order of tags in the Tags cell; (4) an attribute key equal to a predefined header (for example `Tags`). AC-011-03 shows `a, b` (comma and space), which settles the separator.
- Options: 1. (proposed) (1) backslash, slash, colon, asterisk, question mark, double quote, less-than, greater-than, pipe and control characters; (2) the spelling of the first contact in the export order; (3) tag names sorted by Slovenian rules; (4) the attribute column keeps its own header and is a separate column after the predefined ones. 2. You name other readings.
- Human response: none
- Resolution: option 1, pending review
- Overrides: none

## D-22: History and version readings
- Timestamp: 2026-11-09
- Session: S2 clarify
- Class: open
- Type: non-blocking
- Trigger: (1) R-022-05 and R-019-04 record one version on the survivor, while EX-022-08 and R-019-02 say no version when nothing on the survivor changed. (2) R-013-05 lists the group-level events and leaves out "a member joined by invitation (R-003-05)" and "a group was created". (3) US-002 (R-002-08), US-007 (R-007-08), US-012 (R-012-09) and US-015 (R-015-09) require History entries, but US-013, which defines History, depends on them (INVEST: independent).
- Options: 1. (proposed) (1) no version when the survivor's content is unchanged (EX-022-08 and R-019-02 win); (2) the list of R-013-05 is complete: no entry for joining or creating a group; (3) `/plan` makes the History append capability the first task before the first story that writes an entry, each of those stories verifies its own History criteria with it, and US-013 covers the History tab, the content rules and the group-wide events. 2. You name other readings.
- Human response: none
- Resolution: option 1, pending review
- Overrides: none

## D-23: Glossary homonyms
- Timestamp: 2026-11-09
- Session: S2 clarify
- Class: open
- Type: non-blocking
- Trigger: US-013 defines "Save group" (the entries of one save, shown together), which uses the glossary word `group` with another meaning; US-005 defines "Contact number" (the current version number), which reads like a phone number. A new domain term goes through a decision (`requirements-standard.md`).
- Options: 1. (proposed) Code and UI use `ChangeSet` for "Save group" and `contact version number` for "Contact number"; the inputs stay unchanged. 2. Keep the story terms.
- Human response: none
- Resolution: option 1, pending review
- Overrides: none

## D-24: Dependencies and images the skeleton needs beyond tech-stack.md
- Timestamp: 2026-11-09
- Session: S3 plan
- Class: allow
- Type: non-blocking
- Trigger: `tech-stack.md` Rules allow a dependency or tool not listed only with an exact version and a non-blocking record. The skeletons build and test only with these additions, all MIT, Apache-2.0, BSD, ISC or EPL/MPL in test scope, all resolved by the real builds (`out/logs/`):
  - Backend, test scope: `com.tngtech.archunit:archunit-junit5` 1.4.1 (architecture tests for AR-01 to AR-05, AR-11; Apache-2.0); `org.springframework.security:spring-security-test` 6.5.11 (the version Spring Boot 3.5.16 manages; JWT test support; Apache-2.0). Planned for phase 5 and not added yet: `org.pitest:pitest-junit5-plugin` (needed by the listed `pitest-maven`).
  - Frontend, dev: `@eslint/js` 9.36.0, `globals` 17.14.0, `eslint-plugin-react-hooks` 7.1.1 (lint rule sets for the listed ESLint); `jsdom` 27.4.0, `@testing-library/dom` 10.4.2, `@testing-library/jest-dom` 7.0.1, `@testing-library/user-event` 14.6.8 (component tests for the listed Vitest and Testing Library); `@types/react` 19.3.0, `@types/react-dom` 19.3.0, `@types/node` 24.13.6 (type check); `openapi-typescript` 7.13.0 (client types from the contract, AR-13); `@apidevtools/swagger-parser` 13.1.0, `ajv` 8.20.0, `ajv-formats` 3.0.1 (contract validation, phase 2 gate).
  - Frontend `overrides`: `playwright-core` 1.55.1, because `@axe-core/playwright` 4.10.2 otherwise pulls `playwright-core` 1.65.0 beside the pinned Playwright 1.55.1 and the two `Page` types clash.
  - Container images used only as builders, not shipped in a runtime layer: `node:24.13.0-slim` (frontend build stage, the Node version of D-01 option 2) and `maven:3.9.11-eclipse-temurin-21` (backend build stage and the way to run Maven when the host has none, D-10). Also `firebase-tools` 15.35.0 (D-09) is installed in a small image for the Authentication Emulator.
  - `npm audit` after the additions shows no new High or Critical advisory; the remaining High is the Firebase chain of D-08.
- Options: 1. (proposed, applied) Add these with exact versions. 2. Refuse some; the skeleton then cannot lint, type-check or test.
- Human response: none
- Resolution: option 1, pending review
- Overrides: none (additions only)

## D-25: Planning readings
- Timestamp: 2026-11-09
- Session: S3 plan
- Class: allow
- Type: non-blocking
- Trigger: points `/plan` had to settle that no input settles.
  1. The conflict counter of D-19 is the column `contacts.revision` and the API field `revision`; the version number of US-019 is `versionNumber` (ADR-03). AR-14 says "carries the contact's current version number"; the counter satisfies it and also covers tag renames.
  2. A save whose revision differs always conflicts, even when its values would change nothing (ADR-03).
  3. US-022 (estimate 8) is split into two `/implement` sessions (OQ-022-03, F-15): the merge screen and operation, and the History linking (ADR-11).
  4. A group-level History entry about a member holds the member's name only; no e-mail is ever stored for a subject, so the e-mail removal of D-16 applies to actors only (ADR-05).
  5. The Maven wrapper (`./mvnw`, tech-stack.md) is the delivered build command. In this run it could not be started on the host (a permission prompt), so every backend command was run with the same pinned image, `maven:3.9.11-eclipse-temurin-21`, with the Docker socket for Testcontainers; the wrapper files were generated by that image.
  6. `.npmrc` could not be written (protected file), so exact versions rely on the pinned `package.json` and `package-lock.json`; `npm install --save-exact` is documented in the component README.
- Options: 1. (proposed, applied) These readings. 2. You name others.
- Human response: none
- Resolution: option 1, pending review
- Overrides: none

## D-26: Approve wireframes
- Timestamp: 2026-11-09
- Session: W wireframes
- Class: block
- Type: approval
- Trigger: 19 wireframes (`docs/wireframes/WF-01` to `WF-19`; each has `_mobile.png`, `_tablet.png`, `_desktop.png` beside its `.html`, shared `wireframe.css`) cover every screen of specification 9.3 at the three widths (UI-22). Choices inside them: no global search in the header (no story needs it); tablet puts the navigation behind the Menu button and hides phone, city and country in the contacts list.
- Options: 1. (proposed) Approved with the customer. 2. Approved by the developer only. 3. Change (say what, per `WF-nn`).
- Human response: none
- Resolution: pending review
- Overrides: none
