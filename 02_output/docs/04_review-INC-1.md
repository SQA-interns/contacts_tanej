# Review of INC-1

> For: agent (writes), human (reads) · Written in: phase 4 (`/review`) · Frame: criticise, no fixes

Range: `80361bc` (freeze commit, before the first story) to `d6f1250` (HEAD): 241 files, about 18,600 lines outside `out/logs/`. Run: Compose stack (backend, db, emulator, frontend) up and healthy; the frozen E2E-01 to E2E-08 and `responsive.spec.ts` run in Chromium on 2026-11-10 (8 passed, 6 failed; the log is the Playwright line output of this session). Screenshots compared: the existing set in `out/logs/screenshots/responsive/` (320, 768, 1280 px) against the WF-05 and WF-08 wireframes; I did not re-read every screen at every width (see "Limits").

## Result

DoD-I-01 not met (stories not at DoD-S-01, see `00_progress.md`). DoD-I-02 not met (2 High open). DoD-I-03 partly met (stack starts; 5 of 8 main flows pass, the five E2E failures below). DoD-I-04 not met (320 px layouts differ from the wireframes). Next: a fix session (D-48 answered: `/fix` after this review).

## Findings

| F | Severity | Story | Evidence | What is wrong |
|---|---|---|---|---|
| F-01 | High | US-005 | `frontend/src/features/contacts/ContactPage.tsx:226-227` (create navigates to the new page with `replace`; `saved` is only set in the edit branch, line 224); E2E-01 and E2E-03 fail at `getByText('The contact was saved.')`; `en.json:171` | After creating a contact nothing says it was saved; the confirmation exists only for an edit. A first-time user gets no feedback (DoD-P01, DoD-P02 path). The specification (AC-005-01) does not name the text, so the fix session first decides whether the frozen test or the app is wrong (D-48). |
| F-02 | High | US-002, US-004, US-009, US-010, US-012, US-013 | `e2e/responsive.spec.ts` at 320 px: groups, contacts, filter, members, history, Trash overflow (screenshot `logs/screenshots/responsive/groups-chromium-320.png`: the Actions column of the table is clipped; WF-05 mobile shows cards with Name, Role, Actions rows) | NFR-08 not met: horizontal scroll at 320 px on six screens. The mobile layout is a table, the approved wireframes show cards (UI-22). The tab row of the group is wider than 320 px (D-27 knew of it). |
| F-03 | High | US-004 | E2E-02: `getByLabel('Role')` matches the role select of the members table because of `aria-label` "Role of {name}"; the same substring rule is in `00_progress.md` | A frozen end-to-end test of the invite and role flow fails in all engines (NFR-07). It may be a test defect (substring match) or an accessible-name defect; the fix session decides and takes any test change to the developer as a decision (D-48). |
| F-04 | High | US-002, US-015 | E2E-08 fails: heading "Group not found" is not visible (WF-06); E2E-03 and E2E-06 time out finding the "Group" navigation | Flows of the frozen tests cannot be completed in the browser: the not-found screen of a missing or deleted group and the group navigation do not match what the tests (and WF-06) expect. Root cause not yet analysed. |
| F-05 | Medium | US-018, all screens | NFR-09 WebKit run: Tab stop 4 has no visible focus (`logs/nfr09-webkit.log`) | Focus indicator missing on one control in WebKit (UI baseline, NFR-09). |
| F-06 | Medium | US-001 to US-016 | `git log 80361bc..HEAD`: 5a01e68 (T-27) and 6d4c79b (T-20) are `feat` commits without `Red:` lines; e708b64, e68432a, b65d774 (NFR scripts) likewise | Test-first evidence missing (review step 2, DoD-S-02). The NFR scripts have no unit test; their evidence is the log, so Low for those, Medium for the two UI tasks. |
| F-07 | Medium | all | `rules/commits.md`: 24 commits have a subject over 72 characters (for example 102b4d3, 117 characters; 5e39bdf, 104); 18 `feat` commits exceed 15 files or 400 lines (for example e662ac5: 19 files, 1,146 lines; 12c9fee: 18 files, 990 lines; 5e39bdf: 1,065 lines) | Commit rules broken; the history cannot be rewritten (no rebase), so this is a release-notes item, not a fix. |
| F-08 | Medium | US-016, US-012 | `00_progress.md`: cleanup of old `deleted_accounts` rows open (ADR-08); browser tests AC-016-02, 05, 06 not run | A personal-data retention item (SB-13) and three acceptance tests are not evidenced. |
| F-09 | Medium | tooling | `00_progress.md`: JaCoCo report fails with "malformed input" in the Docker run | Coverage cannot be measured as the gate requires in phase 5; needs a fix before the gate. |
| F-10 | Low | US-009 | `WF-08_contacts_mobile.png` against `contacts-chromium-320.png`: the wireframe puts Filter on the search row; the build stacks Filter and Add contact below the search box | Layout differs from the wireframe at 320 px; structure and wording match. |

## Checks without a finding

- Authorisation (SB-02): every group operation goes through `AccessService.requireMember`, `requireOwner` or the Owner/writer checks (`ContactService.requireWriter`, `ExportService` Viewer refusal); non-members get the one 404 (`AccessService.java`); only `/health` is public (`SecurityConfig.java:64`).
- Validation on the trusted side (SB-01): `ContactRules`, `TagNames`, `Attributes` run in `ContactService.create` and `save`; the revision check runs under the row lock (`ContactService.java:121-131`).
- Injection and output (SB-05, SR-07, SR-08): the search and filter SQL text is fixed and every value is a bind parameter (`Where.java`); the export marks cells that start with `= + - @` as quoted text (`ExportWorkbook.java`); no `dangerouslySetInnerHTML` or `innerHTML` in `frontend/src`.
- Logs (SB-07): `RequestLogFilter` logs method, path template, status and user id only; no `console.*` in non-test front-end sources.
- Security headers, CORS for one origin, stateless tokens: `SecurityConfig.java`.
- Out of scope: I found nothing in the sources of US-020 to US-022 (INC-2) in `src/main`.

## Limits

I read the security-relevant backend code (security config, access, contact save, export, search, account deletion, request log) and sampled the screens of the groups and contacts lists; I did not read every controller, repository or React component, and I did not re-take screenshots of every screen at every width. Wording and per-width layout of the remaining screens rest on the NFR run logs. Rule-by-rule test mapping (review step 2, first bullet) was not repeated for every story; the traceability is in `docs/06_verification-report.md` for `/verify`.
