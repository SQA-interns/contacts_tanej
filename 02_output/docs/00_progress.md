# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S2 clarify D-15: 1, D-16: 2, D-17: 1, D-18: 1
- Result: done
- Gate: phase 1 passed
- Notes for the next session: D-15 to D-18 answered. D-16 option 2: group-level History entries hold event type, actor (name, e-mail) and role; invitation entries never hold the invited e-mail; members as subject are named by name; deleting an account removes its e-mail from every entry where it is actor or subject (name stays). D-17: an Owner of a group in Trash sees only name, deletion time, days left and can restore; all other requests answer 404. D-18: US-007 and US-018 verified by acceptance tests (approved DoR-R-04 exception). Non-blocking, pending review, `/plan` uses the proposed defaults: D-14 (now also OQ-004-01, OQ-016-01), D-19 to D-23. Spring Boot 3.5.16, postgresql 42.7.14, vitest 4.1.11, Stryker 10.0.0; pom sets tomcat.version 10.1.59 and jackson-bom.version 2.21.7 (D-13).

## Next
- Command: /plan
- Waiting for: nothing

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 to US-019 | 1 (MVP) | ready | |
| US-020 to US-022 | 2 | ready | |
