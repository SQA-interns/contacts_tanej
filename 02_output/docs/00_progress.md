# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 implement US-013
- Result: done (T-41, T-42, T-43 built)
- Gate: not passed (DoD-S-01: Us013 19 of 20 frozen API tests pass; EX-013-03 waits for DELETE /me of US-016; browser AC-013-01, 08 pass in three browsers, AC-013-10 needs the group History tab of US-014)
- Notes for the next session: US-013 built: `HistoryController` (`GET /groups/{g}/history`, `/contacts/{c}/history`, paging only), `HistoryService`, `HistoryEntryView`, `HistoryRepository` page reads ordered by `seq` (migration V2); recorder calls added to `InvitationService` and `MemberService`. Web: `features/history/{ContactHistoryPage,ChangeSetView}`, route `contacts/:id/history`, "History" link on the contact screen. D-43 lists readings.
- Backend run `out/logs/us013-full.log`, static `us013-static.log` (spotless, PMD, CPD clean). Failures left: US-014 (filters, actors, screen), US-016 (DELETE /me: EX-013-03, EX-004-05), US-020 to US-022, OpenApiContractIT drift on `GET /groups/{g}/history` (contract lists the filters and a 400 that US-014 adds: US-014 must add them). Vitest 187 pass, tsc, eslint, prettier clean (history test timed out once under load).
- Re-run after later stories: AC-013-10 browser (US-014), EX-013-03 (US-016), Us015 AC-015-04 now passes with the History endpoint. Older per-story notes: see `git log -p -- 02_output/docs/00_progress.md`.

## Next
- Command: /implement US-014
- Waiting for: nothing

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
| US-014, US-016 | 1 (MVP) | ready | |
| US-020 to US-022 | 2 | ready | |
