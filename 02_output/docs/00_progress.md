# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S6 quality-gate resume
- Result: blocked
- Gate: phase 5 not passed (mutation thresholds not measured: backend D-63, frontend D-64). Line gate met: backend 97.05 % (JaCoCo, all 610 tests pass), frontend 95.63 % (unit only, 243 tests, 11 added, commit 7020545).
- Notes for the next session: Backend PIT ran 0 tests (948 mutants all NO_COVERAGE) because `pitest-junit5-plugin` is missing; it is not in `tech-stack.md`, so adding it needs approval (D-63, proposed: approve). Then run `mvn org.pitest:pitest-maven:mutationCoverage` once (scope and exclusions already committed, `build:` commit; full suite takes long, use a runner, not an agent background command). Frontend Stryker was stopped at the 2 h limit, no report (D-64). The earlier JaCoCo "malformed input" (F-V09) was a corrupt `target/jacoco.exec`; `mvn clean verify` fixed it. `pom.xml` still has the uncommitted springdoc change (F-06); E2E-06 flake and F-07 are untouched. Figures are in `docs/03_test-strategy.md` (phase 5 tables). Waiting on the human: answer D-63 and D-64, then run `/quality-gate D-63: 1` (or the chosen options).
- Previous session: D design (Design round: skip, baseline final theme, D-62). Gate then: design exit met: final theme applied (`data-theme="final"` in `index.html`, the only code change), 108 captures per phase at the three widths, light and dark, no Critical or Serious axe violation and no overflow; DoD-I-04 (WF-12, WF-16, WF-17) met. Full suite: 232 of 232 unit tests pass (3 and 1 timing flakes in two earlier runs of identical code); Chromium acceptance and e2e 122 of 123, the one failure is E2E-06 (see notes). Firefox and WebKit not re-run.
- Notes for the next session: Screenshots: `out/logs/screens/design-1/{before,after}/` with `axe-summary.json`. E2E-06 `e2e-06-duplicates-and-merge.spec.ts:36` is intermittent (1 of 4 runs failed: after "Confirm merge" the test switches tab at once and the Contacts list shows no Novak row); presentation cannot cause it, it looks like a race between the merge and the tab switch; frozen test, not touched, needs analysis in `/quality-gate` or `/fix` (a test change goes to the developer). Still open and untouched: F-06 (`pom.xml` springdoc change uncommitted) and F-07 (edited `trash-history.spec.ts` with its manifest line, uncommitted: needs a decision id). The compose frontend image was rebuilt with the final theme.
- Previous session: S5 implement US-022 note: apply D-60 (done; AC-017-01 test fixed as approved, D-60). Before: R review INC-2 (report `docs/04_review-INC-2.md`). Before: S5 implement US-022 (D-59 applied earlier, commit 169cc87). US-022 built (T-52, T-53). Backend: `MergeService`, `MergeCombiner`, `MergeRepository`, `POST /merges` in `DuplicateController`; History of a contact now also lists the entries of contacts merged into it (`linkedFrom`). `Us022MergeAcceptanceIT` 19 of 19, `MergeCombinerTest` 6; full backend run has one failure, `Us017SecuredApiAcceptanceIT.AC_017_01`, a defect of the frozen test (expects more than 40 operations, the contract has 40): D-60 waits for the developer (approval). Spotless, PMD, CPD clean. Web: `MergePage.tsx` (route `duplicates/merge?ids=`), Merge button and tick boxes on the set rows, en and sl texts; 232 unit tests pass; the frozen versions-duplicates browser tests (incl. 4 AC-022) pass in Chromium, Firefox, WebKit. Not done: screenshots at three widths and the axe scan of the merge screen (do in `/design` or `/review`). Assumptions in D-61. Uncommitted before this session and left alone: `backend/pom.xml` (springdoc api instead of ui), `trash-history.spec.ts` plus its manifest line, logs. Earlier: S5 implement US-021 (T-50, T-51). Backend: `DuplicateDetector` (pure, union-find on hash maps), `DuplicateService`, `DuplicateRepository`, `DuplicateController` (`/duplicates/search`, `/duplicates/dismissals`); `Us021DuplicatesAcceptanceIT` 20 of 21 pass; AC-021-05 cannot create phone-only contacts (BR-05), frozen test not changed: D-59 waits for the developer (approval). Web: `features/duplicates/DuplicatesPage.tsx`, tab "Duplicates" (Owner, Editor), route, en and sl texts; 7 new unit tests (223 pass); the 3 frozen AC-021 browser tests pass in Chromium, Firefox, WebKit. No "Merge" button yet: `/implement US-022` adds it to the set rows (WF-16) and the merge screen; the frozen merge browser tests start from it. Full backend run: 21 failures, all from the missing `POST /merges` (US-022, US-017 AC-017-01 and AC-017-11, US-020 EX-020-05) and D-59. The compose stack was rebuilt for this run. Earlier: S5 implement US-020 (D-58) (T-48, T-49). Backend: `VersionService.get/restore`, `VersionController` (GET one, POST restore), `VersionRepository.find`; `Us020VersionRestoreAcceptanceIT` 13 of 14 pass, EX-020-05 waits for `POST /merges` (US-022, T-52). Web: `features/versions/*` (list, read-only view, compare, restore dialog), tab link on the contact page, routes, en and sl texts; the 6 frozen AC-020 browser tests pass in Chromium, Firefox, WebKit; 216 unit tests pass. D-58: History entry names the number as field `version`; contract `VersionDetail.snapshot` lacks `tags` and `attributes` (not changed, input side); OQ-020-01 still open. The compose stack was rebuilt for this run. Run Playwright with 1 worker. Earlier: S8 fix F-V13 F-V06 D-56 D-57 (loop 1 results in `docs/06_verification-report.md`).

## Previous
- Session: S7 verify run 1 (blocked; report `docs/06_verification-report.md` "Run 1"; not run for lack of approval: restart-test.sh, restore.sh, clean-host-test.sh; frontend lines 94.61 %, backend JaCoCo "malformed input" F-V09)
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
- Command: /quality-gate D-63: 1
- Waiting for: D-63, D-64

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
| US-020 | 2 | done (EX-020-05 frozen API test waits for US-022 merge) | DoD-S-01 met except EX-020-05; 02 to 07 met |
| US-021 | 2 | done (D-59 applied: AC-021-05 now asserts the BR-05 refusal) | DoD-S-01 to 07 met |
| US-022 | 2 | done (screenshots and axe scan of the merge screen still to do) | DoD-S-01 to 07 met (D-60 applied) |
