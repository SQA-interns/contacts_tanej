# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S1 preflight
- Result: blocked
- Gate: phase 0 not passed (host Node/npm major mismatch D-01; dependency scan and most dependency resolution not yet run)
- Notes for the next session: Done and passing: `.env` keys non-empty, working tree clean, Docker daemon up, images postgres/temurin/nginx/gitleaks resolve, react resolves. Host: Java 21.0.11 Oracle, Node 24.13.0, npm 11.6.2, Docker 29.9.3; no host postgres. Remaining: Maven Central and npm pins, Playwright screenshot, dependency scan, input manifest. Details in `docs/00_preflight-report.md`, decisions D-01 to D-03.

## Next
- Command: /preflight D-01: <1|2|3>
- Waiting for: D-01 (blocking); D-02, D-03 (non-blocking, pending review)

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 to US-019 | 1 (MVP) | ready | |
| US-020 to US-022 | 2 | ready | |
