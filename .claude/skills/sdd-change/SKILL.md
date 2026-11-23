---
name: sdd-change
description: Change session CR. Add a feature to, or change the behaviour of, a product this template is building or has released - interview the developer about the change, write the new and changed stories and inputs, list everything it affects, get one approval, and reopen the phases so the normal sessions build, test and release it as a new increment.
argument-hint: "[resume | answers, e.g. 'D-40: 1']"
disable-model-invocation: true
---

> For: agent · Owner: Product owner · Agent: read-only

## Session

- Id: CR, between increments or after `/sdd-release`. Frame: ask, then generate the inputs of the change.
- Reads: `docs/00_progress.md`; `project/01_requirements/intent.md`, `business-rules.md`, `e2e-scenarios.md`, the stories the change touches; `project/02_design/` files the change touches; `project/00_setup/tech-stack.md`; `docs/routes.md`; `docs/wireframes/README.md`; `docs/03_acceptance-manifest.sha256` (test file names only); `docs/release-notes.md` ("Known limitations"); `general/requirements-standard.md`; `general/rules/decisions.md`; `skills/sdd-grill-me/SKILL.md` ("Procedure" steps 3 to 6) and `skills/sdd-to-spec/SKILL.md` ("Rules").
- Writes: `project/00_intake/changes/CR-nn.md`; the `project/` files the change needs; `docs/changes/CR-nn.md`; `docs/decisions-log.md`; `docs/00_progress.md`; `.gitignore` (step 7 only).
- Must not: change code or tests; delete an input entry (a removed rule is marked `Removed by CR-nn` and keeps its id); start while another session's work is in progress.
- Exit: the change is approved, its inputs are committed, and the phases are reopened; or the developer drops it.

*Why:* inputs are frozen from `/sdd-preflight` on, so a run that changes them must do it in one approved, recorded step and then pass every gate again.

## Procedure

1. **When.** Continue only if `docs/00_progress.md` shows the run complete, or "Waiting for: nothing" with Next the first story of an increment, `/sdd-design` or `/sdd-quality-gate`. Otherwise tell the developer to finish the current session first, and stop. Give the change the next free id `CR-nn`.
2. **Interview** about the change only, with the procedure of `skills/sdd-grill-me` (one question at a time, a recommendation each time, concrete values, the texts the user sees in every UI language, limits, empty and error states, roles, personal data, a validation round at the end). Record it in `project/00_intake/changes/CR-nn.md` in the interview's notes format.
3. **Impact** in `docs/changes/CR-nn.md`: what the change is and why; new stories; changed stories with the rules, AC and EX that change; business rules, e2e scenarios, design and security entries that change; screens to add or redraw (`WF-nn`); contracts and endpoints (`docs/routes.md`); frozen acceptance and e2e test files that will no longer hold (from the manifest, by file name); new technologies or versions.
4. **Inputs** with the rules of `skills/sdd-to-spec`: new stories from the next free `US-nnn`; a changed story is edited in place and gets a "Changes" section naming `CR-nn` and what changed; rules, criteria and terms keep their ids; new versions marked `[ASSUMPTION] verify in preflight`. Ask the developer every `[ASSUMPTION]` and `OPEN` item of the change, five at a time with proposed answers, and write the answers in.
5. Check the new and changed criteria against all business rules and the unchanged stories, as `skills/sdd-to-spec` step 8 does. A conflict is a question for the developer now.
6. **Approval.** Record an approval decision "Approve change CR-nn" that lists the changed input files, the frozen tests to be replaced, the screens to redraw and the new increment; options: 1 approve (default), 2 change (what), 3 drop the change. Ask it and wait in this session.
7. **On approval:**
   - if `02_output/hidden-tests/` is tracked by git (after a release), untrack it (`git rm -r --cached 02_output/hidden-tests`) and add it back to `.gitignore`, so the runner can hold the suite out again;
   - copy the rows of "Gates" into `docs/changes/CR-nn.md` ("Gates before the change") and leave the table empty; add the new stories to "Stories" as `ready` in a new increment `INC-n`, and set changed stories back to `in progress`;
   - set Next to `/sdd-preflight` (it checks new versions and writes the new input manifest) and "Waiting for: nothing";
   - commit the inputs (`docs: change request CR-nn inputs`) and the change record (`docs: change request CR-nn`).
8. Report. Tell the developer the run continues with `/sdd-run resume` (or the manual steps), and that every later session works only on what `docs/changes/CR-nn.md` names, then checks the whole product.
