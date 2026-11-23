# AGENTS.md

> For: agent · Owner: Team lead · Agent: read-only

You are one session of a run. Each session is started by its skill (`/sdd-preflight`, `/sdd-implement US-001`, …), does one job, hands over through files and ends. This file holds what every session needs; everything else you read only when your skill lists it.

## Start

1. Read `02_output/docs/00_progress.md`: what the previous session left, what waits on the human.
2. Follow your skill. Its "Session" section says what you read, write, must not do, and when you are done.
3. Read a file from your "Reads" list when the step that needs it starts, never before, and nothing that is not on the list. *Why:* every file you read costs context for the rest of the session.

`README.md`, `Run_Guide.md` and `system/` are for humans and the runner, not for you; you only run `node system/run.mjs --env-check` and `--leak-check` (`general/rules/secrets.md`), and `/sdd-release` runs `--summary`.

## Paths and precedence

- Aliases: `general/` = `01_input/00_general/`, `project/` = `01_input/01_project/`, `skills/` = `.claude/skills/`, `out/` = `02_output/`, `docs/` = `02_output/docs/`.
- Precedence: this file > `general/rules/` > other `general/` files > skills > `project/` files, except for explicit overrides.
- A project file adds to the general file on the same topic and never repeats it. It overrides a general item only with a line `Overrides: <ID>` or a row of an "Overrides" table, each with a reason.
- Project-wide facts (time zone, currency, tax, formats, languages) live only in `project/01_requirements/business-rules.md` ("Project-wide facts"). Never repeat them; never assume another value.
- A human answer recorded in a decision (`Human response`) replaces the input entry it changes, for that item only. Never edit `01_input/` to apply it.
- A conflict that no rule resolves is a decision record (`general/rules/decisions.md`), never a silent choice.

## Every session

- **One frame:** ask (intake), generate (to-spec, plan, wireframes, implement, design, fix), criticise (clarify, review, verify) or measure (preflight, quality gate). A criticising session never fixes what it finds; the next session does. *Why:* a checker that also fixes starts defending its fixes.
- **Blocking decision ends the session:** record it, ask all open questions in one message, set "Waiting for" and stop. The human answers by starting the same skill again with the answer (`/sdd-clarify D-03: 2`); record it as `Human response` and continue. An argument may hold the work item and answers together (`/sdd-implement US-022 D-59: 1`): record and apply the answers first, then do the whole job the argument names.
- **Hand over only through files.** Before ending, update `docs/00_progress.md` in the format below and commit everything you changed. Never rely on an earlier session's conversation, and do not start the next session's work. *Why:* only files survive; the next job deserves a fresh context.
- **Notes from the developer:** an argument `note: …` is recorded as a non-blocking decision before anything else.
- **Resume:** if your story, task or finding already has commits (`git log --oneline --grep <id>`), continue at the first item not done instead of starting over.
- Write command output longer than 50 lines to `out/logs/`; keep only a summary and the path. Use only the severity levels of `general/quality/severity-scale.md`.
- **Commands:** one command per shell call, no `&&`, `;`, `cd`, here-documents, `$(…)` or shell variables; use `git -C <dir>` and the tool's own working directory. Create and change files only with the file tools (Write, Edit), never with `echo`, `cat >`, `sed -i` or scripts that patch files. Commit with `git commit -m "<subject>" -m "<body line>"`. Run a long command in the background and wait for its notification; never poll with `sleep`. *Why:* the session runs without a person to approve prompts, so a compound or generated command is refused, and a refused write is silently lost.
- **Check your writes:** after adding a record or section to a document, confirm it is there (`grep -c`); after a commit, confirm `git status --porcelain` is empty. A write or commit you could not make is a problem in your report, never left for the next session to find.

## Report (your last message)

End every session with this block and nothing after it; the runner passes it to the developer. Same lines in every session, "none" where empty.

```
Result: done | blocked | failed: <one line>
Changed: <number of files; up to 15 paths, then "and n more">
Pages and endpoints: <new or changed, each with URL or route and the roles that reach it (docs/routes.md)>
Problems: <what went wrong, what was not run and why, what must be fixed before the next step>
Developer, do this: <numbered manual steps, each with the exact command or file; or "nothing">
For you to review: <files to open, as paths; decisions waiting, as ids>
Next: </skill argument>
```

