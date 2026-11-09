# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S2 clarify D-04: 1 (developer has no objection)
- Result: blocked
- Gate: phase 1 not passed (D-15 to D-18 open; US-003, US-004, US-013, US-014, US-015, US-016, US-017 not ready; DoR-R-04 fails for US-007)
- Notes for the next session: D-04 answered (option 1). Report is `docs/01_requirements-check.md` (findings F-01 to F-23). Non-blocking, pending review: D-14, D-19 to D-23 (proposed defaults recorded; `/plan` uses them). Blocking, each with a proposed default: D-15 (confirm OQ-004-01 and OQ-016-01), D-16 (what group-level History entries hold about other people), D-17 (Owner access to a group in Trash), D-18 (US-007 in no scenario). Answer with `/clarify D-15: 1, D-16: 1, D-17: 1, D-18: 1` (or other options). Spring Boot 3.5.16 and postgresql 42.7.14 stay (D-04); vitest is 4.1.11 (D-12).

## Next
- Command: /clarify D-15: 1, D-16: 1, D-17: 1, D-18: 1
- Waiting for: D-15, D-16, D-17, D-18

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001, US-002, US-005 to US-012, US-018, US-019 | 1 (MVP) | ready | |
| US-003 | 1 (MVP) | not ready (D-16) | |
| US-004 | 1 (MVP) | not ready (D-15, D-16) | |
| US-013, US-014 | 1 (MVP) | not ready (D-16) | |
| US-015 | 1 (MVP) | not ready (D-17) | |
| US-016 | 1 (MVP) | not ready (D-15, D-16) | |
| US-017 | 1 (MVP) | not ready (D-17) | |
| US-020 to US-022 | 2 | ready | |
