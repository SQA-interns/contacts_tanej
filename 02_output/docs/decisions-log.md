# Decisions log

> For: agent (writes), human (reads) · Written in: every session · Format: `general/rules/decisions.md` ("Decision record") · Agent: appends only

## D-01: Host Node.js and npm are other major versions than the pins
- Timestamp: 2026-11-09
- Session: S1 preflight
- Class: block
- Type: blocking
- Trigger: `tech-stack.md` pins Node 22.20.0 and npm 10.9.3; the host runs Node 24.13.0 and npm 11.6.2 (`node --version`, `npm --version`). The agent must not install or upgrade software on the host.
- Options: 1. (proposed) Install Node 22.20.0 (for example with nvm-windows or a portable copy) so the pins hold; npm 10.9.3 comes with it. 2. Approve Node 24.13.0 / npm 11.6.2 as the project's versions (replaces the `tech-stack.md` entries for this run). 3. Run frontend tooling in a `node:22.20.0` container.
- Human response: none
- Resolution: pending
- Overrides: tech-stack.md platforms `node`, tooling `npm` (only if option 2)

## D-02: Host Java and Docker report other versions than the pins
- Timestamp: 2026-11-09
- Session: S1 preflight
- Class: open
- Type: non-blocking
- Trigger: pin Temurin 21.0.8, host Oracle Java 21.0.11 (same major, newer patch, other vendor); pin Docker 28.4.0, host 29.9.3. Both run. The listed pins stay authoritative (`tech-stack.md` Rules); the container image `eclipse-temurin:21.0.8_9-jre` is unaffected.
- Options: 1. (proposed) Accept the host versions for local builds; the delivered images use the pinned tags. 2. Install the exact pins.
- Human response: none
- Resolution: pending review
- Overrides: none

## D-03: PostgreSQL host binary and dependency scan
- Timestamp: 2026-11-09
- Session: S1 preflight
- Class: open
- Type: non-blocking
- Trigger: `postgres --version` is not available on the host (PostgreSQL runs as the `postgres:17.6` container, which resolves); the dependency scan (OWASP dependency-check, `npm audit`) cannot run because `02_output/` holds no `pom.xml` or `package.json` yet, and no scan tool is installed.
- Options: 1. (proposed) Check the PostgreSQL version through `docker run --rm postgres:17.6 postgres --version`, and scan the pinned set from a temporary manifest generated from `tech-stack.md` in `out/logs/` in the next `/preflight`. 2. Move the dependency scan to the first `/implement` (not allowed by the phase 0 gate).
- Human response: none
- Resolution: pending review
- Overrides: none
