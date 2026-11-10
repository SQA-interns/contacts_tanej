# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 implement US-015
- Result: done (T-38, T-39, T-40 built)
- Gate: not passed (DoD-S-01: Us015 13 of 14 frozen API tests pass, AC-015-04 waits for the History endpoint of US-013; browser AC-015-01, 02 not run in this session)
- Notes for the next session: US-015 built: `GroupController` (`POST /groups/{g}/trash`, `/restore`, `GET /deleted-groups`), `GroupService` (trash, restore, deletedFor, purgeExpired), `GroupRepository`, `domain/DeletedGroup`, `GroupAccess`; `TrashCleanupService` purges groups after contacts. Web: `DeleteGroupDialog`, `DeletedGroupsPage`, route `/deleted-groups`, "Delete group" button in the groups list. D-42 lists readings. Also fixed the 429 drift of `ExportController` (US-011).
- Backend run `out/logs/us015-full.log` (487 tests, 103 fail) and `us015-static.log`: own tests DeletedGroupTest 3, GroupTrashIT 11, OpenApiContractIT, IsolationMatrixIT pass; all failures are the frozen tests of US-013, US-014, US-016, US-020 to US-022 and the History or DELETE /me parts of earlier stories. spotless, PMD, CPD clean. Vitest 181 pass (the export test timed out once under load, passes alone), tsc, eslint, prettier clean. Run `acceptance/groups-members.spec.ts -g AC-015` with the stack up.
- Re-run after later stories: Us015 AC-015-04 (US-013), plus the older items of US-011. Older per-story notes: see `git log -p -- 02_output/docs/00_progress.md`.

## Next
- Command: /implement US-013
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
| US-013, US-014, US-016 | 1 (MVP) | ready | |
| US-020 to US-022 | 2 | ready | |
