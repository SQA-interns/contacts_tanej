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
