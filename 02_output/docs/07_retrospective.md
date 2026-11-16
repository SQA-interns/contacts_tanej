# Retrospective

> For: agent (writes), human (reads) · Written in: phase 7 (`/release`) · Source: `docs/decisions-log.md`, `docs/04_review-*.md`, `docs/06_verification-report.md`, `docs/00_progress.md` · Agent: writes

## What went well

- **Inputs were traceable.** Every R, AC, EX, BR, SR, SB and NFR is mapped in the specification and checked by `check-traceability.mjs`; the three manifests (input 73, acceptance 43, hidden 12 files) matched in every verify run, so no run changed its own judge.
- **The test-first split held.** Frozen acceptance tests existed before the code; 22 stories in two increments reached backend line coverage 97 % and mutation 91 %, frontend line coverage 97 %.
- **Blocking questions stopped the run instead of being guessed** (D-04 to D-13 for versions; D-59, D-60, D-70, D-71 for approvals). Block topics (18 decisions) were not decided by the agent.
- **Security evidence is concrete.** Each of the 12 Spring CVEs was accepted only after a code and dependency-tree check of its precondition (D-70); the production set was built, started and probed live (verify run 5).
- **Non-functional claims have logs:** restart, backup-restore, clean-host and load tests each left a log in `out/logs/`.

## What held the run back

1. **Frozen tests with defects cost many fix rounds.** D-31, D-50, D-51, D-52, D-53, D-57, D-59 and D-60 are frozen tests that could not pass as written (a wrong operation count, a role-label clash, a race on sign-in, a contact BR-05 forbids). Each needed a human approval and a manifest change. 13 `/fix` sessions ran in total.
2. **The hidden suite gave no usable signal.** F-V01 stayed open in five verify runs (62, 47, then 17 failures three times); D-68 shows the category list could not lead to a cause and the hidden tests may not be read. The run ended by acceptance (D-71), not by understanding.
3. **The pinned dependency versions already had advisories.** D-04 to D-13, D-56 and D-70 (14 decisions) concern advisories in versions of `tech-stack.md`; the real fix (Spring Boot 4) is outside the stack, so 12 CVEs were accepted.
4. **Long and host-level jobs did not fit the agent's limits.** Backend PIT exceeded 2 h (D-65), the full Stryker run did not finish (D-64), restart, restore and clean-host scripts waited for the developer in runs 1 to 4. The frontend mutation target of 90 % was not met (D-66, about 77.9 %) after three survivor rounds.
5. **Environment friction.** Host Node, Java and Docker versions differed from the pins (D-01 to D-03); JaCoCo failed with "malformed input" in the Docker run (F-V09); Playwright and Vitest flake under load (F-V07, D-69); 38 input files have CRLF endings on some clones (F-V12).
6. **The second increment was built after the first verification loop** (D-55): its frozen and hidden tests failed until `/implement` ran, which inflated early findings.
7. **Commit rules were broken in INC-1** (review F-06, F-07: missing Red lines, 24 long subjects, 18 oversized commits) and cannot be repaired afterwards.
8. **Customer confirmations never arrived** (D-14, D-49): the MVP rests on developer-confirmed readings; about 30 decisions stay "pending review" and the product name is a working name.
9. **No real Firebase project:** sign-in and the core flows on the production configuration are untested (DoD-P06, DoD-P09 partly met).

## Improvements

| # | Improvement | For (project input or template rule / skill) | Evidence |
|---|---|---|---|
| 1 | Have `/acceptance-tests` check each frozen test against the specification before the freeze (counts, labels, data rules such as BR-05) or run it once against a stub of the contract. | Template: `/acceptance-tests` skill | D-31, D-50 to D-53, D-57, D-59, D-60; 13 fix sessions |
| 2 | Make the hidden evaluation report, for every failure, the rule id and the input sentence it derives from (not the test code), so a failure can be analysed or the test judged wrong. | Template: runner and the `06_hidden-evaluation.md` format | D-68, D-71; F-V01 open in five runs |
| 3 | Check the pinned versions against advisories before `tech-stack.md` is frozen and prefer versions that still have an open-source fix line. | Project: `tech-stack.md`; template: `/preflight` | D-04 to D-13, D-56, D-70 |
| 4 | Plan long jobs as developer-run steps from the start (PIT, full Stryker, the three scripts, each with command and report path), and set mutation targets per layer from a first measured baseline. | Template: `/quality-gate` and `/verify` skills; project: `quality-requirements.md` | D-64, D-65, D-66; verify runs 1 to 4 |
| 5 | Obtain the customer's answers (OQ-P-04 to OQ-P-08, product name) and a real Firebase development project before `/to-spec`; set `eol=lf` for `01_input/` in `.gitattributes`. | Project inputs | D-14, D-49, F-V12, DoD-P06 |

## Numbers

Written by `system/run.mjs --summary`.

| Measure | Value |
|---|---|
| Sessions started | 72 |
| Sessions failed (exit code) | 2 |
| Sessions per skill | /to-spec 2, /preflight 4, /clarify 2, /plan 1, /wireframes 2, /acceptance-tests 1, /implement 35, /review 2, /fix 13, /verify 5, /design 1, /quality-gate 4 |
| Cost reported by the agents (USD) | 107.11 |
| Decisions | 71 |
| Decisions by class (allow / open / block) | 6 / 14 / 18 |
| Approvals | 2 |
| Verification runs | 5 |
| Commits | 226 |
