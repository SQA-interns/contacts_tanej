# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S4 acceptance-tests
- Result: done
- Gate: phase 3 passed (363 AC and EX each have a test; 226 R and BR each have a hidden test; 8 e2e tests, one per scenario; all tests fail for a behavioural reason; manifests committed last)
- Notes for the next session: Frozen tests: backend `backend/src/test/java/app/contactgroups/acceptance/` (+ `src/test/resources/acceptance/openapi.yaml`, a copy of the contract), browser `frontend/acceptance/` and `frontend/e2e/`; do not edit them (manifest `docs/03_acceptance-manifest.sha256`). Run commands, harness and first-run counts: `docs/03_test-strategy.md`. The browser tests need the stack: `docker compose --env-file ../.env up --build -d` from `02_output/` (it also compiles the backend tests, so they must compile). The tests read the contract's field names; where a story leaves a form open the readings are listed in the strategy file ("Choices where…"). The skeleton check `shell.spec.ts` moved to `frontend/checks/`; the build commit `884425b` (labelled docs) carries that rename by mistake of staging. Hidden suite: `hidden-tests/` (the runner moves it out).

## Next
- Command: /implement US-001
- Waiting for: nothing

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 to US-019 | 1 (MVP) | ready | |
| US-020 to US-022 | 2 | ready | |
