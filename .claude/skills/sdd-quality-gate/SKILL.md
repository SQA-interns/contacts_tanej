---
name: sdd-quality-gate
description: Session S6, phase 5. Measure line coverage and mutation score per component over the defined scope, run at most three targeted survivor rounds, and record the gate result or the shortfall.
disable-model-invocation: true
---

> For: agent · Owner: QA lead · Agent: read-only

## Session

- Id: S6, phase 5. Frame: measure.
- Reads: `general/testing/measurement.md`; `project/02_design/quality-requirements.md` ("Thresholds", "Mutation scope"); `docs/03_test-strategy.md` ("Measurement exclusions"); the production code and tests of the area being measured.
- Writes: unit and integration tests, tool configuration, `docs/03_test-strategy.md`, `docs/00_progress.md`, `out/logs/` (reports, `jobs/request.json`).
- Must not: change production code, except to fix a defect a new test proves (a finding with its own `fix:` commit).
- Exit (phase 5 gate): the quality gate is met per component, or the shortfall after the third survivor round is recorded with a decision.

## Procedure

1. Configure coverage and mutation tools exactly to the scope and exclusions, with settings that fit a slow suite: a time limit for the tool's first test run long enough for the whole suite; a fixed number of parallel test processes the host can carry (`docs/00_preflight-report.md`); temporary folders removed after every run; history or incremental mode, so a stopped run continues instead of starting over; the adapter the tool needs for the test framework (for example PIT needs its JUnit 5 plugin); the test profile or settings the normal test run uses. Commit the configuration alone (`build:`).
2. Measure coverage per component (`unit`, `nonUnit`, `all`), after deleting coverage data left by earlier or stopped runs. Below the line threshold, add tests area by area (validation, security, persistence, business rules first), measuring after each area.
3. Check once, on one small module, that the mutation tool works: it runs tests, and a mutant in plainly covered code is killed. A score far below line coverage means mutants are not activated: stop and raise a decision; record nothing.
4. Full mutation runs are **runner jobs** (`AGENTS.md`, "Long jobs"): one job per component, full scope, full suite, each writing its report into `out/logs/`. End the session; when the runner starts you again, read only the reports' summary.
5. Below the mutation threshold: up to **three** survivor rounds on the code that matters most, re-running only the mutated files or classes (a round that may take longer than your tool allows is a runner job too), then one confirming full run as a runner job. Justify every equivalent mutant. After writing tests in a round, run the type check, lint and format of the component on them before you commit. *Why:* v001's new tests broke the build.
6. Stop each component at the gate; record a shortfall after the third round as a non-blocking decision and a finding.
7. Fill the phase 5 sections of `docs/03_test-strategy.md`; record each mutation score also with timed-out mutants counted as survived, next to the raw score. Commit; add the phase 5 row to "Gates" (also with a recorded shortfall); hand over (next: `/sdd-verify`).
