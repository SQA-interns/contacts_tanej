# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S8 fix F-04 D-53: 1
- Result: done
- Gate: not passed (DoD-I-02: INC-2 tests of versions-duplicates and E2E-06 still fail as expected; not re-run in full)
- Notes for the next session: D-53 answered 1 and applied: frozen-test fixes in AC-018-01, EX-018-02, AC-001-04, AC-003-08; manifest updated; these four pass 3 of 3 in Chromium. Run `/verify` for the full run (Firefox/WebKit too). Older: Full Chromium run `out/logs/fix-f04b-full-chromium.log`: 118 passed, 15 failed (AC-001-04, AC-018-01, EX-018-02, 11 INC-2 tests of versions-duplicates, E2E-06); AC-003-08 passed there but is flaky. Vitest 207 passed. D-52: AC-016-05 `exact: true` (manifest updated), invite Role label stays exactly "Role" with a visible `*` outside the label. D-54 (app fixes): stale ID token after verifying in another tab (EX-001-07), visible attribute-key suggestion listbox instead of a datalist (AC-008-03), nginx `CSP_FRAME_EXTRA` so the emulator Google popup works (AC-001-04 app side). D-53 (waits for the developer, `/fix F-04 D-53: 1`): test changes for AC-018-01 (password counts as a textbox), EX-018-02 (modal dialog hides the SL button), AC-003-08 (no verification mail created), AC-001-04 (popup click race). Rebuilt frontend only; Firefox/WebKit not run. Older: D-51 answered 1 with a change: `signIn` in `acceptance/support.ts` now waits for User menu, Verify heading or alert; manifest updated. Chromium re-run (`out/logs/fix-f04-signin-chromium.log`): 103 passed, 21 failed. INC-2, not INC-1 defects: versions-duplicates.spec.ts (AC-020/021/022, 12 tests) and E2E-06. Still failing in INC-1, not fixed: AC-016-05 (frozen test heading 'Account' strict-mode, test defect, needs a decision), AC-008-03, AC-001-04, EX-001-07, AC-018-01, EX-018-02 (not analysed), AC-003-08 (flaky: "No verification e-mail"). The D-50 `exact: true` change did not work (label was "Role (required)"); fixed in the app instead (Role label exactly "Role", `required`); AC-003-01 and E2E-02 pass now. Rebuilt frontend only; Firefox/WebKit not run. Delete the untracked `frontend/e2e/tmp-f04.spec.ts` (note: it is in the logs run too). Older: F-04 has four causes (D-51). Fixed in the app: the group tab bar now also shows on a contact page (without the group History tab, so a contact page has one "History" link; red test in `contact-history.test.tsx`), and the group History reads "N contacts exported" for an `EXPORTED` entry (red test in `group-history.test.tsx`; plural keys in en and sl). Vitest 203 passed; E2E-03 passes in Chromium on the rebuilt stack (not run in Firefox and WebKit). Not an app defect: E2E-08 (the frozen `signIn` returns before the sign-in ends, then `goto` cuts it off; same as AC-002-03, D-31) and E2E-06 (Duplicates tab is US-021, second increment). D-51 asks the developer to approve a change to the frozen `acceptance/support.ts` (`signIn` waits for "User menu") and to update the manifest; answer with `/fix F-04 D-51: 1`. Same run also failed in Chromium: AC-003-01 (selectOption timeout after the D-50 change), AC-003-08 (D-32), AC-008-03, AC-016-05, AC-012-03; not analysed, not caused by F-04. An untracked `frontend/e2e/tmp-f04.spec.ts` (my temporary copy of E2E-08) could not be deleted by the session: delete it. Earlier: F-03 fixed as a human-approved test-defect change (D-50 option 1): `getByLabel('Role', { exact: true })` in E2E-02 and AC-003-01 only; manifest lines updated; not re-run in the browser (needs the Compose stack), `/verify` does that. Earlier note, now resolved: F-03 was a conflict between frozen tests (E2E-02 and AC-003-01 page-wide `getByLabel('Role')` against E2E-07 row-scoped `getByLabel('Role')`); no code changed; D-50 asks the developer to approve option 1 (scope the invite selects in E2E-02/AC-003-01, update the acceptance manifest). F-02 fixed in 19f6efc (red: `mobile-layout.test.tsx`; vitest 201 passed; `e2e/responsive.spec.ts` 6 passed in Chromium at 320/768/1280 px against the dev server, not the other engines). Groups, members and Trash tables are cards below 768 px via `components/ui/stacked-table.ts`; the group tab row wraps (D-27 option 1). Next F-03, F-04. F-01 fixed in b717413 (red: create case in contact-page.test.tsx; vitest 199 passed with `--maxWorkers=2 --testTimeout=30000`, at default workers a few tests hit the 5 s timeout under load). Frozen E2E-01/03 not re-run in the browser (needs the Compose stack). D-49 records the developer's note. Next F-02, F-03, F-04 (see below). Review text: 4 High (F-01 fixed; F-02 320 px overflow and table instead of cards; F-03 E2E-02 `Role` label match; F-04 "Group not found" heading and "Group" navigation). Per D-48 the next session is `/fix`: check each failing E2E for a test defect first and bring test changes to the developer as decisions. Medium: F-05 WebKit focus, F-06 missing `Red:` lines, F-07 commit size and subject length (release notes), F-08 deleted_accounts cleanup and AC-016 browser tests, F-09 JaCoCo.

