# Requirements check

> For: agent (writes), human (reads) · Written in: phase 1 (`/clarify`) · Source: `project/01_requirements/*`, `general/requirements-standard.md`, `general/quality/definition-of-ready.md` · Agent: writes (results and gaps only; do not copy the requirements)

Session S2 clarify (`D-15: 1, D-16: 2, D-17: 1, D-18: 1`, 2026-11-09). Phase 1 gate: **passed**. No blocking decision is open (D-15 to D-18 answered). The session read only the files on its Reads list, so cited `Q-nn` ids (interview) and the screens of `ui-design.md` were not checked here.

## Run readiness (DoR-R)

| Item | Result | Note |
|---|---|---|
| DoR-R-01 | pass | `intent.md` has the problem, users, core outcome, MVP, non-goals, principles (PP-01 to PP-06) and DoD-P01 to DoD-P11. |
| DoR-R-02 | pass | "Project-wide facts" give time zone, currency, tax, formats, languages, encoding, sorting, page size, retention; no story repeats or overrides one. |
| DoR-R-03 | pass | All stories ready (D-15 to D-17 answered). |
| DoR-R-04 | pass (approved exception) | US-007 is in no scenario and US-018 is left out by the file's note; both are approved as verified by their acceptance tests (D-18). All cited AC ids in E2E-01 to E2E-08 exist. |
| DoR-R-05 | pass | Every NFR-01 to NFR-15, SR-01 to SR-20 and DoD-P01 to DoD-P11 names how it is checked. |
| DoR-R-06 | pass | Phase 0 gate passed (`docs/00_progress.md`, S1 preflight). |

## Stories

Every story has the structure of the standard, a Purpose, rules in EARS, Given/When/Then criteria citing rules, an edge-case table and "Out of scope". Cited story, rule, AC, EX, BR, NFR, SR, DoD ids that were checked exist; every `depends_on` is an existing story. No `[ASSUMPTION]` marker exists in the files read. "Ready" means DoR-S met, with Allow and non-blocking Open findings carried to `/plan`.

| Story | Ready | Failing DoR-S items | Rules → criteria | Findings (decision ids) |
|---|---|---|---|---|
| US-001 | yes | none | R-001-01..09 → AC-001-01..08, EX-001-01..07 | F-07 (D-20), F-16, F-18 (D-14) |
| US-002 | yes | none | R-002-01..08 → AC-002-01..10, EX-002-01..05 | F-08 (D-20), F-13 (D-22), F-16, F-18 (D-14), F-19 |
| US-003 | yes | none (was DoR-S-03; settled by D-16) | R-003-01..11 → AC-003-01..10, EX-003-01..08 | F-02 (D-16), F-06, F-09 (D-20), F-18 |
| US-004 | yes | none (was DoR-S-07; settled by D-15) | R-004-01..09 → AC-004-01..08, EX-004-01..05 | F-01 (D-15), F-02 (D-16) |
| US-005 | yes | none (DoR-S-05 row for phone 30 is Allow, F-17) | R-005-01..10 → AC-005-01..10, EX-005-01..12 | F-05 (D-19), F-06 (D-20), F-17, F-23 (D-23) |
| US-006 | yes | none | R-006-01..10 → AC-006-01..08, EX-006-01..08 | F-05 (D-19), F-18 |
| US-007 | yes | none (run-level DoR-R-04, D-18) | R-007-01..08 → AC-007-01..08, EX-007-01..05 | F-04 (D-18), F-05 (D-19), F-13 (D-22) |
| US-008 | yes | none | R-008-01..08 → AC-008-01..07, EX-008-01..06 | none |
| US-009 | yes | none | R-009-01..10 → AC-009-01..11, EX-009-01..09 | F-17 |
| US-010 | yes | none | R-010-01..11 → AC-010-01..10, EX-010-01..08 | F-18 |
| US-011 | yes | none | R-011-01..12 → AC-011-01..13, EX-011-01..08 | F-10 (D-21) |
| US-012 | yes | none | R-012-01..09 → AC-012-01..09, EX-012-01..08 | F-13 (D-22), F-21 |
| US-013 | yes | none (was DoR-S-03; settled by D-16) | R-013-01..13 → AC-013-01..12, EX-013-01..08 | F-02 (D-16), F-12, F-13 (D-22), F-23 (D-23) |
| US-014 | yes | none (was DoR-S-03; settled by D-16) | R-014-01..08 → AC-014-01..08, EX-014-01..05 | F-02 (D-16) |
| US-015 | yes | none (was DoR-S-03; settled by D-17) | R-015-01..10 → AC-015-01..08, EX-015-01..06 | F-03 (D-17), F-13 (D-22) |
| US-016 | yes | none (was DoR-S-07; settled by D-15) | R-016-01..07 → AC-016-01..06, EX-016-01..05 | F-01 (D-15), F-02 (D-16) |
| US-017 | yes | none (was DoR-S-03; settled by D-17) | R-017-01..15 → AC-017-01..14, EX-017-01..09 | F-03 (D-17) |
| US-018 | yes | none | R-018-01..08 → AC-018-01..07, EX-018-01..04 | F-04, F-16 |
| US-019 | yes | none | R-019-01..08 → AC-019-01..08, EX-019-01..04 | F-05 (D-19), F-11 (D-22) |
| US-020 | yes | none | R-020-01..09 → AC-020-01..09, EX-020-01..06 | F-21 |
| US-021 | yes | none | R-021-01..12 → AC-021-01..13, EX-021-01..08 | none |
| US-022 | yes | none (DoR-S-02 Allow, F-15) | R-022-01..14 → AC-022-01..14, EX-022-01..08 | F-11 (D-22), F-15 |

