---
name: sdd-preflight
description: Session S1, phase 0. Check that every platform, tool, browser, service, secret and dependency the run needs is present, resolvable and free of known Critical or High vulnerabilities, ask for whatever is missing, and freeze the inputs with a manifest.
disable-model-invocation: true
---

> For: agent · Owner: Team lead · Agent: read-only

## Session

- Id: S1, phase 0. Frame: measure.
- Reads: `project/00_setup/tech-stack.md`, `environments.md`, `secrets.env.example` (key names), `run-config.md` ("Pre-approved findings"); `general/rules/decisions.md`; `general/rules/secrets.md`; `general/quality/severity-scale.md`.
- Writes: `docs/00_preflight-report.md`, `docs/00_input-manifest.sha256`, `docs/decisions-log.md`, `docs/00_progress.md`, `out/logs/`.
- Must not: read requirements or design; install, upgrade or substitute anything.
- Exit (phase 0 gate): every check passes; every dependency resolves with no known Critical or High vulnerability; the input manifest is written.

## Procedure

1. For every `platforms` and `tooling` entry: run its `version_check` (or the tool's own version command) and confirm the listed version. A pin that runs but reports another version (for example an image tag) is a non-blocking decision; one that does not resolve or run is a failure.
2. For a component with a user interface: confirm the browser test runner starts **every** browser engine the quality requirements name (for example Chromium, Firefox and WebKit, with their system libraries) and saves a screenshot of a blank page per engine (`out/logs/preflight-screenshot-<engine>.png`). *Why:* `/sdd-wireframes` and every UI check depend on it; in earlier runs a missing engine or library surfaced only at `/sdd-verify` and cost a round.
3. For every environment and service in `environments.md` needed locally: confirm it is running or reachable. Record the host's free memory and CPU cores; if they look too small for the mutation runs of phase 5 (several test processes at once), say so in the report. Every later gate's tool must run here: the scanners, the mutation tools and the image scanner, each started once.
4. For every key of `secrets.env.example` not marked test-only or not needed: confirm it is `filled` in the output of `node system/run.mjs --env-check` (never displays a value). Also confirm `git check-ignore .env` succeeds.
5. For every `dependencies` entry: confirm the exact version resolves. A pin marked `[ASSUMPTION] verify in preflight` that does not resolve, or has a newer patch with a security fix, is replaced as `general/rules/decisions.md` allows (same minor: non-blocking; otherwise blocking).
5a. For every framework line the stack depends on (for example the web framework and its security module): find when its free (open-source) security support ends. A line already out of free support, or ending within six months, is a blocking decision with the newest supported line as the default option. *Why:* in v001 every new CVE of an unsupported line could only be accepted, never fixed.
6. Scan the dependency set with the `dependency-scan` tools, using each tool's `options`, and every pinned container image with the image scanner. Critical or High is a blocking decision unless "Pre-approved findings" covers it. An analyser that needs credentials `secrets.env.example` does not list: disable it, record a non-blocking decision, re-run; at least one analyser must complete. For each Critical or High, save the advisory's text (affected and fixed versions, preconditions) to `out/logs/advisories/<id>.md`, so later sessions can judge reachability without searching.
7. Confirm the working tree is clean (apart from an untracked `.env`).
8. Re-run every failed check a second, different way; report only failures both confirm. Write each result with its method (never values) to `docs/00_preflight-report.md`.
9. If anything failed: ask for all of it in one message (what to install or start with its exact version; which key to add to `.env`; version or vulnerability decisions) and end the session. The next `/sdd-preflight` re-runs only the failed checks.
10. When everything passes, ask the developer once (in the report's "Developer, do this") to hold upgrades of the host's tools (runtime, JDK, browsers, Docker) until the run ends. *Why:* a tool that upgrades itself mid-run changes what the gates measure.
11. Write `docs/00_input-manifest.sha256`: SHA-256 of every file under `01_input/`, `.claude/skills/` and `system/`, and of `AGENTS.md`, `CLAUDE.md`, `README.md`, `Run_Guide.md`; LF-normalised, `sha256sum` format.
12. Commit the report with the manifest (`docs:`); add the phase 0 row to "Gates"; hand over (next: `/sdd-clarify`).
