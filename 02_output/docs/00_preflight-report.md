# Preflight report

> For: agent (writes), human (reads) · Written in: phase 0 (`/preflight`) · Source: `project/00_setup/*` · Procedure: `skills/preflight` · Agent: writes

Session S1 re-run (`/preflight D-01: 2, D-02: 1, D-03: 1`), 2026-11-09. Result: **not passed** (see "Asked the human"). D-01 to D-03 are answered (see `decisions-log.md`).

| Check | Source of truth | Result |
|---|---|---|
| Run configuration complete | `project/00_setup/run-config.md` | Passed |
| Platforms and tools at the listed versions | `project/00_setup/tech-stack.md` | Passed with decisions: Node 24.13.0 / npm 11.6.2 approved (D-01), Java and Docker patch/major accepted (D-02), PostgreSQL 17.6 confirmed in container (D-03). Maven not on host (D-10) |
| Local environments and services reachable | `project/00_setup/environments.md` | Passed for Docker (daemon up, Compose v5.7.0) and the `postgres:17.6` container. Firebase emulator not checkable before `firebase-tools` is settled (D-09) |
| Secrets present in `.env` | `project/00_setup/secrets.env.example` | Passed (first session; `DB_PASSWORD`, `FIREBASE_WEB_API_KEY` non-empty, values not shown) |
| Every listed dependency resolves | `project/00_setup/tech-stack.md` | **Passed**: all 56 npm and Maven entries resolve (registry fetch of the exact version or its pom); 3 container images resolve (first session) |
| No Critical or High vulnerability | `project/00_setup/tech-stack.md` | **Not passed**: see "Dependency results" (D-04 to D-10) |
| Clean working tree | repository | Passed: only untracked run logs and `out/logs/` scan files |
| Browser runner screenshot | `tech-stack.md` (Playwright) | **Not run**: browsers must be downloaded to the host (D-11) |
| Input manifest written | `docs/00_input-manifest.sha256` | Not written (gate not passed) |

## Platform and tool results

| id | Pinned | Found | Method | Result |
|---|---|---|---|---|
| java | Temurin 21.0.8 | Oracle 21.0.11 | `java -version` | Runs; accepted (D-02) |
| node | 22.20.0 | 24.13.0 | `node --version` | Approved for the run (D-01) |
| npm | 10.9.3 | 11.6.2 | `npm --version` | Approved for the run (D-01) |
| docker | 28.4.0 | 29.9.3, Compose v5.7.0 | `docker --version`, `docker compose version` | Runs; accepted (D-02) |
| postgres | 17.6 | 17.6 (Debian 17.6-2.pgdg13+1) | `docker run --rm postgres:17.6 postgres --version` | Passed (D-03) |
| maven | 3.9.11 | not on host | `mvn -v` (exit 127); pom resolves on Maven Central | Not runnable (D-10) |
| playwright browsers | 1.55.1 | not downloaded | n/a | Not run (D-11) |

## Dependency results

Methods: (1) registry lookup of the exact version (npm registry, Maven Central pom); (2) OSV query per pin (npm, Maven; direct pins only); (3) `npm audit` (all and `--omit=dev`) on a lockfile generated from a temporary manifest in `out/logs/preflight-scan/` (`--package-lock-only`, nothing installed). Logs: `out/logs/preflight-resolve.json`, `preflight-npm-audit-all.json`, `preflight-npm-audit-prod.json`. The OWASP dependency-check analyser did not run (D-10).

| id | version | Resolves | Highest finding | Action |
|---|---|---|---|---|
| spring-boot-starter-* | 3.5.6 | yes | High (actuator, auth bypass) | -> 3.5.16, no finding (D-04, non-blocking) |
| org.postgresql:postgresql | 42.7.8 | yes | High | -> 42.7.14, no finding (D-04) |
| vitest, @vitest/coverage-v8 | 3.2.4 | yes | Critical | -> 3.2.7 (D-04); Moderate remains |
| react-router | 7.9.3 | yes | High (7 of 13) | Blocking, fix in 7.18.x (D-05) |
| vite | 7.1.7 | yes | High (3 of 11) | Blocking, fix in 7.3.x (D-06) |
| @stryker-mutator/core | 9.1.1 | yes | High | Blocking, fix 9.6.1 (D-07) |
| firebase | 12.3.0 | yes | High (transitive, Firestore/gRPC, prod) | Blocking (D-08) |
| firebase-tools | 14.19.1 | yes | Critical (transitive `tar`) | Blocking; also Node 24 (D-09) |
| all other npm and Maven pins | as listed | yes | none by OSV / npm audit at High or above | none |
| transitive Maven dependencies | | | not scanned | Blocking (D-10) |
| postgres, eclipse-temurin, nginx, gitleaks (images) | as listed | yes | not scanned | none |

## Asked the human

| When | What was missing | Answer | Re-check |
|---|---|---|---|
| 2026-11-09 | D-01 to D-03 (host versions, scan approach) | 2, 1, 1 | Done |
| 2026-11-09 | D-05 to D-11 (vulnerability replacements, Maven scan route, Playwright browsers) | pending | `/preflight` re-run with answers |
