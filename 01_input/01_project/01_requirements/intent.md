# Intent

> For: agent (reads), human (reviews) · Owner: Product owner · Agent: read-only

Why the product exists and what "done" means for the customer. Sources: interview `Q` ids.

## Problem statement

Customers keep contacts of a project or company in Excel sheets and e-mail lists, or in a competing CRM that is too complex or too expensive for them. The pains named are lost time, duplicate mailings and contacting people tagged `neObvescaj`. The cost is not known and there is no baseline to measure it against (Q-01).

## Product statement

A multi-tenant web system in which users create groups of contacts, invite colleagues with a role, keep contacts with predefined fields, free tags and free key-value attributes, search and filter them, and export selected contacts to Excel. Every change is recorded and versioned, duplicates can be found and merged, and the data of one group is invisible to everybody outside it. It is reachable through a secured REST API (Q-02, Q-07).

## Users

| Role | Primary or secondary | Needs | Must not be able to |
|---|---|---|---|
| Owner | Primary | Create a group; invite and manage members; do everything with contacts; "Delete forever"; delete and restore the group | See or manage a group they are not a member of |
| Editor | Primary | Create, edit, tag, delete (to Trash), restore and merge contacts; export; see History and versions | Manage members, "Delete forever", delete the group |
| Viewer | Secondary | View, search and filter contacts | Export, see History or versions, change anything |
| Other tools (for example a mailing tool) | Secondary, later | Use the REST API directly (not decided; Q-02a) | Be given API access in the MVP |

## Core outcome

Ease of use compared with competing CRM systems (Q-03). Measured by the success criteria of the project (see DoD-P02 to DoD-P05): a new user signs in, creates a group and adds a first contact in under 2 minutes without help; search and filter answer in under 1 second for groups of up to 10,000 contacts; selected contacts export to an Excel file that opens with all columns filled; a user never sees another group's contacts.

## MVP

The smallest useful set, in priority order (Q-07a; the stories with `mvp: true`):

- Sign-in and groups: US-001, US-002, US-003, US-004 (sign-in, create group, invite, roles).
- Contacts: US-005, US-006, US-007, US-008 (contact, tags, tag management, attributes).
- Finding and export: US-009, US-010, US-011 (list and search, filters, export).
- Safety: US-012 (Trash), US-017 (secured API and OpenAPI), US-013, US-014 (History), US-015 (delete and restore a group), US-016 (delete own account), US-019 (record versions; placed in the MVP so versions exist from the first save, [ASSUMPTION]), US-018 (language).

Second increment (the customer accepts the product only when it works too, Q-19): US-020 (view, compare, restore versions), US-021 (find duplicates), US-022 (merge).

## Scope

In scope (story ids):

- Sign-in with Firebase (e-mail/password, Google), verified e-mail — US-001
- Groups, members, roles, invitations — US-002, US-003, US-004, US-015, US-016
- Contacts with predefined fields, tags, attributes — US-005, US-006, US-007, US-008
- List, search, filter — US-009, US-010
- Export to .xlsx — US-011
- Trash, restore, "Delete forever" — US-012
- History (audit trail) — US-013, US-014
- Versions — US-019, US-020
- Find and merge duplicates — US-021, US-022
- Secured REST API with OpenAPI — US-017
- Slovenian and English UI — US-018
- Delivery as container images with Docker Compose and an install and backup guide (Q-17a)

## Non-goals (out of scope)

From Q-32 (the customer must confirm): (1) sending e-mails or notifications to contacts or users, apart from Firebase's own; (2) direct use of the REST API by other tools and API keys for them; (3) import from Excel or any other file; (4) saved filters; (5) multi-factor authentication and sign-in providers other than e-mail/password and Google; (6) fuzzy duplicate matching and undoing a "not a duplicate" mark; (7) History export; (8) an in-app system administrator, billing and plans; (9) typed attribute values and several e-mails or phones per contact; (10) a mobile app; (11) export to CSV; (12) usual CRM features: tasks, deals, a notes timeline, calendar, pipelines; (13) real-time collaboration and offline use; (14) contact data from external sources. Also: bulk actions on several contacts besides export (Q-22b); deployment to the customer's production (Q-17a); reuse of any code from the SCM example (Q-04, Q-16d).

## Product principles

