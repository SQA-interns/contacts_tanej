# Preflight report

> For: agent (writes), human (reads) · Written in: phase 0 (`/preflight`) · Source: `project/00_setup/*` · Procedure: `skills/preflight` · Agent: writes

Session S1 re-run (`/preflight D-12: 2 ..., D-13: 1 ...`), 2026-11-09. Result: **passed**. D-01 to D-13 are answered (`decisions-log.md`). The input manifest is written.

| Check | Source of truth | Result |
|---|---|---|
| Run configuration complete | `project/00_setup/run-config.md` | Passed |
| Platforms and tools at the listed versions | `project/00_setup/tech-stack.md` | Passed with decisions: Node 24.13.0 / npm 11.6.2 (D-01), Java and Docker (D-02), PostgreSQL 17.6 in container (D-03). Maven runs in a container (D-10) |
| Local environments and services reachable | `project/00_setup/environments.md` | Passed for Docker (daemon up, Compose v5.7.0) and the `postgres:17.6` container. `firebase-tools` 15.35.0 supports Node 24 (engines), emulator start belongs to `/implement` |
| Secrets present in `.env` | `project/00_setup/secrets.env.example` | Passed (first session; values not shown) |
| Every listed dependency resolves | `project/00_setup/tech-stack.md` | Passed: all pins and the replacements below resolve (npm registry, Maven Central); 3 images resolve |
| No Critical or High vulnerability | `project/00_setup/tech-stack.md` | **Passed with recorded exceptions**: no Critical in npm; firebase High recorded as Medium (D-08); two Spring Framework Critical recorded as not reachable on the human's approval (D-13) |
| Clean working tree | repository | Passed apart from untracked run logs and scratch scan folders in `out/logs/` (`preflight-scan2/node_modules` not committed) |
| Browser runner screenshot | `tech-stack.md` (Playwright) | Passed (D-11): Chromium 140, Firefox 141, WebKit 26, `out/logs/preflight-screenshot*.png` |
| Input manifest written | `docs/00_input-manifest.sha256` | Written: 73 files, SHA-256, LF-normalised |

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

Methods: (1) registry lookup of the exact version; (2) OSV per pin; (3) `npm audit` (all and `--omit=dev`) on a scratch lockfile with the final versions (`out/logs/preflight-scan2/`, `--package-lock-only`); (4) Maven: OWASP dependency-check 12.1.6 failed (NVD status 524, database column error, no report), so the second method is `mvn dependency:list` of 130 artifacts with the D-13 overrides (`out/logs/preflight-scan-maven/deps2.txt`) queried against OSV (`osv.mjs`).

| id | version | Resolves | Highest finding | Action |
|---|---|---|---|---|
| spring-boot-starter-*, postgresql | 3.5.16, 42.7.14 | yes | none at direct level | D-04 applied |
| react-router | 7.9.3 -> 7.18.5 | yes | none | D-05 applied |
| vite | 7.1.7 -> 7.3.8 | yes | none | D-06 applied |
| vitest, @vitest/coverage-v8 | 3.2.4 -> 4.1.11 | yes | none above Moderate; tinypool Critical gone | D-12 applied |
| @stryker-mutator/core, vitest-runner | 9.1.1 -> 10.0.0 | yes | Moderate | D-12 applied (replaces D-07) |
| firebase | 12.3.0 kept | yes | High (Firestore/gRPC, prod) | Recorded as Medium (D-08); `/verify` re-checks the bundle |
| firebase-tools | 14.19.1 -> 15.35.0 | yes | Moderate | D-09 applied |
| tomcat-embed-core | transitive 10.1.55 -> 10.1.59 | yes | none | D-13 override |
| jackson-databind, jackson-core | transitive 2.19.1 -> 2.21.7 | yes | none | D-13 override |
| spring-webmvc | transitive 6.2.19 (latest 6.2.x) | yes | **Critical** x2 (GHSA-j9f9-w8pj-32f8, GHSA-pc63-qcmh-9cmg) | Recorded as not reachable, human-approved (D-13); `/verify` re-checks |
| commons-lang3 3.17.0, log4j-api 2.24.3 | transitive | yes | Moderate | none |
| all other npm and Maven pins | as listed | yes | none at High or above | none |
| postgres, eclipse-temurin, nginx, gitleaks (images) | as listed | yes | not scanned | none |

## Asked the human

| When | What was missing | Answer | Re-check |
|---|---|---|---|
| 2026-11-09 | D-01 to D-03 | 2, 1, 1 | Done |
| 2026-11-09 | D-05 to D-11 | 1, 1, 1, 2 (fallback 1), 1, 1, 1 | Done |
| 2026-11-09 | D-12 (vitest Critical), D-13 (Maven transitive) | 2 (vitest 4.1.11, Stryker 10.0.0), 1 (overrides) | Done: npm audit has no Critical; OSV shows only the two approved Spring findings |
