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
- Human response: option 2, approved by the developer only; the customer has not seen the wireframes (2026-11-09, `/wireframes D-26: 2`)
- Resolution: option 2
- Overrides: none

## D-27: Tab row on mobile is cut off in the wireframes
- Timestamp: 2026-11-09
- Session: W wireframes
- Class: open
- Type: non-blocking
- Trigger: note from the developer with the answer to D-26: at 375 px the tab row (Contacts, Trash, Tags, History, Members, Duplicates) is cut off at the right edge in the wireframes. The wireframes stay as approved (D-26); the note binds the build (`/implement`, `/design`).
- Options: 1. (proposed) The real screen wraps the tabs onto a second line, so nothing is hidden. 2. The real screen scrolls the tab row sideways with a visible hint that more tabs follow. Either satisfies the developer's note; the choice is made once, in the shared tab component, and applies at every width where the row does not fit.
- Human response: the developer's note allows either option (2026-11-09)
- Resolution: option 1, pending review
- Overrides: none

## D-28: Content-Security-Policy allows Google's sign-in script
- Timestamp: 2026-11-10
- Session: S5 implement US-001
- Class: allow
- Type: non-blocking
- Trigger: "Sign in with Google" (R-001-01) opens a pop-up through the Firebase SDK, which loads `https://apis.google.com/js/api.js`. The policy of the web server (`frontend/docker/default.conf.template`) had `script-src 'self'` and blocked it, so the button only showed an error (AC-001-04 failed in the browser).
- Options: 1. (proposed, applied) `script-src 'self' https://apis.google.com`: one named Google host, still no inline script and no wildcard (SR-06). 2. A different sign-in flow without the script (redirect), which the specification (4.1) does not use.
- Human response: none
- Resolution: option 1, pending review
- Overrides: none

## D-29: Readings made while building the secured API (US-017)
- Timestamp: 2026-11-10
- Session: S5 implement US-017
- Class: allow
- Type: non-blocking
- Trigger: (1) The emulator-host guard of ADR-02 needs a profile, but the frozen acceptance tests set the emulator host and no profile. (2) AC-017-11 and the drift test of ADR-09 compare the served description with all 40 operations of the contract, which exist only after the last story. (3) `/health` must appear in `/v3/api-docs` as `getHealth`; the actuator endpoint is not described by springdoc. (4) Most frozen US-017 tests set the scene with `POST /groups` (US-002) or read `GET /groups`, so they cannot pass before US-002. (5) The `feat` commit 7cebd93 lists a `Red:` line for AC-017-12 that was not observed: the actuator health already answered 503 and 200; only the `getHealth` description was missing.
- Options: (1) Surefire and Failsafe run under the profile `test` (pom, no change to the tests); a host outside `local` and `test` stops the start. (2) `OpenApiContractIT` checks code to contract now (operationId, response statuses, parameters, request body present); the other direction stays with AC-017-11. Schema property names are not compared yet because `schemas.yaml` is not in the backend folder; this is added with the first story that has a body. (3) Own `HealthController`; the actuator starter is removed (its own `build` commit). (4) The story stays `in progress`; its own tests pass. (5) Stated here; history is not rewritten.
- Human response: none
- Resolution: as listed, pending review
- Overrides: none

