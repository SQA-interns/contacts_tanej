---
name: sdd-fix
description: Session S8, phase 4 or 6. Fix the open findings of a review (argument INC-n) or of the last verification (no argument), one finding per commit with its re-run evidence, without touching frozen tests or anything that is not a finding.
argument-hint: "[INC-n] [answers, e.g. 'D-12: 1']"
disable-model-invocation: true
---

> For: agent · Owner: Team lead · Agent: read-only

## Session

- Id: S8, after `/sdd-review <INC>` with findings (argument `INC-n`, phase 4) or after each `/sdd-verify` with open findings (no argument, phase 6). Frame: generate.
- Reads: the open findings, in `docs/04_review-<INC>.md` with an argument, otherwise in the last section of `docs/06_verification-report.md`; the code, specification sections and rules they cite; the advisory texts in `out/logs/advisories/` for a dependency finding; `general/testing/writing-tests.md`; `general/rules/commits.md`; `general/rules/decisions.md`; for a UI finding `skills/sdd-build-ui/SKILL.md`.
- Writes: production code, non-frozen tests, the resolution column of the report the findings came from, `docs/routes.md`, `docs/decisions-log.md`, `docs/00_progress.md`.
- Must not: change frozen tests; fix anything that is not a finding; look for the hidden tests.
- Exit: every open finding has a fix commit or a decision.

## Procedure

1. Critical and High first. For each finding: a failing test that shows it (red), the fix (green), the full suite and the check that found it. Commit each fix alone (`fix: … (F-nn)`, with its `Red:` line).
2. A hidden-test finding gives only the rule and a case category: re-read the rule, write your own visible test for that category with values from the inputs, and make the rule hold for every such case, not only your test.
3. A finding that needs a change to a frozen test, the specification or an input is a decision; record it and go on. A frozen test that fails only because it waits on a story of a later increment is not a finding to fix: note it and leave it.
4. A dependency advisory: decide reachability from the advisory's preconditions (`out/logs/advisories/`, or fetch it from an allowed advisory site) and evidence from the code or the dependency tree. No advisory text, no acceptance: then it is a decision.
5. Medium and Low: fixed, accepted with a reason (non-blocking decision), or an open item for the release notes.
6. Fill only the resolution column; commit; restart the development stack (`node out/scripts/dev.mjs up`) if production code changed. Hand over: after a review (argument `INC-n`), next is what that review named for a clean increment (the first story of the next increment, or `/sdd-design` after the last one); after a verification, next is `/sdd-verify`.
