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
- Human response: none
- Resolution: option 1, pending review
- Overrides: tech-stack.md dependencies `spring-boot-starter-*` (3.5.6 -> 3.5.16), `org.postgresql:postgresql` (42.7.8 -> 42.7.14), `vitest` and `@vitest/coverage-v8` (3.2.4 -> 3.2.7)

## D-05: react-router 7.9.3 has High findings; the fix is another minor line
- Timestamp: 2026-11-09
- Session: S2 preflight
- Class: block
- Type: blocking
- Trigger: `react-router@7.9.3` has 13 advisories, 7 High (for example GHSA-2w69-qvjg-hvjx XSS via open redirects, GHSA-chx6-hx7r-mcp5 unauthenticated DoS in route matching); `npm audit --omit=dev` reports it as High. No 7.9.x patch fixes them (7.9.6 still has them); the fixes start at 7.18.0 and `npm audit` proposes 7.18.5. A different minor is blocking.
- Options: 1. (proposed) Replace with `react-router` 7.18.5 (checked again in the next preflight). 2. Another exact version you name. 3. Keep 7.9.3 with written evidence (a client-only SPA does not use the SSR, RSC or `__manifest` paths); lowering a High is a blocking decision and does not cover the open-redirect and XSS findings.
- Human response: none
- Resolution: pending
- Overrides: tech-stack.md dependency `react-router` (if option 1 or 2)

## D-06: vite 7.1.7 has High findings; the fix is another minor line
- Timestamp: 2026-11-09
- Session: S2 preflight
- Class: block
- Type: blocking
- Trigger: `vite@7.1.7` has 11 advisories, 3 High (GHSA-p9ff-h696-f583 arbitrary file read via dev-server WebSocket, GHSA-v2wj-q39q-566r `server.fs.deny` bypass, GHSA-587h-7wmj-h5pm). The 7.1.x line has no fix (7.1.12 still affected); fixed in 7.3.x (`npm audit`: 7.3.8). The affected code is the dev server, not the built app.
- Options: 1. (proposed) Replace with `vite` 7.3.8; `@tailwindcss/vite` 4.1.13 and `@vitejs/plugin-react` 5.0.4 stay (compatibility is checked in `/implement`). 2. Another exact version you name. 3. Keep 7.1.7 with written evidence that the dev server is bound to localhost only (blocking decision to lower).
- Human response: none
- Resolution: pending
- Overrides: tech-stack.md dependency and tooling `vite` (if option 1 or 2)

## D-07: @stryker-mutator/core 9.1.1 has High findings; the fix is another minor line
- Timestamp: 2026-11-09
- Session: S2 preflight
- Class: block
- Type: blocking
- Trigger: `@stryker-mutator/core@9.1.1` is reported High by `npm audit` (via `minimatch`, `ajv`, `mutation-server-protocol`); the fix target is 9.6.1 (non-major, but another minor). The tooling note also names `@stryker-mutator/vitest-runner` 9.1.1, which is not a `dependencies` entry.
- Options: 1. (proposed) Replace `@stryker-mutator/core` and `@stryker-mutator/vitest-runner` with 9.6.1. 2. Keep 9.1.1 (dev-only mutation tool, no runtime exposure), with written evidence; blocking decision to lower.
- Human response: none
- Resolution: pending
- Overrides: tech-stack.md tooling `@stryker-mutator/core` and its option text (if option 1)

## D-08: firebase 12.3.0 has a High finding in the unused Firestore client
- Timestamp: 2026-11-09
- Session: S2 preflight
- Class: block
- Type: blocking
- Trigger: `npm audit --omit=dev` reports `firebase@12.3.0` High through `@firebase/firestore` -> `@grpc/grpc-js` (GHSA-m9gg-hp2v-232j, High, CVSS 7.4: unauthorised certificates accepted in certain configurations). `npm audit`'s suggested fix (`firebase@9.14.0`) is an older major and not usable. The project uses only Firebase Authentication (`tech-stack.md`); the Firestore code is installed but not imported.
- Options: 1. (proposed) Keep `firebase` 12.3.0 and record the finding as Medium on written evidence: Firestore is never imported, gRPC runs server-side or on Node only, and the browser build tree-shakes it (checked again by `/verify` on the built bundle). 2. Move to a newer 12.x release that resolves it, if one exists (the agent brings the exact version to the next preflight). 3. Keep as High (blocks release).
- Human response: none
- Resolution: pending
- Overrides: none (severity lowering, `general/quality/severity-scale.md`)

## D-09: firebase-tools 14.19.1 has Critical and High findings and does not support Node 24
- Timestamp: 2026-11-09
- Session: S2 preflight
- Class: block
- Type: blocking
- Trigger: `npm audit` reports `firebase-tools@14.19.1` High with transitive Critical `tar` and High `minimatch`, `zod`, `basic-ftp` and others (`out/logs/preflight-npm-audit-all.json`); the fix is `firebase-tools@15.35.0` (another major). `superstatic@9.2.0`, a dependency, declares `node 18 || 20 || 22` and warns on the host's Node 24.13.0 (D-01 option 2). The package is test-only (Authentication Emulator on localhost, no deploy).
- Options: 1. (proposed) Replace with `firebase-tools` 15.35.0; the agent checks its Node range and findings in the next preflight. 2. Run the emulator in a `node:22` container so the Node range holds, and keep 14.19.1 as test-only with written evidence (lowering Critical/High, blocking). 3. Another exact version you name.
- Human response: none
- Resolution: pending
- Overrides: tech-stack.md dependency and tooling `firebase-tools` (if option 1 or 3)

## D-10: Maven dependency scan cannot run on the host
- Timestamp: 2026-11-09
- Session: S2 preflight
- Class: block
- Type: blocking
- Trigger: `mvn` is not on the host and no `pom.xml` or Maven wrapper exists, so `org.owasp:dependency-check-maven` (the listed dependency-scan tool for the backend) cannot run; the agent must not install software. Only the direct pins were checked, with OSV (all resolved; findings in D-04); transitive Maven dependencies are not scanned. The phase 0 gate needs a complete scan.
- Options: 1. (proposed) Allow running the scan inside a `maven:3.9.11-eclipse-temurin-21` container (docker only, nothing installed on the host; adds one image as dev-only tooling, non-blocking record at the time). 2. You install Maven 3.9.11 on the host, then re-run `/preflight`. 3. Accept the OSV direct-pin scan as the gate evidence and run the full OWASP scan in the quality gate (weaker; blocking decision).
- Human response: none
- Resolution: pending
- Overrides: none

## D-11: Playwright browsers must be downloaded to the host
- Timestamp: 2026-11-09
- Session: S2 preflight
- Class: block
- Type: blocking
- Trigger: Preflight step 2 needs Playwright to start its browser and save `out/logs/preflight-screenshot.png`; `@playwright/test@1.55.1` resolves, but its Chromium, Firefox and WebKit downloads (`npx playwright install`) go to the host's user profile (`AGENTS.md`: no installs on the host without approval). Not run.
- Options: 1. (proposed) Approve `npx playwright install chromium firefox webkit` for the run (the pinned 1.55.1 build). 2. You run the command yourself. 3. Run Playwright in the pinned-version Microsoft container (adds an image; separate decision).
- Human response: none
- Resolution: pending
- Overrides: none