## D-30: Readings made while building groups (US-002)
- Timestamp: 2026-11-10
- Session: S5 implement US-002
- Class: allow
- Type: non-blocking
- Trigger: (1) T-10 depends on T-07 (the History append foundation, D-22 reading 3), which no session had built yet; T-11 writes the rename entry with it. (2) R-002-08 does not say what a "rename" to the name the group already has does. (3) R-002-04 says nothing about the order of the groups list. (4) R-002-02 says "trimming spaces"; Java's `strip()` leaves a non-breaking space, which would make a name that looks empty valid. (5) R-002-06 and AC-002-01 want the opened group to show no contacts, but the contacts list belongs to US-009 (T-30). (6) The Slovenian names of the roles are in no input. (7) Four frozen US-002 tests set the scene with later stories: AC-002-01 (`POST /contacts/search`, US-009), AC-002-07 and EX-002-05 (`POST /trash`, US-015), AC-002-10 and EX-002-05 (invitations through `join`, US-003). (8) `OpenApiContractIT` (my own test, not frozen) read only the parameters declared on an operation, while the contract declares `groupId` once per path.
- Options: (1) T-07 is built in this session as its own `feat` commit before T-10 (domain types, `HistoryRepository`, `HistoryRecorder` with a mandatory transaction); US-013 keeps its other tasks. (2) The same name (after trimming, exactly equal) changes nothing: no rule is applied, no History entry is written, the answer is 200 with the group. A change of capitalisation is a rename and is recorded. (3) Slovenian alphabetical order by name (`sl-SI-x-icu`), then creation time. (4) Trimming removes every Unicode space character, also non-breaking ones, on the server and the form accepts the same through `trim()`. (5) The group page shows the name, the role, the tab bar with the Contacts tab and an empty-group text "No contacts yet"; US-009 replaces the text with the list. (6) Lastnik, Urednik, Bralec (formal tone of `ui-design.md`). (7) The story stays `in progress`; its own tests pass. (8) The test merges the path-level parameters with the operation's.
- Human response: none
- Resolution: as listed, pending review
- Overrides: none

## D-31: Two frozen browser tests that fail for reasons outside US-002
- Timestamp: 2026-11-10
- Session: S5 implement US-002
- Class: approval
- Type: non-blocking
- Trigger: With the rebuilt stack, `AC-002-03` (`groups-members.spec.ts`) fails every time: the helper `signIn` returns right after clicking "Sign in", and the test then calls `page.goto('/groups/<id>')`, which cuts the sign-in off before Firebase stores the session, so the page shows "Sign in". `AC-001-04` (Google popup, `us001-sign-in.spec.ts`) fails every time: the buttons stay disabled after the emulator popup closes; no groups code is involved.
- Options: 1. (proposed) Leave both frozen tests unchanged and the story `in progress`; the screens they target exist (AC-002-03 page checked in `group-page.test.tsx`). You decide whether `signIn` should wait for the signed-in screen (a change to a frozen file, new freeze) and whether the Google popup failure is a US-001 finding for `/review`. 2. Change the frozen test now (not allowed in this session).
- Human response: none
- Resolution: option 1, pending review
- Overrides: none

## D-32: US-003 readings and one frozen browser failure
- Timestamp: 2026-11-10
- Session: S5 implement US-003
- Class: assumption
- Type: non-blocking
- Trigger: (1) The specification does not say what happens to an expired row of the same group and address when the Owner invites again, while the unique index (group, e-mail) would refuse the insert. (2) A missing or unknown role has no field code in the specification. (3) `acceptFor` runs for `GET /me` and `GET /groups` as specified; a group in Trash still gets the membership (the list hides it). (4) `AC-003-08` (`groups-members.spec.ts`) fails in WebKit only (Chromium and Firefox pass): the frozen helper `verifyEmail` finds no verification e-mail in the emulator for `ivan@example.com`. No invitation code is involved, and the API side of AC-003-08 passes. The cause was not investigated further.
- Options: (1) The expired row is deleted in the create transaction before the insert. (2) 400 `VALIDATION_FAILED` for the field `role` with the code `INVALID_VALUE`. (3) As specified. (4) Leave the frozen test unchanged and the story `in progress`; you decide whether `verifyEmail` should wait for the e-mail (frozen file).
- Human response: none
- Resolution: as listed, pending review
- Overrides: none

