# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S1 preflight D-05: 1, D-06: 1, D-07: 1, D-08: 2 (fallback 1), D-09: 1, D-10: 1, D-11: 1
- Result: blocked
- Gate: phase 0 not passed (Critical: vitest via tinypool, D-12; Critical/High transitive Maven, D-13)
- Notes for the next session: D-05 to D-11 answered and recorded. Applied: react-router 7.18.5, vite 7.3.8, stryker 9.6.1, firebase-tools 15.35.0; firebase 12.3.0 kept, High recorded as Medium (D-08, `/verify` re-checks the bundle). Playwright browsers installed, screenshots saved (D-11). Maven OWASP scan failed (NVD 524, no API key); OSV on all 130 resolved artifacts replaced it (D-10). Still to re-check after D-12 and D-13: `npm audit` on `out/logs/preflight-scan2/package.json` and OSV on `out/logs/preflight-scan-maven/deps.txt` (re-run `osv.mjs`), then write the input manifest and commit. Details in `docs/00_preflight-report.md`.

## Next
- Command: /preflight D-12: 1, D-13: 1 (replace each number by your choice)
- Waiting for: D-12, D-13 (blocking); D-04 (non-blocking, pending review)

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 to US-019 | 1 (MVP) | ready | |
| US-020 to US-022 | 2 | ready | |