## Previous
- Session: S5 implement T-N04 (log commit after the developer's re-run of 2026-11-10, about 19:18 UTC)
- Result: done
- Gate: not applicable (NFR tasks)
- T-N04: `restore.sh` PASS (NFR-06): 15 tables, identical row counts and checksums (`out/logs/backup-restore-test.log`); the source held only 1 group and 4 contacts. The `backups/` dump is not committed (in `.gitignore`).
- T-N10: `clean-host-test.sh`: stack built from 723 committed files plus `.env`, healthy (backend, db, emulator, frontend); E2E-01 failed only at `getByText('The contact was saved.')`, the known D-48 failure (`out/logs/clean-host-test.log`). NFR-12 counts as met up to E2E-01; re-run the script after `/fix`.
- Earlier: T-N03, T-N11, T-N12 done; T-N05 to T-N09 specs run, findings in D-48 (answered 1: `/fix` after `/review INC-1`; check the failing E2E for test defects first, test changes go to the developer as decisions; NFR-09 focus check in `/verify`).
- Still open: the install guide text for the daily backup and 14-day retention (phase 7); the manual Safari/Edge check (NFR-07).
- Previous: S5 implement T-N01 (readings in D-46)
- Gate: not applicable (NFR tasks). NFR-01, NFR-02, NFR-04, DoD-P03, EX-009-08, EX-010-07 pass in `out/logs/perf-load-test.log`.
- Notes for the next session: `node scripts/perf/load-test.mjs [--duration 60] [--skip-seed]` needs the Compose stack up; it creates 100 emulator accounts (perf-user-001..100) and loads `scripts/perf/seed-10000.sql` (group a0000000-...-0 with 10,000 contacts, four groups of 250). Watch item in D-46: the two-word search with accents takes about 550 to 700 ms.
- Browser tests AC-016-02, 05, 06 (`acceptance/groups-members.spec.ts`) are still not run; run them in `/review` or the NFR tasks. Cleanup of old `deleted_accounts` rows is open (ADR-08).
- JaCoCo report generation fails with "malformed input" in the Docker run; spotless, PMD, CPD run separately.
- Do not put "to", "from" or similar words in `aria-label`s on the History screen: the frozen tests use `getByLabel('To')`, which matches by substring.
- Older per-story notes: see `git log -p -- 02_output/docs/00_progress.md`.

## Next
- Command: /verify
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
| US-014 | 1 (MVP) | done (D-44 resolved; browser tests of the story passed in the previous session) | DoD-S-01 to 07 met |
| US-016 | 1 (MVP) | done (browser AC-016-02, 05, 06 not run) | DoD-S-01 met for the API; 02 to 07 met |
| US-020 to US-022 | 2 | ready | |