## Handover format (`docs/00_progress.md`)

```
## Last session
- Session: <id> <skill> <argument>
- Result: done | blocked | failed
- Gate: <phase gate> passed | not passed (<which item>)
- Notes for the next session: <at most 10 lines the next session needs and cannot find in docs/>

## Next
- Command: </skill argument>
- Waiting for: <decision ids, or "nothing">

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |

## Gates
| Phase | Passed in | Commit |
```

The runner reads `Session`, `Result`, `Command`, `Waiting for` and the "Gates" table; keep these names.

- **Gates:** when your session passes a phase gate (its skill's "Exit" names it), add one row: the phase number, your skill, the commit. Phase 2 is added by `/sdd-wireframes` (by `/sdd-plan` when there is no user interface); phase 4 by `/sdd-design`, once every story is done and no review finding Critical or High is open (with no user interface there is no `/sdd-design`: the clean `/sdd-review` or the `/sdd-fix INC-n` of the last increment adds it). Never add a row for a gate that did not pass. *Why:* the runner refuses every session whose earlier gates are missing, so no phase is skipped.
- **Long jobs:** a command that may run longer than your tool allows (a full mutation run, a long scan) is not yours to run. Write `out/logs/jobs/request.json` (`[{ "name", "cwd", "command" }]`, `name` in a-z, 0-9, -), set "Waiting for: runner jobs" and Command to your own skill, and end the session. Each command must be one plain command your session may run (no shell operators); the runner runs them one at a time without a time limit and starts your skill again; read `out/logs/jobs/<name>.result.json` and `<name>.log`. If it refused the request, `out/logs/jobs/rejected.txt` says why: fix the request or do the work another way.
- **Changes:** while `docs/changes/` holds a change request `CR-nn` whose stories are not all done, work only on what its "Impact" names (its stories, screens, contracts, the frozen tests it replaces), and run every check, review and verification over the whole product. `/sdd-acceptance-tests` replaces exactly the frozen tests the approved change lists and freezes again; `/sdd-release` adds a section for the change to the release notes and the retrospective instead of rewriting them.

## Never

- From `/sdd-preflight` on: do not change anything under `01_input/` or `.claude/skills/`, or `AGENTS.md`, `CLAUDE.md`, `README.md`, `Run_Guide.md`, `system/`. Only `/sdd-grill-me`, `/sdd-to-spec` and `/sdd-run` (review answers) write `project/`, before the initial commit, and `/sdd-change` for an approved change request. *Why:* the inputs are the contract; a run that changes its own inputs can make anything pass.
- Write only what your skill's "Writes" lists. *Why:* writing elsewhere mixes jobs and hides changes from the session that checks them.
- Do not edit a file listed in `docs/03_acceptance-manifest.sha256`, and do not change `docs/00_input-manifest.sha256` after `/sdd-preflight`; raise a decision instead. *Why:* frozen tests are the judge, the manifest the proof that inputs did not change.
- Do not read, list, search or run `02_output/hidden-tests/` or the hidden vault, unless your skill is `/sdd-acceptance-tests` (it writes the suite and, in a change, may run it). Never take them from git history (`git show`, `git log -p`) either. The runner runs them; you see only `docs/06_hidden-evaluation.md`. *Why:* held-out tests measure generalisation only while the implementer has not seen them.
- Do not change a technology, version, service or secret of `project/00_setup/` without human approval, and do not install or upgrade software on the host. *Why:* the stack was checked; the host belongs to the human.
- Do not read `.env` with a file tool, print, copy or log its values, commit secrets, or ask the human to paste one (`general/rules/secrets.md`). *Why:* whatever you read can end up in logs, commits or the conversation.
- Do not decide a `Block` topic yourself (`general/rules/decisions.md`). *Why:* a plausible guess about money, rights or personal data is still a guess.
- Do not start a session before the gate it depends on has passed, and do not end the run while a Critical or High finding is open.
