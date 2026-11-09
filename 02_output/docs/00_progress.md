# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S3 plan
- Result: done
- Gate: phase 2 passed except the wireframes (they follow in `/wireframes`): 638 of 638 ids map to a section (`out/logs/traceability-check.log`); 4 contracts validate, 40 operations (`contracts-validation.log`); 12 ADRs; both skeletons build and pass check and tests; CI parsed; images built once; seed loaded; 22 stories in exactly one increment, every R, AC and EX has a task (`tasks-check.log`)
- Notes for the next session: Backend commands were run in the pinned `maven:3.9.11-eclipse-temurin-21` image with the Docker socket, not with `./mvnw` on the host (it needed approval), see D-25; the OWASP Maven scan and a real CI run were not done (no runner), syntax only. `docker-compose.prod.yml` is checked only up to its required variables. `scripts/seed.sh` was not run as a script (approval), its two steps were run by hand with the same result and a second run changed nothing. New decisions D-24 (added tools and builder images) and D-25 (planning readings), both non-blocking, pending review. Order of work is in `02_tasks.md`: T-07 History foundation and T-08 version recorder come before the stories that use them (D-22). Wireframes: draw every screen of specification section 9.3 at 375, 768 and 1440 px in the wireframe theme; the shell and tokens already exist. One `.env` value (the Firebase web API key, which is not a secret but is listed in `secrets.env.example`) was echoed once in tool output while checking the generated `config.js`; it was written nowhere.

## Next
- Command: /wireframes
- Waiting for: nothing

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 to US-019 | 1 (MVP) | ready | |
| US-020 to US-022 | 2 | ready | |