| ID | Principle | How it is checked |
|---|---|---|
| PP-01 | A user never sees data of a group they are not a member of (zero tolerance). | One test per API operation shows 404 for a non-member (AC-017-07); E2E-04. |
| PP-02 | Nothing is silently overwritten or lost: a conflicting save is refused, and deletes go to Trash first (except an Owner's confirmed "Delete forever"). | AC-005-07, AC-012-01, AC-012-03; E2E-05. |
| PP-03 | The History is never changed after it is written, except the anonymisation after an erasure. | AC-013-09, AC-013-10, SR-14. |
| PP-04 | Personal data leaves the system only through the export by an Owner or Editor, and every export is logged. | AC-011-10, AC-013-05. |
| PP-05 | The product never contacts a person on its own. | AC-006-08; no e-mail-sending code (SR-17). |
| PP-06 | Simple wins: no feature adds a step to the common flows (create a contact, search, export) if fewer steps give the same result. | Step count in E2E-01 and E2E-03 reviewed in the design review; DoD-P02. |

## Open questions

Product-level questions; story-level questions are in the story. Every answer marked `open — needs customer` in the interview is a developer proposal; the ones below need the customer's decision.

| ID | Question | Owner | Due | Answer |
|---|---|---|---|---|
| OQ-P-01 | Success criteria and acceptance list (Q-03, Q-36) are developer proposals. Does the customer accept them? | Customer | before the run | OPEN — needs customer |
| OQ-P-02 | Does the customer confirm that SCM is inspiration only and not an acceptance check (Q-04)? | Customer | before the run | OPEN — needs customer |
| OQ-P-03 | Block (rights, personal data, deletion, destructive, legal): the roles, invitations, 30-day Trash, anonymised History, account deletion and merge are developer proposals. | Customer | before the stories are built | OPEN — needs customer (see the stories' open questions) |
| OQ-P-04 | Hosting region (EU?), countries of the contacts, special categories of data; Firebase Authentication processes e-mails at Google (Q-33). | Customer | before delivery | OPEN — needs customer |
| OQ-P-05 | Production Firebase project: exists, owner, Google sign-in set-up (Q-14b). | Customer | before delivery | OPEN — needs customer |
| OQ-P-06 | Production host, domain, budget, whether their infrastructure runs containers (Q-17). | Customer | before delivery | OPEN — needs customer |
| OQ-P-07 | Customers come from Excel sheets and e-mail lists, yet there is no import and existing contacts are entered by hand (Q-22a). Accept this risk? | Customer | before the run | OPEN — needs customer |
| OQ-P-08 | Product name, brand, colours, logo; which SCM screens the customer likes and what they dislike in competing CRMs (Q-34, Q-34a, Q-34b). | Customer | before the design phase | OPEN — needs customer |
| OQ-P-09 | Licence rules with listed exceptions and "no Google service other than Firebase Authentication" (Q-16c, Q-16d). | Customer | before preflight | OPEN — needs customer |
| OQ-P-10 | Cost of the current way of working is unknown (Q-01); no baseline for "ease of use". | Customer | later | OPEN — deferred, no baseline |
| OQ-P-11 | Backups kept 14 days still hold contacts erased after 30 days; who owns the 99 % availability when the customer runs production (Q-15b). | Customer | before delivery | OPEN — needs customer |

## Project Definition of Done

Project additions to the release Definition of Done (`general/quality/done-release.md`), checked in phase 6.

| ID | Criterion | How it is checked |
|---|---|---|
| DoD-P01 | Journeys E2E-01 to E2E-08 pass (E2E-06 with the second increment). | One end-to-end test per scenario in `e2e-scenarios.md`, run against the local stack through the UI; result in the verification report. |
| DoD-P02 | A new user signs in, creates a group and adds a first contact in under 2 minutes without help. | Timed run of the first steps of E2E-01 (sign-in to saved first contact, at most 120 seconds of scripted action time), plus a walkthrough of the demo by a person who has not seen the product. |
| DoD-P03 | Search and filter answer in under 1 second for groups of up to 10,000 contacts. | NFR-01 measured on the deployed system with seeded data. |
| DoD-P04 | An exported file opens in Excel with all columns filled. | An automated test reads the file back and compares every cell with the source contacts (AC-011-02, AC-011-11); one manual opening in Microsoft Excel at the demo. |
| DoD-P05 | Zero tolerance on isolation: for every API operation with a group in its path, a non-member gets 404. | Test matrix generated from the OpenAPI description (AC-017-07); E2E-04. |
| DoD-P06 | The numbers of NFR-01 to NFR-09 are met on the deployed system. | Measurement per NFR in `quality-requirements.md`; recorded in the verification report. |
| DoD-P07 | The security check is against OWASP ASVS 5.0 Level 2 and no Critical or High finding is open. | Checklist per `security-requirements.md`, dependency, secret and static scans; findings in the verification report. |
| DoD-P08 | Delivery exists: container images, Docker Compose set-up, install and backup guide (start, update, backup, restore, health check), a restore tested once, the OpenAPI description, and a short user guide in Slovenian. | File check in the release phase; the restore test is run and logged. |
| DoD-P09 | Demo: J1 to J5 run on a system started from the delivered files. | E2E-01 to E2E-05 run against the delivered Compose setup; written in the verification report. |
| DoD-P10 | The second increment (versions, duplicates, merge) is finished and J6 passes. | US-020, US-021, US-022 done; E2E-06 passes. |
| DoD-P11 | The written verification report of the workflow exists; this is the acceptance document (no other). | `docs/06_verification-report.md` exists and covers every DoD-P item. |
