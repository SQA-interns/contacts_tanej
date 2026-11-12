# Review of INC-2

> For: agent (writes), human (reads) · Written in: phase 4 (`/review`) · Frame: criticise, no fixes

Range: `bf006a9` (before US-020) to `1bd318c` (HEAD): 6 commits, 33 files under `backend/src/main` and `frontend/src` (about 2,350 lines). Compose stack was up and healthy; I read the merge service and controller in full and the other new sources by diff stat and by name only (see "Limits").

## Result

No Critical or High finding. DoD-I-02 met. DoD-I-01 is not met only because of D-60 (frozen test AC-017-01). Next: the D-60 test fix and manifest update (approved by the developer with this command, see F-01), then `/design`.

## Findings

| F | Severity | Story | Evidence | What is wrong |
|---|---|---|---|---|
| F-01 | Medium | US-017 (found in US-022) | `backend/src/test/java/app/contactgroups/acceptance/Us017SecuredApiAcceptanceIT.java:90` `hasSizeGreaterThan(40)`; `docs/decisions-log.md` D-60 | `AC_017_01` fails on its own number: the contract has exactly 40 operations. The developer approved option 1 with this command (`isGreaterThanOrEqualTo(40)`; the per-operation loop is the real check). **Not applied by this session:** a review must not change tests, and `AGENTS.md` bans edits of frozen files outside an approved session. The next implement or fix session changes line 90, updates the hash of `Us017SecuredApiAcceptanceIT.java` in `docs/03_acceptance-manifest.sha256`, and records the `Human response` in D-60. |
| F-02 | Medium | US-020, US-021, US-022 | `git log bf006a9..HEAD`: `68846a1`, `334e367`, `6dc0d69`, `d85409c`, `1bd318c` carry `Red:` lines that name the tests and their pass counts ("20 of 21 pass", "19 of 19 pass"), not a failing run before the code | Test-first evidence is a claim of tests that exist, not of a red state (DoD-S-02). No way to check afterwards; release-notes item. |
| F-03 | Medium | US-020, US-021, US-022 | commit sizes: `68846a1` 16 files, 2,551 lines; `6dc0d69` 10 files, 1,941; `d85409c` 11 files, 1,792 | Over the 15 files or 400 lines limit of `rules/commits.md`, as in INC-1 (F-07). History cannot be rewritten; release-notes item. |
| F-04 | Medium | US-022 | `docs/wireframes/WF-17_merge_*.png` and the three widths: no screenshot of the merge, duplicates or versions screens exists in `out/logs/screenshots/`; progress note "not done: screenshots and axe scan of the merge screen" | DoD-I-04 (layout per width against the wireframes, UI-22) is not evidenced for WF-12, WF-16, WF-17. Wording and structure are covered by the frozen browser tests (pass in three engines), layout is not. Goes to `/design`. |
| F-05 | Low | US-022 | `D-61 (c)`: WF-17's "at most 5 contacts" message not built; `contracts/schemas.yaml` `MergeRequest.contacts` has `minItems: 2` and no `maxItems` | Wireframe and contract disagree; the backend accepts any number of picks and locks them one by one in one transaction. Not exploitable beyond the request-size limit (413) but a bound would remove the question. Customer/designer to confirm. |
| F-06 | Low | tooling | `backend/pom.xml` (springdoc `-api` instead of `-ui`) is changed and uncommitted, with logs | The dependency change has no decision or commit. The effect is smaller attack surface (no Swagger UI), which is welcome, but the change is a stack item of `project/00_setup/`-class and needs the developer's note. |
| F-07 | Low | US-012 (uncommitted) | `frontend/acceptance/trash-history.spec.ts`: one added `expect(dialog(page)).toHaveCount(0)` plus its manifest line, both uncommitted | A frozen test was edited with its manifest hash and no decision id in the commit; check that a `D-` entry covers it before it is committed. |

## Checks without a finding

- Rules of US-022: `MergeService.merge` takes the oldest contact (ties by id text, D-61b), validates the combined fields on the trusted side (`ContactRules.validate`, line 87), checks Viewer refusal and membership first, one 404 for non-members or other groups (`lock(groupId, id)`), Trash refusal, and the revision of every pick under the row locks in id order (no deadlock between two merges); everything runs in one `@Transactional`, so a refusal changes nothing. The `!=` at line 183 compares a primitive `long` with a `Long`, which unboxes: correct.
- History: the survivor's entry lists merged contacts by id, not by name (BR-06, D-61a); no personal data in logs (the INC-1 request log is unchanged).
- Authorisation (SB-02): `DuplicateController` has no `permitAll`; every operation goes through `AccessService.requireMember`.
- Out of scope: nothing found in `src/main` that no rule asks for; no undo of a merge, no automatic merge.
- Frozen tests: AC-021-05 was changed under D-59 with its manifest hash (`169cc87`), as approved.

## Limits

I did not start the application to exercise the flows, did not take screenshots at 320, 768 and 1280 px, and did not read `VersionService`, `DuplicateDetector`, `DuplicateRepository`, `MergePage.tsx`, `VersionsPage.tsx` or `DuplicatesPage.tsx` line by line; their evidence is the test counts in the commits and the progress notes. Per-rule test mapping was not repeated (see `docs/06_verification-report.md`). The checks of the INC-1 review that were not re-run still stand as written there.
