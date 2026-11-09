# Preflight report

> For: agent (writes), human (reads) · Written in: phase 0 (`/preflight`) · Source: `project/00_setup/*` · Procedure: `skills/preflight` · Agent: writes

Session S1, 2026-11-09. Result: **not passed** (partial run; see "Asked the human").

| Check | Source of truth | Result |
|---|---|---|
| Run configuration complete | `project/00_setup/run-config.md` | Passed (read; agent, model, permission mode set) |
| Platforms and tools installed at the listed versions | `project/00_setup/tech-stack.md` | **Not passed** (see table below) |
| Local environments and services running or reachable | `project/00_setup/environments.md` | Partly: Docker daemon reachable (`docker info`); PostgreSQL runs as a container (not checked on host); Firebase emulator not checkable until `firebase-tools` is installed |
| Secrets present in `.env` or marked test-only | `project/00_setup/secrets.env.example` | Passed: `DB_PASSWORD` and `FIREBASE_WEB_API_KEY` non-empty (sed/grep check, values not shown) |
| Every listed dependency resolves | `project/00_setup/tech-stack.md` | Not complete: container manifests and `react@19.1.1` resolve; Maven Central and the other npm pins not checked |
| No listed dependency has a known Critical or High vulnerability | `project/00_setup/tech-stack.md` | Not run (no build manifests exist in `02_output/` yet; no scan tool available) |
| Clean working tree on the starting commit | repository | Passed: only untracked `02_output/logs/run-console-*.log` (runner logs) |
| Input manifest written | `docs/00_input-manifest.sha256` | Not written (gate not passed) |
| Browser runner screenshot (`out/logs/preflight-screenshot.png`) | `tech-stack.md` (Playwright) | Not run: Playwright is not installed |

## Platform and tool results

| id | Pinned | Found | Method | Result |
|---|---|---|---|---|
| java | Temurin 21.0.8 | Java(TM) SE 21.0.11 (Oracle HotSpot) | `java -version` | Runs, other patch and vendor |
| node | 22.20.0 | 24.13.0 | `node --version` | Runs, **other major** |
| npm | 10.9.3 | 11.6.2 | `npm --version` | Runs, **other major** |
| docker | 28.4.0 | 29.9.3 (daemon 29.9.3) | `docker --version`, `docker info` | Runs, other major |
| postgres | 17.6 | not found on host | `postgres --version` (refused to run / not on PATH) | Not confirmed; runs in the `postgres:17.6` container |
| maven | 3.9.11 | not checked | `./mvnw -v` | No wrapper yet (no backend project in `02_output/`) |
| gitleaks, firebase-tools, playwright, stryker, other npm/Maven tools | pinned | not checked | n/a | Not installed; resolved only when the project is built |

## Dependency results

| id | version | Resolves | Highest vulnerability |
|---|---|---|---|
| postgres (image) | 17.6 | yes (`docker manifest inspect`) | not scanned |
| eclipse-temurin (image) | 21.0.8_9-jre | yes | not scanned |
| nginx (image) | 1.29.1 | yes | not scanned |
| gitleaks (image) | 8.28.0 (`v8.28.0`) | yes | not scanned |
| react | 19.1.1 | yes (`npm view`) | not scanned |
| all other entries | | not checked | not scanned |

## Asked the human

| When | What was missing | Answer | Re-check |
|---|---|---|---|
| 2026-11-09 | Decisions D-01 to D-03 (host tool versions, scan approach) | pending | `/preflight` re-run |
