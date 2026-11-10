# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 implement US-001
- Result: done (web app part); 5 frozen browser tests wait for the backend (see notes)
- Gate: not passed (DoD-S-01: AC-001-02, AC-001-03, AC-001-04, AC-001-08, EX-001-07 need `GET /groups` and `POST /groups`, which the backend does not have yet: US-017 and US-002)
- Notes for the next session: US-001 is entirely web app (T-01, T-02; the backend has no controller yet). Browser run of `acceptance/us001-sign-in.spec.ts`: 10 of 15 pass (log `out/logs/us001-acceptance-run2.log`). The 5 that fail all stop at the groups API: AC-001-02 sets the scene with `POST /groups` (500), the other four wait for the empty state, which asks `GET /groups` (the screen shows the error state until the endpoint exists). Run them again after US-017/US-002 and expect them to pass without a change to the web app. The leftovers of runs 013/014 were checked (25 tests, lint, types clean) and committed. D-28: the web server's CSP now allows `https://apis.google.com` (Google sign-in script). The empty state's "Create group" button has no action until US-002 adds the dialog (`EmptyState onCreate`). `GroupsHome` shows a plain name list until US-002. `checks/shell.spec.ts` now scans the three sign-in screens (screens in `out/logs/screens/`, no Critical or Serious violation, no overflow). Test helpers: `src/test/fakeAuth.ts`, `src/test/renderApp.tsx`.

## Next
- Command: /implement US-017
- Waiting for: nothing

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 | 1 (MVP) | in progress (web app done; 5 frozen browser tests wait for US-017, US-002) | DoD-S-01 not met; 02 to 07 met |
| US-002 to US-019 | 1 (MVP) | ready | |
| US-020 to US-022 | 2 | ready | |