## Findings

| ID | Story or file | Property or DoR item | What is missing | Consequence of a wrong reading | Class | Decision |
|---|---|---|---|---|---|---|
| F-01 | US-004 OQ-004-01, US-016 OQ-016-01 | DoR-S-07 | Two Block topics (rights; keeping the name in the History) are marked "needs customer"; every other Block topic is confirmed by the developer. | The build rests on an unconfirmed rights model or legal basis. | Block | D-15 |
| F-02 | US-013 R-013-05/-07, US-003 R-003-08, US-016 R-016-03, `security-requirements.md` | Complete, consistent | What a group-level entry holds about the other person (invited e-mail, member name and e-mail) is not defined; the retention table (invitation e-mail removed after accept, cancel, expiry; Owners only) contradicts a History seen by Editors for the life of the group; R-016-03 anonymises only entries the deleted user caused. | A deleted person's e-mail stays in the History; Editors see invitation e-mails. | Block | D-16 |
| F-03 | US-015 R-015-03/-04, BR-01, US-017 R-017-05, SR-03 | Unambiguous | What an Owner's request other than "Deleted groups" and restore gets for a group in Trash. | Personal data of a deleted group readable, or a broken restore screen. | Block | D-17 |
| F-04 | `e2e-scenarios.md` | DoR-R-04 | US-007 is in no scenario. US-018 is left out by the file's own note (Q-35h): Allow, the inputs settle it. | The run cannot meet DoR-R. | Block, answered (approved exception, US-007); Allow (US-018) | D-18 |
| F-05 | US-005 R-005-08, US-007 R-007-06, US-019 R-019-03 | Complete | A group-wide tag rename or delete changes the contact but records no version; the conflict check uses the version number. | A stale save brings the old tag name back silently (PP-02). | Open | D-19 |
| F-06 | US-003 R-003-02, US-005 R-005-03, data model | Unambiguous, complete | "Valid e-mail format" is not defined; an invitation e-mail has no length limit. | Different readings accept or refuse the same address. | Open | D-20 |
| F-07 | US-001 R-001-02 | Complete | A Google account without a name or with one over 100 characters. | Sign-in fails or the History shows an empty name. | Open | D-20 |
| F-08 | US-002 R-002-08, US-015 R-015-08 | Complete | Rename of a group in Trash. | A trashed group changes under its restore check. | Open | D-20 |
| F-09 | US-003 R-003-05 | Unambiguous | The event "signs in" is not visible to the backend; a user already signed in when invited. | The group appears at an unclear moment. | Open | D-20 |
| F-10 | US-011 R-011-05, R-011-06 | Unambiguous | Forbidden file-name characters, header spelling of a merged key, tag order in the cell, key equal to a predefined header. (The separator `, ` is settled by AC-011-03: Allow.) | Test and file differ per reading. | Open | D-21 |
| F-11 | US-022 R-022-05, US-019 R-019-04 vs EX-022-08, R-019-02 | Consistent | One version "always" versus "none when nothing changed". | Version numbers differ by reading. | Open | D-22 |
| F-12 | US-013 R-013-05 | Complete | No entry for "member joined" (R-003-05) and "group created". | Gap in the audit of rights. | Open | D-22 |
| F-13 | US-002, US-007, US-012, US-015 vs US-013 | Independent (INVEST) | These stories require History entries, while US-013 depends on them. | Their History criteria cannot pass in their own session. | Open | D-22 |
| F-14 | Prose of `intent.md`, US-013, US-019, glossary, `security-requirements.md` | DoR-S-11 | Synonyms of the glossary: "audit trail", "snapshot" (in the glossary's own Version row), "multi-tenant", "project" for group (Purpose texts). | None: code, API, tests and UI use the glossary terms (glossary and `AGENTS.md` precedence). | Allow | — |
| F-15 | US-022 | DoR-S-02 | Estimate 8 is more than a third of increment 2 (18 points; a third is 6). OQ-022-03: the developer leaves the split to planning. | An oversized implement session. | Allow (OQ-022-03) | — |
| F-16 | R-001-08, R-002-03, R-002-08, R-004-02, R-013-05, R-018-04 | Singular | Rules that join several statements. | A test covers only part of a rule. | Allow (the specification splits each clause; one test per clause) | — |
| F-17 | US-005, US-009 | DoR-S-05 | No row for phone 30/31 characters and e-mail 255 (data model Contact settles: 30 saved, 31 refused; 255 saved, 256 refused); descending sort of an empty last name (reverse of EX-009-07). | None: the data model gives the values. | Allow (`business-rules.md` data model, EX-009-07) | — |
| F-18 | OQ-P-03, OQ-P-09 and the story OQs marked "confirmed by the developer; the customer still has to confirm"; delivery-time customer OQs | DoR-S-07, DoR-S-09 | Customer confirmation outstanding. | Rework if the customer disagrees. | Open (build on the developer's answer; `security-requirements.md` leaves legal matters to the customer) | D-14 |
| F-19 | BR-01, BR-02, BR-06 story lists; US-003 R-003-02 | Consistent | BR-02 omits US-002, BR-06 omits US-016, BR-01 omits US-013/US-014; R-003-02 cites BR-05, a contact rule, for the invitation e-mail. | None: the stories' own rules govern. | Allow | — |
| F-20 | US with UI | DoR-S-10 | Screens are named in prose (groups list, contact screen, Manage tags, Trash view, Deleted groups, Versions tab, merge screen), not in one list; `ui-design.md` is not on this skill's Reads list. | Screens missed. | Allow (`/wireframes` lists every screen per story) | — |
| F-21 | `business-rules.md` BR-04, US-012 | Unambiguous | Restore between the 30th day and the next cleanup: BR-04 says restore "until then" (30 days), EX-012-02 agrees. Version restore recreates a deleted tag by R-006-01/-02. | None. | Allow (BR-04, R-006-01) | — |
| F-22 | NFR-01..15, SR-01..20 | DoR-R-05 | Every item has a check. | None. | Allow | — |
| F-23 | US-013 "Save group", US-005 "Contact number" | DoR-S-11 | Homonym and a confusing term. | Wrong names in code and UI. | Open | D-23 |

Decisions answered in this session: D-15 (1), D-16 (2: invitation entries hold the event and the role only; members by name; e-mail removed from every entry where the deleted account is actor or subject), D-17 (1), D-18 (1: approved exception for US-007 and US-018). Findings F-01 to F-04 are resolved by these answers. Open non-blocking, pending review: D-14 (now also lists OQ-004-01 and OQ-016-01), D-19 to D-23. Blocking, waiting: none.