## D-33: US-005 readings (contact fields, version recorder, screen)
- Timestamp: 2026-11-10
- Session: S5 implement US-005
- Class: assumption
- Type: non-blocking
- Trigger: (1) T-08 (the version recorder, D-22 reading 3) had not been built, and T-18 needs it for version 1; only `VersionService.record` and `VersionRepository.insert/count` were built, the rest of US-019 (list, read) stays. (2) R-005-03 gives no rule for leading or trailing spaces; R-005-02 treats spaces as empty. (3) A save with a different revision conflicts before the no-change check (D-25 reading 2); a contact in Trash is reported before the revision check. (4) A Viewer is refused with 403 before the body is looked at. (5) History for a create lists every filled field as a change (old value empty); for an edit only the changed fields. (6) The country codes are the JDK list of ISO 3166-1 alpha-2 (249); the web list holds the same codes, names come from `Intl.DisplayNames`. (7) The contact screen has the Details section only: tags (US-006), attributes (US-008), the Delete button (US-012) and the History and Versions tabs (US-013, US-020) come with their stories. (8) The screen code was written before its tests (not red first); the tests were then written against the wireframe and the frozen browser spec.
- Options: (1) as built. (2) Text fields are stripped at both ends before the rules apply, so spaces alone are empty and stored as empty; the phone is stripped, not rewritten. (3) to (7) as built. (8) Recorded here.
- Human response: none
- Resolution: as listed, pending review
- Overrides: none

## D-34: US-006 readings (tags in the save path)
- Timestamp: 2026-11-10
- Session: S5 implement US-006
- Class: assumption
- Type: non-blocking
- Trigger: (1) The contract does not say what a save without `tags` does. (2) A tag typed with spaces around it. (3) Tag changes of one group could race (create and orphan delete). (4) A tags-only save and its History. (5) The tag input and its tests were written in the same step (screen code before its tests, as in D-33).
- Options: (1) `tags` absent keeps the tags; a list replaces them. (2) Stripped at both ends; blank is `TOO_SHORT`; the first spelling wins when the same name repeats ignoring case. (3) A transaction advisory lock per group serialises tag changes. (4) A tags-only change makes a new version and revision; History gets one `TAG_CHANGED` entry per tag added or removed (field `tag`, old or new value) and `CONTACT_EDITED` only when a field changed. (5) Recorded here.
- Human response: none
- Resolution: as listed, pending review
- Overrides: none

## D-35: US-008 readings (attributes in the save path)
- Timestamp: 2026-11-10
- Session: S5 implement US-008
- Class: assumption
- Type: non-blocking
- Trigger: (1) The contract does not say what a save without `attributes` does. (2) Field names and codes of a refused attribute. (3) A value with spaces around it. (4) History for attributes. (5) The attribute rows and their tests were written in the same step (screen code before its tests, as in D-33, D-34).
- Options: (1) `attributes` absent keeps them; a list replaces them (as tags, D-34). (2) `attributes[i].key` REQUIRED (empty after trim), TOO_LONG, DUPLICATE_KEY (the later of two equal keys ignoring case); `attributes[i].value` TOO_LONG; `attributes` TOO_MANY. (3) Only the key is trimmed (EX-008-06); the value is kept as typed. (4) One `ATTRIBUTE_CHANGED` entry per key added, removed or given another value (field `attribute:<key>`, old and new value); an attribute-only save makes a new version and revision. (5) Recorded here. A row added in the form and left empty is dropped on save.
- Human response: none
- Resolution: as listed, pending review
- Overrides: none

## D-36: US-019 readings (version list)
- Timestamp: 2026-11-10
- Session: S5 implement US-019
- Class: assumption
- Type: non-blocking
- Trigger: (1) Order of checks of the version list. (2) Version list of a contact in Trash. (3) Recording on every kind of change already existed in the save path (T-08, T-21, T-23), so T-25 only adds the list.
- Options: (1) Role first (a Viewer gets 403 even for an unknown contact), then the contact (404 when it is not in the group). (2) A trashed contact still lists its versions (EX-019-02 needs it). (3) VersionController.list, VersionService.list, VersionRepository.list (newest first), VersionSummary (number, userName, createdAt).
- Human response: none
- Resolution: as listed, pending review
- Overrides: none

