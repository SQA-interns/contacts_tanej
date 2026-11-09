# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: T to-spec resume
- Result: done
- Gate: intake (DoR-S, DoR-R) passed; open items are listed in the review checklist, not resolved
- Notes for the next session: All project files in `project/` are filled (intent, 22 stories, business rules, e2e scenarios, design, setup). `project/00_intake/review-checklist.md` lists every `[ASSUMPTION]`, `OPEN` and developer-sourced item; Block topics stay `OPEN — needs customer`. Tech-stack versions are unverified (`/preflight` verifies). The developer review (with `/run`) comes before `/preflight`.

## Next
- Command: /preflight
- Waiting for: review of `project/00_intake/review-checklist.md`

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 to US-019 | 1 (MVP) | ready | |
| US-020 to US-022 | 2 | ready | |
