# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S1 preflight D-01: 2, D-02: 1, D-03: 1
- Result: blocked
- Gate: phase 0 not passed (Critical/High findings D-05 to D-09; Maven scan D-10; Playwright screenshot D-11)
- Notes for the next session: D-01 to D-03 answered (Node 24.13.0 / npm 11.6.2 approved). All 56 npm/Maven pins resolve; patch bumps applied in D-04 (Spring Boot 3.5.16, postgresql 42.7.14, vitest and coverage-v8 3.2.7). Still to re-check after answers: new versions of react-router, vite, stryker, firebase-tools (D-05 to D-09) with OSV and `npm audit` (scratch lockfile in `out/logs/preflight-scan/`), Maven scan (D-10), Playwright screenshot (D-11), then write the input manifest and commit. Details in `docs/00_preflight-report.md`.

## Next
- Command: /preflight D-05: 1, D-06: 1, D-07: 1, D-08: 1, D-09: 1, D-10: 1, D-11: 1 (replace each number by your choice)
- Waiting for: D-05, D-06, D-07, D-08, D-09, D-10, D-11 (blocking); D-04 (non-blocking, pending review)

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 to US-019 | 1 (MVP) | ready | |
| US-020 to US-022 | 2 | ready | |