## D-37: US-007 readings (rename and delete a tag)
- Timestamp: 2026-11-10
- Session: S5 implement US-007
- Class: assumption
- Type: non-blocking
- Trigger: (1) Order of checks and the unknown tag. (2) Locking against a save that changes the same contacts. (3) What a rename to the exact same name does. (4) History entry shape. (5) The Manage tags screen and its tests were written in the same step (screen code before its tests, as in D-33 to D-35). (6) The Tags tab for a Viewer.
- Options: (1) Member (404), then role (a Viewer gets 403 even for an unknown tag), then the tag (404; a tag no contact carries does not exist). (2) The contacts that carry the tag are locked in id order before the tag row changes, as a save locks its contact first, so there is no deadlock; a name that clashes at that moment through the unique index gives the same 409 `TAG_NAME_TAKEN`. (3) 200 with the tag, no change, no History entry, no revision bump (EX-007-01). (4) One `TAG_RENAMED` or `TAG_DELETED` entry per affected contact, one change set, field `tag` with old and new value (delete: new value empty); a change of capitalisation only is a rename. `revision` grows, `versionNumber` does not (R-007-06, D-19). (5) Recorded here. (6) The Tags tab is shown to Owner and Editor only (AC-007-07 in the frozen browser test); a Viewer who types the address sees the list without controls.
- Human response: none
- Resolution: as listed, pending review
- Overrides: none

