# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 implement US-012
- Result: done (T-33, T-34, T-35 built)
- Gate: not passed (DoD-S-01: Us012 15 of 16 frozen API tests pass, EX-012-07 needs the History endpoint of US-013; browser AC-012-01..04, 08, 09 pass in 3 browsers, AC-012-03 failed once in chromium under load and passed on re-run)
- Notes for the next session: US-012 built: `TrashController` (`POST /contacts/{id}/trash|restore`, `DELETE /contacts/{id}`, `GET /trash-contacts`), `TrashService`, `TrashCleanupService` (`@Scheduled`, `run()` erases contacts in Trash 30 days; groups, invitations, tombstones still to add with US-015, US-003, US-016), `ContactRepository.trash/restore/delete/pageTrashed/expired`, `HistoryRecorder.erase` + `SYSTEM_ACTOR` for actor-less entries, `HistoryRepository.anonymiseContact`, `TagRepository.deleteUnused`. Web: `features/contacts/TrashPage`, "Delete contact" on the contact screen, Trash tab for Owner and Editor. D-40 lists readings.
- Backend run `out/logs/us012-backend.log` (Us009 19, Us010 17, TrashIT 5, ContactsIT 9, OpenApiContractIT, IsolationMatrixIT, Architecture pass; Us013 20 fail: US-013 not built); `us012-static.log` clean (spotless, PMD, CPD). Vitest 165 pass (trash.test.tsx 5), tsc, eslint, prettier clean. Browser log not kept; rerun `acceptance/trash-history.spec.ts -g AC-012` if in doubt.
- Re-run after later stories: Us012 EX-012-07 (US-013); Us009 AC-009-08 and Us010 EX-010-04 now pass; Us005 AC-005-09, EX-005-12 and the US-006, US-007, US-008 browser tests that need the list; E2E-03/04/05 need export and History. Older per-story notes: see `git log -p -- 02_output/docs/00_progress.md`.

## Next
- Command: /implement US-011
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
| US-011, US-013 to US-016 | 1 (MVP) | ready | |
| US-020 to US-022 | 2 | ready | |
