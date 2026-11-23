---
name: sdd-implement
description: Session S5, phase 4. Implement one user story (or one task group of it) test-first - for each task red, green, refactor - build its screens to the approved wireframes, keep every check clean, and finish only when the story meets the story Definition of Done.
argument-hint: "US-nnn [T-nn..T-nn] [note: …]"
disable-model-invocation: true
---

> For: agent · Owner: Team lead · Agent: read-only

## Session

- Id: S5, phase 4, once per story or task group. Frame: generate.
- Reads: the story file; its tasks in `docs/02_tasks.md`; the specification sections and contracts it cites; the story's frozen tests; findings addressed to it in `docs/04_review-*.md`; `project/01_requirements/business-rules.md`; `general/engineering-standards.md`; `general/testing/writing-tests.md`; `general/quality/done-story.md`; `general/rules/commits.md`; `general/rules/decisions.md`. For a story with UI also `skills/sdd-build-ui/SKILL.md` and what it lists.
- Writes: production code, unit and integration tests, `docs/routes.md`, `docs/decisions-log.md`, `docs/00_progress.md`.
- Must not: change frozen tests; touch another story's code beyond its tasks; change the specification (a needed change is a decision); write production code without a failing test that needs it. *Why:* every line then has a reason and a test.
- Exit: the story meets `DoD-S` (for a task group: its tasks are done and all checks are clean).

## Procedure

1. Confirm in `docs/00_progress.md` that this story is next and the previous one is done. Fix the review findings addressed to the story first, each as its own `fix:` commit.
2. Run the full suite once; note which frozen tests of the story fail: they are the target.
3. For each task (of the group, if given) and each rule it covers:
   - **Red:** write a unit or integration test for the rule; it must fail for the expected reason.
   - **Green:** write the least production code that makes it pass.
   - **Refactor:** remove duplication, improve names, keep functions short (ES-11); all tests stay green.
   - Commit (`feat:` with its `Red:` lines, or `refactor:`), within the size limits.
   This holds for screens as well: a component test that fails before the component exists. A `Red:` line names a failure you saw in this session.
4. For a story with UI, build each screen with `skills/sdd-build-ui`.
5. When the story's frozen tests pass: run the full suite, then format, lint, type check, static analysis and the secret scan on the changed code; fix what they report.
6. Check `DoD-S` item by item. A value without a source is an `[ASSUMPTION]` in the decision log; on a `Block` topic stop and ask instead. A frozen test that seems wrong is an approval decision.
7. Update `docs/routes.md`: every page and endpoint this session added or changed, status `built`, with its roles. Then start or refresh the development stack (`node out/scripts/dev.mjs up`) and leave it running, so the developer can try the story; put its URLs and the development accounts to use in the report's "Pages and endpoints".
8. Hand over: the story `done`; next is `/sdd-implement <next story or group>`, or `/sdd-review <INC>` after the increment's last story.