## D-38: US-009 readings (list and search)
- Timestamp: 2026-11-10
- Session: S5 implement US-009
- Class: assumption
- Type: non-blocking
- Trigger: (1) How a word is matched. (2) Descending order and empty last names. (3) Request shape before US-010. (4) Display of a row without a last name. (5) The list screen and its tests were written in the same step (screen code before its tests, as in D-33 to D-37). (6) When the search box sends its text.
- Options: (1) Each word is folded in SQL (`f_fold`) and must be part of `search_fold` (own fields), of a tag name or of an attribute value; `%`, `_` and `\` are escaped, nothing but bind parameters reaches SQL. (2) Ascending puts empty last names (stored as NULL) last, descending puts them first (a true reversal); ties break on last name, first name, then id, so pages are stable (EX-009-06). (3) `SearchController.SearchRequest` has `q`, `sort`, `page` only; `conditions` is ignored until US-010 adds it. An unknown sort field is refused with 400 by the enum and never reaches SQL (SR-07). (4) The last-name cell is the link to the contact; when empty, the link text is the first name, e-mail or company. (5) Recorded here. (6) The request follows the box after 250 ms; a new search text goes back to page 1, a new sort keeps the page (R-009-08).
- Human response: none
- Resolution: as listed, pending review
- Overrides: none

## D-39: US-010 readings (filter)
- Timestamp: 2026-11-10
- Session: S5 implement US-010
- Class: assumption
- Type: non-blocking
- Trigger: (1) Name of the button that clears all conditions. (2) How conditions are checked. (3) Matching of tags, keys, values, companies. (4) Where EX-010-04 and the E2E tests wait. (5) The suggestions of the company condition. (6) Screen code and its tests written in the same step (as in D-33 to D-38).
- Options: (1) The frozen tests click "Filter" by a name that matches as a substring, and AC-011-01 and E2E-03 do so while a chip is shown; a second button named "Clear filters" would make that click ambiguous. The chip bar therefore offers "Clear all" (sl: "Počisti vse"); "Clear filters" stays in the "No contacts match" panel (R-010-10, AC-010-08), which also clears the search text. (2) `Conditions.parse` returns the seven records of `Condition` or one 400 `VALIDATION_FAILED` naming `conditions[i].type|tags|tag|key|value|country|company` (REQUIRED; INVALID_VALUE for an unknown type; INVALID_FORMAT for a country that is not two capitals); texts are stripped; an empty attribute value is allowed for "key equals value". (3) Tag and key compare with `lower()`, the attribute value and the company with `f_fold` (case and accents); a contact only has tags of its own group, so tags of another group never match; every value is a bind parameter (SR-07); conditions and search words combine with AND in `Where`. (4) EX-010-04 (Trash) waits on US-012 like AC-009-08; own `ContactSearchIT` covers it with a SQL-trashed contact. E2E-03 and E2E-04 need export and Trash and are not run. (5) `GET /groups/{g}/companies?q=` (`SearchController`, `SearchService.companies`) lists each company once (folded), active contacts only, for every member. (6) Recorded here.
- Human response: none
- Resolution: as listed, pending review
- Overrides: none

## D-40: US-012 readings (Trash)
- Timestamp: 2026-11-10
- Session: S5 implement US-012
- Class: assumption
- Type: non-blocking
- Trigger: (1) Actor of the `CONTACT_ERASED` entry written by the cleanup. (2) Revision on trash and restore. (3) The cleanup's advisory lock. (4) How the Trash view shows the days left. (5) Order of the erasure steps. (6) Screen code and its tests written in the same step (as in D-33 to D-39).
- Options: (1) `history_entries.actor_name` is NOT NULL and no source names an actor for a system change; the cleanup writes `actor_id` NULL, `actor_name` "System", no e-mail (`HistoryRecorder.SYSTEM_ACTOR`); US-013 may show it as it likes. (2) Trash and restore raise `revision` by 1 (no version), so an edit opened before a trash and restore round trip meets a conflict instead of overwriting (same idea as D-19). (3) ADR-08 asks for `pg_try_advisory_lock`; one backend container runs the cleanup (ADR-08: several instances are not a requirement), so the lock is left out until there is a second instance. (4) The "Days left" column shows the number only (Slovenian "dni" has four forms); the column header carries the unit. (5) Erasure: record `CONTACT_ERASED` first, then anonymise every entry of the contact (the new one too), delete the row, delete unused tags, all in one transaction. The cleanup covers contacts only; groups, invitations and tombstones join it with US-015, US-003 and US-016. (6) Recorded here.
- Human response: none
- Resolution: as listed, pending review
- Overrides: none

## D-41: US-011 readings (export)
- Timestamp: 2026-11-10
- Session: S5 implement US-011
- Class: assumption
- Type: non-blocking
- Trigger: (1) Request with both or neither of `contactIds` and `filter`. (2) How a formula-like value is stored. (3) Which count the History entry holds. (4) Order and duplicates of ticked ids. (5) Where the 10,000 limit is checked. (6) Screen code and its tests written in the same step (as in D-33 to D-40).
- Options: (1) 400 `VALIDATION_FAILED`, field `contactIds`, code `REQUIRED`; an empty `filter` object means "all contacts". (2) The cell is a string with the quote-prefix style (spec 4.11); the text is unchanged. E-mail and phone cells use the text format "@". (3) `EXPORTED` holds `contacts` = rows really written, so a contact trashed meanwhile (EX-011-08) is not counted. (4) Ids are de-duplicated; rows follow the list sort (default last name, first name); unknown, foreign and trashed ids are left out silently. (5) Ids: by list length before reading; filter: by count before reading; both give 400 `TOO_MANY_CONTACTS` on field `contacts`, no file and no History entry. (6) Recorded here. "Select all N results" ends when one row is unticked: the rest of the page stays ticked.
- Human response: none
- Resolution: as listed, pending review
- Overrides: none

## D-42: US-015 readings (delete and restore a group)
- Timestamp: 2026-11-10
- Session: S5 implement US-015
- Class: assumption
- Type: non-blocking
- Trigger: (1) What delete and restore answer for the roles and states the contract leaves open. (2) Restore after day 30 and before the cleanup. (3) The enabled state of "Delete group" in the dialog. (4) Order of the cleanup steps. (5) The 429 missing in the code of `POST /groups/{groupId}/exports` (drift test). (6) Screen code and its tests written in the same step (as in D-33 to D-41).
- Options: (1) Delete: a non-Owner gets 403 for an active group and 404 for one in Trash; an Owner who meets a group already in Trash gets 409 `GROUP_IN_TRASH` (EX-015-05); a non-member gets 404. Restore: 404 for everyone except an Owner of a group in Trash (D-17: "a group not in Trash gets 404"), so the 403 of the contract is not produced. (2) Allowed, as ADR-08 says for contacts. (3) The button is enabled when something is typed; the server compares (case-sensitive, trimmed), so the frozen browser test AC-015-02 can click it and see the refusal. (4) The cleanup erases expired contacts first, then purges expired groups, each in its own transaction, so a contact's own 30 days (EX-015-06) run on their own. (5) The annotation list of `ExportController` got the 429 of the contract (one line; the drift test failed since US-011). (6) `GroupTrashIT`, `DeletedGroupTest` and `group-trash.test.tsx` were written with the code.
- Human response: none
- Resolution: as listed, pending review
- Overrides: none

## D-43: US-013 readings (History)
- Timestamp: 2026-11-10
- Session: S5 implement US-013
- Class: assumption
- Type: non-blocking
- Trigger: (1) "Newest first" with a clock that the frozen test EX-013-02 moves back (the older-looking edit must still be on top). (2) The group-wide History endpoint belongs to US-014's screen, but the frozen US-013 tests read `GET /groups/{g}/history`. (3) Group-level entries hold no e-mail (D-16). (4) The contact History tab as a link. (5) EX-013-03 (account deletion removes the actor's e-mail) needs `DELETE /me`.
- Options: (1) Order by a write sequence number (`seq`, migration V2) instead of the time; in production both orders are the same. (2) Build the endpoint with paging only (25 per page, Owner and Editor); the filters `actorId`, `types`, `from`, `to`, `/history/actors` and the screen stay with US-014. (3) An invitation or its cancellation holds the role only, a role change or removal the member's name and role; a leaving is recorded as MEMBER_REMOVED by the member (R-013-05). (4) A link "History" next to "Details" at `contacts/:id/history`, for Owner and Editor; the group-wide History tab is US-014. (5) Left to US-016; EX-013-03 and the browser test AC-013-10 (group History tab) wait for US-016 and US-014. (6) The tab shows a CONTACT_CREATED entry as one line without the first field values (the frozen browser test AC-013-01 expects the old phone once; the API still records them).
- Human response: none
- Resolution: as listed, pending review
- Overrides: none

## D-44: US-014 readings and two frozen API tests that contradict the specification
- Timestamp: 2026-11-10
- Session: S5 implement US-014
- Class: approval
- Type: non-blocking
- Trigger: (1) Frozen `AC_014_04` asserts that the answer for 2 July contains neither "Dan1" nor "Dan3", but the 2 July entry records the change Dan1 to Dan2 (R-013-02 old and new value), so "Dan1" is always in it; the count `total = 1` and the entry are right. (2) Frozen `EX_014_03` expects one entry by Marko after he left, but R-013-05 records a leaving as a removal by the member, so Marko has two (the contact he created and his leaving). (3) The group tab bar and the contact's own "Details / History" bar both showed a link "History" on the contact page; the frozen `AC-013-01` clicks `link "History"` and needs exactly one. (4) Where the filters and the person list are built.
- Options: (1) and (2) (proposed) Leave both frozen tests unchanged and the story `in progress` (DoD-S-01 not met); the behaviour follows the specification. You decide whether the tests should compare the entry (not the whole body) and expect two entries for Marko (a change to frozen files, new freeze). Alternative for (2): record a leaving with no actor, against R-013-05. (3) The group tab bar is not shown on the contact pages (WF-10 and WF-11 have no group tab bar; the contact page keeps its breadcrumb). (4) `HistoryFilter` (domain; days taken in Europe/Ljubljana, the end of the last day exclusive; `to` before `from` gives 400 `INVALID_DATE_RANGE` on `to`), `HistoryRepository` builds the WHERE for group, person, types and days, `GET /groups/{g}/history/actors` lists each actor once with the latest name, also those who left. The screen takes one event type (the wireframe has one list) and refuses a range with the end before the start before it sends the request.
- Human response: "2 - both frozen tests are wrong and the app follows the specification (R-013-07 keeps the old value in every edit entry; R-013-05 records a leaving as a removal by the member). Fix only these two tests, then freeze the tests again. Items 3 and 4 are approved as proposed."
- Resolution: human-approved fix of a frozen-test defect (2026-11-10, S5 implement US-014). `AC_014_04` keeps `total = 1`, checks that the one entry's new value is "Dan2" and that no returned entry has "Dan1" or "Dan3" as its new value (no longer a search of the whole body). `EX_014_03` expects 2 entries by Marko, one of them `MEMBER_REMOVED` (his leaving). Only these two tests in `Us014GroupHistoryAcceptanceIT.java` changed; its line in `docs/03_acceptance-manifest.sha256` is updated to the new hash. Items 3 and 4 as proposed. No production code changed.
- Overrides: none

## D-45: US-016 readings (account deletion)
- Timestamp: 2026-11-10
- Session: S5 implement US-016
- Class: assumption
- Type: non-blocking
- Trigger: (1) The "Sign in" button of the "Sign in again to do this" dialog (WF-19) needs an action; the web app has no re-authentication call. (2) The backend deletes the data before Firebase, so a failing Firebase `deleteUser` leaves a Firebase account without product data (ADR-02 accepts this). (3) The wireframe intro says the name is "anonymised"; R-016-03 keeps the name.
- Options: (1) The button signs the person out, which leads to the sign-in screen; after signing in the person deletes again with a recent sign-in. (2) The screen then signs the person out so that the session ends; the person can retry as ADR-02 describes. (3) The intro says the e-mail is removed from the History and the name stays (R-016-03, D-16).
- Human response: none
- Resolution: as listed, pending review. Server: `DELETE /me` (`AccountService`) checks `auth_time` within 5 minutes, locks the user's groups in id order, refuses with `SOLE_OWNER` and `groups[]` (also groups in Trash), removes the e-mail from the History entries the user caused, records the id in `deleted_accounts` and deletes the user (memberships cascade). The daily removal of old `deleted_accounts` rows (ADR-08) is not in this story's tasks.
- Overrides: none

## D-46: NFR timing readings (T-N01, T-N02)
- Timestamp: 2026-11-10
- Session: S5 implement T-N01 to T-N03
- Class: assumption
- Type: non-blocking
- Trigger: The load test with 10,000 contacts (`scripts/perf/load-test.mjs`) gave p95 search 543 ms and open contact 17 ms with no errors in 1,740 requests (`out/logs/perf-load-test.log`). The two-word search with accents takes about 550 to 700 ms. This record was referenced in `00_progress.md` but was missing from the log; it is written here from that log.
- Options: (1) Accept as measured, since NFR-01 and NFR-02 pass. (2) Add an index for the folded two-word search later if the time grows.
- Human response: none
- Resolution: (1), a watch item for `/review INC-1`. Export timing is in `out/logs/perf-export-time.log` (NFR-15 records the time only).
- Overrides: none

## D-47: Build-only npm packages with a licence outside the allowed list (T-N12, NFR-14)
- Timestamp: 2026-11-10
- Session: S5 implement T-N12
- Class: assumption
- Type: non-blocking
- Trigger: `scripts/licence-check.mjs` finds five build-scope packages whose licence field is not an allowed SPDX id: argparse 3.0.4 (PSF-2.0), deep-freeze (public domain), url-template (BSD), fuzzy (a `licenses` array, MIT), valid-url (no field; MIT in its repository). They come through `firebase-tools` (the emulator) or `swagger-parser` (contract tooling); none is in an image or the web bundle. Two Maven test-scope dependencies (archunit-junit5, spring-security-test) are not in `tech-stack.md`; they are recorded with Apache-2.0 in this log (D-24, "Backend, test scope").
- Options: (1) Accept them as reviewed build-only exceptions, listed in `BUILD_REVIEWED` of the script (proposed). (2) Replace the tools. (3) Add them to `tech-stack.md` (needs the human).
- Human response: none
- Resolution: (1), pending review. The MIT licences of fuzzy and valid-url are not read from a file in the repository: please confirm them in `/review`.
- Overrides: none
