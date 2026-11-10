# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 implement T-N04 to T-N12 (T-N02 is the label of the session that runs T-N03 to T-N12)
- Result: blocked on the developer for T-N04 and T-N10 (scripts written, not run); NFR findings open in D-48.
- Done: T-N03 (passing restart run committed), T-N11 (`MigrationIT`, 4 tests pass), T-N12 (`scripts/licence-check.mjs` passes, CI step added, D-47). Specs written and run: T-N05 (E2E in 3 engines: E2E-01, 02, 03, 06, 08 fail in all three, so NFR-07 not met), T-N06 (NFR-08 fails at 320 px, passes at 768 and 1280), T-N07 (axe clean everywhere, WebKit keyboard focus fails at Tab stop 4), T-N08 (passes), T-N09 (first contact 2.7 s, empty and error states pass). Details and suggested severities: D-48.
- For the developer, from `02_output/` (Git Bash), : T-N04 (stack up): `./scripts/backup.sh` then `./scripts/restore.sh` (log `logs/backup-restore-test.log`). T-N10: `docker compose --env-file ../.env down` then `./scripts/clean-host-test.sh` (log `logs/clean-host-test.log`), then `docker compose --env-file ../.env up -d` again.
- Still open: the install guide text for the daily backup and 14-day retention (phase 7); the manual Safari/Edge check (NFR-07).
- Previous: S5 implement T-N01 (readings in D-46)
- Gate: not applicable (NFR tasks). NFR-01, NFR-02, NFR-04, DoD-P03, EX-009-08, EX-010-07 pass in `out/logs/perf-load-test.log`.
- Notes for the next session: `node scripts/perf/load-test.mjs [--duration 60] [--skip-seed]` needs the Compose stack up; it creates 100 emulator accounts (perf-user-001..100) and loads `scripts/perf/seed-10000.sql` (group a0000000-...-0 with 10,000 contacts, four groups of 250). Watch item in D-46: the two-word search with accents takes about 550 to 700 ms.
- Browser tests AC-016-02, 05, 06 (`acceptance/groups-members.spec.ts`) are still not run; run them in `/review` or the NFR tasks. Cleanup of old `deleted_accounts` rows is open (ADR-08).
- JaCoCo report generation fails with "malformed input" in the Docker run; spotless, PMD, CPD run separately.
- Do not put "to", "from" or similar words in `aria-label`s on the History screen: the frozen tests use `getByLabel('To')`, which matches by substring.
- Older per-story notes: see `git log -p -- 02_output/docs/00_progress.md`.

## Next
- Command: /review INC-1 (after the developer has run T-N04 and T-N10 and given the results; if one fails: /implement T-N04 or T-N10 with the log)
- Waiting for: the developer's run of `scripts/backup.sh` + `scripts/restore.sh` (T-N04) and `scripts/clean-host-test.sh` (T-N10); D-48 for the fix session

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 | 1 (MVP) | in progress (web app done; 5 frozen browser tests wait for US-002) | DoD-S-01 not met; 02 to 07 met |
| US-017 | 1 (MVP) | in progress (T-03 to T-06 done; frozen tests wait for US-002 and later stories) | DoD-S-01 not met; 02 to 07 met |
| US-018 | 1 (MVP) | in progress (T-09 done; frozen browser tests wait for later screens) | DoD-S-01 not met; 02 to 07 met |
| US-002 | 1 (MVP) | in progress (T-07, T-10 to T-12 done; 4 frozen API tests wait for US-003, US-009, US-015; browser AC-002-03 D-31) | DoD-S-01 not met; 02 to 07 met |
| US-003 | 1 (MVP) | in progress (T-13 to T-15 done; browser AC-003-08 D-32) | DoD-S-01 not met; 02 to 07 met |
| US-004 | 1 (MVP) | in progress (T-16, T-17 done; frozen AC-004-01, AC-004-02 wait on US-005, EX-004-05 on US-016; browser tests not run) | DoD-S-01 not met; 02 to 07 met |
| US-005 | 1 (MVP) | in progress (T-08 minimum, T-18 to T-20 done; frozen tests wait for US-013, US-019; browser tests not run) | DoD-S-01 not met; 02 to 07 met |
| US-006 | 1 (MVP) | in progress (T-21, T-22 done; browser tests not run) | DoD-S-01 not met; 02 to 07 met |
| US-008 | 1 (MVP) | in progress (T-23, T-24 done; browser AC-008-02, 03, 07 not run) | DoD-S-01 not met; 02 to 07 met |
| US-019 | 1 (MVP) | in progress (T-08, T-25 done; frozen API tests wait for US-007, US-016) | DoD-S-01 not met; 02 to 07 met |
| US-007 | 1 (MVP) | in progress (T-26, T-27 done; frozen API tests wait for US-013; browser tests not run) | DoD-S-01 not met; 02 to 07 met |
| US-009 | 1 (MVP) | in progress (T-28 to T-30 done; EX-009-08 timing in the NFR tasks) | DoD-S-01 not met; 02 to 07 met |
| US-010 | 1 (MVP) | in progress (T-31, T-32 done; browser AC-010-08, 09, 10 pass) | DoD-S-01 not met; 02 to 07 met |
| US-012 | 1 (MVP) | in progress (T-33 to T-35 done; EX-012-07 waits on US-013) | DoD-S-01 not met; 02 to 07 met |
| US-011 | 1 (MVP) | in progress (T-36, T-37 done; browser AC-011-01, 05, 06, 07, 09, 10 not run; AC-013-05 waits on US-013) | DoD-S-01 not met; 02 to 07 met |
| US-015 | 1 (MVP) | in progress (T-38 to T-40 done; AC-015-04 waits on US-013; browser AC-015-01, 02 not run) | DoD-S-01 not met; 02 to 07 met |
| US-013 | 1 (MVP) | in progress (T-41 to T-43 done; EX-013-03 waits on US-016; browser AC-013-01, 08 pass, AC-013-10 waits on US-014) | DoD-S-01 not met; 02 to 07 met |
| US-014 | 1 (MVP) | done (D-44 resolved; browser tests of the story passed in the previous session) | DoD-S-01 to 07 met |
| US-016 | 1 (MVP) | done (browser AC-016-02, 05, 06 not run) | DoD-S-01 met for the API; 02 to 07 met |
| US-020 to US-022 | 2 | ready | |
