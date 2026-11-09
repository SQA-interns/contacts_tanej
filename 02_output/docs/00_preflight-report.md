# Preflight report

> For: agent (writes), human (reads) · Written in: phase 0 (`/preflight`) · Source: `project/00_setup/*` · Procedure: `skills/preflight` · Agent: writes

Session S1 re-run (`/preflight D-05: 1, D-06: 1, D-07: 1, D-08: 2 (fallback 1), D-09: 1, D-10: 1, D-11: 1`), 2026-11-09. Result: **not passed** (see "Asked the human"). D-05 to D-11 are answered (`decisions-log.md`); the re-scan raised D-12 and D-13.

| Check | Source of truth | Result |
|---|---|---|
| Run configuration complete | `project/00_setup/run-config.md` | Passed |
| Platforms and tools at the listed versions | `project/00_setup/tech-stack.md` | Passed with decisions: Node 24.13.0 / npm 11.6.2 (D-01), Java and Docker (D-02), PostgreSQL 17.6 in container (D-03). Maven runs in a container (D-10) |
| Local environments and services reachable | `project/00_setup/environments.md` | Passed for Docker (daemon up, Compose v5.7.0) and the `postgres:17.6` container. `firebase-tools` 15.35.0 supports Node 24 (engines), emulator start belongs to `/implement` |
| Secrets present in `.env` | `project/00_setup/secrets.env.example` | Passed (first session; values not shown) |
| Every listed dependency resolves | `project/00_setup/tech-stack.md` | Passed: all pins and the replacements below resolve (npm registry, Maven Central); 3 images resolve |
| No Critical or High vulnerability | `project/00_setup/tech-stack.md` | **Not passed**: vitest (D-12), transitive Maven (D-13) |
| Clean working tree | repository | Passed apart from untracked run logs and scratch scan folders in `out/logs/` (`preflight-scan2/node_modules` left by the Playwright install, not committed) |
| Browser runner screenshot | `tech-stack.md` (Playwright) | **Passed** (D-11): Chromium 140, Firefox 141, WebKit 26 each saved a blank-page screenshot, `out/logs/preflight-screenshot*.png` |
| Input manifest written | `docs/00_input-manifest.sha256` | Not written (gate not passed) |

## Platform and tool results

| id | Pinned | Found | Method | Result |
|---|---|---|---|---|
| java | Temurin 21.0.8 | Oracle 21.0.11 | `java -version` | Accepted (D-02) |
| node | 22.20.0 | 24.13.0 | `node --version` | Approved (D-01) |
| npm | 10.9.3 | 11.6.2 | `npm --version` | Approved (D-01) |
| docker | 28.4.0 | 29.9.3, Compose v5.7.0 | `docker --version` | Accepted (D-02) |
| postgres | 17.6 | 17.6 | `docker run --rm postgres:17.6 postgres --version` | Passed (D-03) |
| maven | 3.9.11 | 3.9.11 in `maven:3.9.11-eclipse-temurin-21` | `docker run` of `mvn` | Passed in container (D-10); not on host |
| playwright | 1.55.1 | 1.55.1, browsers downloaded | `npx playwright --version`, screenshot script | Passed (D-11) |

## Dependency results

Methods: (1) registry lookup of the exact version; (2) OSV per pin; (3) `npm audit` (all and `--omit=dev`) on a scratch lockfile with the new versions (`out/logs/preflight-scan2/`, `--package-lock-only`; logs `preflight-npm-audit-all-2.json`, `-prod-2.json`); (4) Maven: OWASP dependency-check 12.1.6 in a Maven container (failed: NVD status 524 and a database column error, no report) and, as the second method, `mvn dependency:list` of 130 artifacts queried against OSV (`out/logs/preflight-scan-maven/`).

| id | version | Resolves | Highest finding | Action |
|---|---|---|---|---|
| spring-boot-starter-*, postgresql | 3.5.16, 42.7.14 | yes | none at direct level | D-04 applied |
| react-router | 7.9.3 -> 7.18.5 | yes | none | D-05 applied |
| vite | 7.1.7 -> 7.3.8 | yes | none | D-06 applied |
| @stryker-mutator/core, vitest-runner | 9.1.1 -> 9.6.1 | yes | none | D-07 applied |
| firebase | 12.3.0 kept (12.19.0 tested) | yes | High (Firestore/gRPC, prod; also in 12.19.0) | Recorded as Medium by the human's fallback (D-08); `/verify` re-checks the bundle |
| firebase-tools | 14.19.1 -> 15.35.0 | yes | none | D-09 applied |
| vitest, @vitest/coverage-v8 | 3.2.7 | yes | **Critical** (tinypool 1.1.1) | Blocking (D-12) |
| tomcat-embed-core 10.1.55, spring-webmvc 6.2.19, jackson-databind and -core 2.19.1 | transitive | yes | **Critical / High** | Blocking (D-13) |
| all other npm and Maven pins | as listed | yes | none at High or above | none |
| postgres, eclipse-temurin, nginx, gitleaks (images) | as listed | yes | not scanned | none |

## Asked the human

| When | What was missing | Answer | Re-check |
|---|---|---|---|
| 2026-11-09 | D-01 to D-03 | 2, 1, 1 | Done |
| 2026-11-09 | D-05 to D-11 | 1, 1, 1, 2 (fallback 1), 1, 1, 1 | Done; Maven OWASP scan did not complete, replaced by OSV on the full tree |
| 2026-11-09 | D-12 (vitest Critical), D-13 (Maven transitive Critical/High) | pending | `/preflight` re-run with answers |
