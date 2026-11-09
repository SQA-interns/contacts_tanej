# Tasks and increments

> For: agent (writes), human (reads) · Written in: phase 2 (`/plan`) · Source: stories, `docs/02_specification.md` · Agent: writes

How to read this file. A task is one vertical slice that a test can observe. `/implement US-nnn` takes the story's tasks in order; a story with more than six tasks would be split into groups (`/implement US-003 T-13..T-14`); US-022 is split in two groups by decision (OQ-022-03, ADR-11). File prefixes: `B/` = `backend/src/main/java/app/contactgroups/`, `BT/` = `backend/src/test/java/app/contactgroups/`, `BR/` = `backend/src/main/resources/`, `F/` = `frontend/src/`, `E/` = `frontend/e2e/`. A task covers every rule and criterion it names; a rule that joins several statements has one test per clause (F-16). Front-end tasks follow the approved wireframes of `/wireframes` and the build-ui skill; every test display name carries the ids it proves.

## Increments

| Increment | Stories (in implement order) | Goal |
|---|---|---|
| INC-1 | US-001, US-017, US-018, US-002, US-003, US-004, US-005, US-006, US-008, US-019, US-007, US-009, US-010, US-012, US-011, US-015, US-013, US-014, US-016, then the NFR tasks T-N01 to T-N12 | MVP: sign in, groups and members, contacts with tags and attributes, list, search, filters, export, Trash, History, secured API, language. 19 stories, 73 points |
| INC-2 | US-020, US-021, US-022 | Versions screens, duplicates, merge (the customer accepts the product only when these work, Q-19). 3 stories, 18 points |

Order notes. US-017 comes right after US-001 because every later endpoint needs the secured API, the error format and the isolation harness. D-22 reading 3: the History append foundation (T-07, part of US-013) and the version recorder (T-08, part of US-019) are built before the first story that writes an entry or a version; the later tasks of US-013 and US-019 finish them. US-018's formatting comes before the screens. US-011 comes before the rest of US-013 because US-013 depends on it.

## Tasks

### Increment 1

| Task | Story | Covers (R, AC, EX) | Expected files | Depends on |
|---|---|---|---|---|
| T-01 | US-001 | Sign-in, registration, reset: R-001-01..03, R-001-06, R-001-07; AC-001-01, AC-001-04..07; EX-001-01..05 | `F/features/auth/{SignInPage,RegisterPage,ResetPasswordPage}.tsx`, `F/lib/auth.ts`, locales, tests | skeleton |
| T-02 | US-001 | Verify e-mail, session, sign out, route guard, empty state: R-001-04, R-001-05, R-001-08, R-001-09; AC-001-02, AC-001-03, AC-001-08; EX-001-06, EX-001-07 | `F/features/auth/VerifyEmailPage.tsx`, `F/app/{guards,routes}.tsx`, `F/features/groups/EmptyState.tsx` | T-01 |
| T-03 | US-017 | Identity: R-017-01..03, R-017-14; AC-017-01..03; EX-017-01; BR-03; Google name rule D-20; `GET /me`; deleted-account 401 (R-016-06 part); startup refusal of the emulator host outside local and test (ADR-02) | `B/config/{VerifiedEmailFilter,UserSyncFilter}.java`, `B/api/AccountController.java`, `B/service/UserService.java`, `B/repository/UserRepository.java`, tests | skeleton |
| T-04 | US-017 | Errors and logs: R-017-07, R-017-10, R-017-13; AC-017-10, AC-017-13; EX-017-07; SR-10, SR-11 (validation mapping, 405, 500 without internals, `RequestLogFilter`, log test) | `B/api/ApiExceptionHandler.java`, `B/domain/ApiException.java`, `B/config/RequestLogFilter.java`, tests | T-03 |
| T-05 | US-017 | Limits: R-017-08, R-017-09; AC-017-08, AC-017-09; EX-017-04, EX-017-05; SR-09 | `B/config/{RateLimitFilter,BodySizeLimitFilter}.java`, tests | T-03 |
| T-06 | US-017 | Health, OpenAPI, isolation harness, headers: R-017-04..06, R-017-11, R-017-12; AC-017-04, AC-017-05, AC-017-07, AC-017-11, AC-017-12; EX-017-02, EX-017-06, EX-017-08, EX-017-09; SR-03, SR-05, SR-06; ADR-09 (drift test, one 404 builder `AccessService.notFound`) | `BT/OpenApiContractIT.java`, `BT/IsolationMatrixIT.java`, `BT/SecurityHeadersIT.java`, `B/service/AccessService.java` (404 only) | T-03, T-04 |
| T-X01 | all | Glossary lint (Do-not-use words of `business-rules.md` appear nowhere in sources, ES-13); no mail dependency (SR-17); no `dangerouslySetInnerHTML` | `BT/GlossaryLintTest.java`, `F/glossary.test.ts` | T-06 |
| T-07 | US-013 | History append foundation (D-22): R-013-07, R-013-10, R-013-11; AC-013-09; EX-013-01, EX-013-08; AR-09, SR-14; `HistoryRecorder`, change sets, guard triggers proven, read model | `B/service/HistoryRecorder.java`, `B/domain/{HistoryEntry,ChangeSet,HistoryEventType}.java`, `B/repository/HistoryRepository.java`, tests | T-06 |
| T-08 | US-019 | Version recorder foundation: R-019-01, R-019-05, R-019-07, R-019-08; AC-019-07; EX-019-01; snapshot JSON, numbering under lock, `ContactVersion` | `B/service/VersionService.java` (record), `B/repository/VersionRepository.java`, `B/domain/ContactVersion.java`, tests | T-06 |
| T-09 | US-018 | Language and formats: R-018-01..06, R-018-08; AC-018-01..05, AC-018-07; EX-018-01..04 (formatDateTime in `Europe/Ljubljana`, decimals, country names, language persistence, no key shown, server error texts) | `F/lib/{format,i18n}.ts`, `F/components/layout/LanguageSwitch.tsx`, locales, tests | T-02 |
| T-10 | US-002 | Create, list, open a group; access check: R-002-01, R-002-02, R-002-04..07; AC-002-01..04; EX-002-01..04 | `B/api/GroupController.java`, `B/service/{GroupService,AccessService}.java`, `B/repository/{GroupRepository,MembershipRepository}.java`, tests | T-07 |
| T-11 | US-002 | Name rule per Owner and rename with History: R-002-03, R-002-08; AC-002-05..10; EX-002-05; D-20 reading 3 | `B/service/GroupService.java`, locks of ADR-03, tests | T-10 |
| T-12 | US-002 | Groups UI: list, empty state button, create and rename dialogs, not-found page | `F/features/groups/*`, tests | T-10, T-09 |
| T-13 | US-003 | Create, list, cancel invitations: R-003-01..04, R-003-07..09, R-003-11; AC-003-01, AC-003-03..05, AC-003-07, AC-003-09, AC-003-10; EX-003-01, EX-003-02, EX-003-06, EX-003-08 | `B/api/InvitationController.java`, `B/service/InvitationService.java`, `B/repository/InvitationRepository.java`, `B/domain/EmailAddress.java`, tests | T-11 |
| T-14 | US-003 | Acceptance on the first request and expiry: R-003-05, R-003-06, R-003-10; AC-003-02, AC-003-06, AC-003-08; EX-003-03..05, EX-003-07; D-20 reading 4 | `B/service/InvitationService.java` (`acceptFor`), tests with the mutable clock | T-13 |
| T-15 | US-003 | Invite UI: form, pending list with expiry, cancel | `F/features/members/{InviteForm,PendingInvitations}.tsx`, tests | T-13, T-12 |
| T-16 | US-004 | Member list, role change, removal, leaving, last Owner: R-004-01..09; AC-004-01..08; EX-004-01..04; EX-017-03 (a removed member's old token gets 404); parallel-request test on the row lock | `B/api/MemberController.java`, `B/service/MemberService.java`, tests | T-14 |
| T-17 | US-004 | Members tab UI with role select, remove, leave, dialogs, last-Owner message | `F/features/members/*`, tests | T-16, T-15 |
| T-18 | US-005 | Create and open a contact with validation: R-005-01..06; AC-005-01..05, AC-005-10; EX-005-01..08, EX-005-10, EX-005-11; version 1 (T-08), History `CONTACT_CREATED` (T-07) | `B/api/ContactController.java`, `B/service/ContactService.java`, `B/domain/{ContactRules,CountryCodes}.java`, `B/repository/ContactRepository.java`, tests | T-08, T-16 |
| T-19 | US-005 | Save with revision, no-op, Trash and role: R-005-07..10; AC-005-06..09; EX-005-09, EX-005-12; R-017-15, AC-017-14; D-19; History `CONTACT_EDITED` | `B/service/ContactService.java` (save), tests incl. parallel save | T-18 |
| T-20 | US-005 | Contact screen and form (11 fields, country select), conflict screen, Viewer read-only, trashed notice | `F/features/contacts/{ContactPage,ContactForm,ConflictPanel}.tsx`, tests | T-19, T-12 |
| T-21 | US-006 | Tags in the save path: R-006-01..09; AC-006-01..04, AC-006-06..08; EX-006-01..06, EX-006-08; History `TAG_CHANGED` | `B/service/TagService.java`, `B/domain/TagName.java`, `B/repository/TagRepository.java`, tests | T-19 |
| T-22 | US-006 | Suggestions and tag input: R-006-10; AC-006-05; EX-006-07 | `B/api/TagController.java` (list), `F/features/contacts/TagInput.tsx`, tests | T-21, T-20 |
| T-23 | US-008 | Attributes in the save path: R-008-01..04, R-008-06..08; AC-008-01, AC-008-02, AC-008-04..07; EX-008-01..06; History `ATTRIBUTE_CHANGED` | `B/service/ContactService.java`, `B/domain/Attribute.java`, tests | T-19 |
| T-24 | US-008 | Key suggestions and attribute rows: R-008-05; AC-008-03 | `B/api/ContactController.java` (`attribute-keys`), `F/features/contacts/AttributeRows.tsx`, tests | T-23, T-20 |
| T-25 | US-019 | Versions recorded on every kind of change: R-019-02..04, R-019-06; AC-019-01..06, AC-019-08; EX-019-02..04 (fields, tags, attributes, no-op, refused save, trash and restore, tag rename; `GET /versions`) | `B/api/VersionController.java` (list), tests | T-21, T-23 |
| T-26 | US-007 | Rename and delete a tag: R-007-01..08; AC-007-01..08; EX-007-01..05; revision bump (D-19); History per contact incl. Trash | `B/api/TagController.java`, `B/service/TagService.java`, tests | T-21, T-25 |
| T-27 | US-007 | Manage tags UI | `F/features/tags/ManageTagsPage.tsx`, tests | T-26, T-22 |
| T-28 | US-009 | List, sort, paging, Slovenian order: R-009-01, R-009-02, R-009-07, R-009-08, R-009-10, R-018-07; AC-009-01, AC-009-09..11; AC-018-06; EX-009-01, EX-009-06, EX-009-07, EX-009-09 | `B/api/SearchController.java`, `B/service/SearchService.java`, `B/repository/ContactRepository.java` (search), tests | T-23 |
| T-29 | US-009 | Search by folded words, tags and attributes: R-009-03, R-009-04; AC-009-02..05, AC-009-08; EX-009-02..05 (injection strings) | `B/service/SearchService.java`, tests with Škerlj, Čuk, Šuster | T-28 |
| T-30 | US-009 | List screen: table or cards, search box, sort, paging, empty and no-match states, open a contact: R-009-05, R-009-06, R-009-09; AC-009-06, AC-009-07, AC-009-10 | `F/features/contacts/{ContactListPage,ContactTable}.tsx`, tests | T-29, T-24 |
| T-31 | US-010 | Seven conditions with AND: R-010-01..09; AC-010-01..07, AC-010-10; EX-010-01..08 (companies endpoint) | `B/service/SearchService.java`, `B/api/ContactController.java` (`companies`), tests | T-29 |
| T-32 | US-010 | Filter dialog, chips, clear: R-010-10, R-010-11; AC-010-08, AC-010-09 | `F/features/contacts/{FilterDialog,FilterChips}.tsx`, tests | T-31, T-30 |
| T-33 | US-012 | Trash and restore a contact: R-012-01..03, R-012-07, R-012-08; AC-012-01, AC-012-02, AC-012-05, AC-012-07, AC-012-08; EX-012-01, EX-012-03..06; History | `B/api/TrashController.java`, `B/service/TrashService.java`, tests | T-30 |
| T-34 | US-012 | Delete forever, erasure and cleanup: R-012-04..06, R-012-09; AC-012-03, AC-012-04, AC-012-06, AC-012-09; EX-012-02, EX-012-07, EX-012-08; SR-15; ADR-05, ADR-08 | `B/service/{TrashService,TrashCleanupService}.java`, tests with the mutable clock | T-33 |
| T-35 | US-012 | Trash tab, delete and Delete forever dialogs | `F/features/contacts/{TrashPage,ConfirmDialog}.tsx`, tests | T-34 |
| T-36 | US-011 | Export file: R-011-04..09, R-011-11, R-011-12; AC-011-01..04, AC-011-06, AC-011-08, AC-011-09, AC-011-11..13; EX-011-01..08; SR-08; History `EXPORTED` | `B/api/ExportController.java`, `B/service/ExportService.java`, `BR/messages/export_{sl,en}.properties`, tests reading the file back | T-32 |
| T-37 | US-011 | Selection, Export button, download: R-011-01..03, R-011-10; AC-011-05, AC-011-07, AC-011-10, AC-017-06 | `F/features/export/*`, `F/features/contacts/ContactTable.tsx` (tick boxes), tests | T-36 |
| T-38 | US-015 | Delete and restore a group, D-17 rules: R-015-01..05, R-015-07..10; AC-015-01..04, AC-015-06..08; EX-015-01, EX-015-03..05; History | `B/api/GroupController.java` (trash, restore, deleted-groups), `B/service/GroupService.java`, tests | T-37 |
| T-39 | US-015 | Purge of a group in the cleanup: R-015-06; AC-015-05; EX-015-02, EX-015-06; SR-15 | `B/service/TrashCleanupService.java`, tests | T-38, T-34 |
| T-40 | US-015 | Delete group dialog with typed name; Deleted groups list | `F/features/groups/{DeleteGroupDialog,DeletedGroupsPage}.tsx`, tests | T-38, T-35 |
| T-41 | US-013 | Contact History content and tab: R-013-01, R-013-02, R-013-07, R-013-09, R-013-12, R-013-13; AC-013-01..03, AC-013-07, AC-013-08, AC-013-10, AC-013-11; EX-013-02..05, EX-013-07 (one test per event clause) | `B/api/HistoryController.java`, `B/service/HistoryService.java`, tests | T-37 |
| T-42 | US-013 | Group-level and tag and export events: R-013-03..06, R-013-08, R-013-11; AC-013-04..06, AC-013-12; EX-013-04, EX-013-06 (invite entries hold the role only, D-16) | `B/service/{GroupService,MemberService,InvitationService,TagService,ExportService}.java` (recorder calls), tests | T-41, T-38 |
| T-43 | US-013 | History tab UI with change sets and the erasure marker in the active language | `F/features/history/{HistoryTab,ChangeSetView}.tsx`, tests | T-41 |
| T-44 | US-014 | Group-wide History with filters: R-014-01..08; AC-014-01..08; EX-014-01..05 (project-time-zone day boundaries) | `B/api/HistoryController.java` (group, actors), `B/service/HistoryService.java`, tests | T-42 |
| T-45 | US-014 | Group History screen | `F/features/history/{GroupHistoryPage,HistoryFilters}.tsx`, tests | T-44, T-43 |
| T-46 | US-016 | Account deletion: R-016-01..07; AC-016-01..06; EX-016-01..05; EX-004-05 (last Owner deleting the account); D-16 anonymisation; tombstone (ADR-02) | `B/service/AccountService.java`, `B/api/AccountController.java` (delete), tests | T-44 |
| T-47 | US-016 | Account screen: confirmation, sole-Owner refusal, re-sign-in, Firebase deletion | `F/features/account/*`, tests | T-46, T-45 |

### Non-functional tasks of increment 1

| Task | Story | Covers | Expected files | Depends on |
|---|---|---|---|---|
| T-N01 | — | NFR-01, NFR-02, NFR-04, DoD-P03, EX-009-08, EX-010-07: timing and load test with 10,000 seeded contacts, 20 parallel users, 100 users | `scripts/perf/{seed-10000.sql,load-test.mjs}`, log in `out/logs/` | T-47 |
| T-N02 | — | NFR-03, NFR-15: export of 1,000 and 10,000 contacts | `BT/ExportPerformanceIT.java` or `scripts/perf/export-time.mjs` | T-36 |
| T-N03 | — | NFR-05: write, restart, read back, `/health` | `scripts/restart-test.sh` | T-47 |
| T-N04 | — | NFR-06: backup, restore into a clean stack, data comparison; daily backup and 14-day retention in the guide | `scripts/{backup,restore}.sh`, log | T-47 |
| T-N05 | — | NFR-07: main flows in Chromium, Firefox, WebKit | `E/*.spec.ts` (shared with the E2E scenarios) | T-47 |
| T-N06 | — | NFR-08: screenshots at 320, 768, 1280 px, no horizontal scroll | `E/responsive.spec.ts` | T-47 |
| T-N07 | — | NFR-09: axe per main screen in both themes and languages, keyboard-only run | `E/accessibility.spec.ts` | T-47 |
| T-N08 | — | NFR-10: language switch on each screen of the main flows | `E/language.spec.ts` | T-09, T-47 |
| T-N09 | — | NFR-11, DoD-P02: states review, first contact in under 120 s | `E/first-contact.spec.ts` | T-47 |
| T-N10 | — | NFR-12: delivered Compose set-up on a clean host with only `.env`, E2E-01 passes | `scripts/clean-host-test.sh` | T-47 |
| T-N11 | — | NFR-13: start on an empty and on the previous release's database | `BT/MigrationIT.java` | T-47 |
| T-N12 | — | NFR-14, SR-19: licence scan, forbidden imports, SCM search, dependency scans | `scripts/licence-check.mjs`, CI | T-47 |

### Increment 2

| Task | Story | Covers (R, AC, EX) | Expected files | Depends on |
|---|---|---|---|---|
| T-48 | US-020 | Versions list, version view, restore: R-020-01..09 (backend); AC-020-01, AC-020-02, AC-020-04..09; EX-020-01..06; History `VERSION_RESTORED`; D-19 conflict | `B/api/VersionController.java`, `B/service/VersionService.java`, tests | T-47 |
| T-49 | US-020 | Versions tab, compare view with highlighted differences, restore dialog: R-020-01..03, R-020-09; AC-020-03 | `F/features/versions/*`, tests | T-48 |
| T-50 | US-021 | Detection and dismissals: R-021-01..12 (backend); AC-021-01..13; EX-021-01..08; ADR-11 | `B/api/DuplicateController.java`, `B/service/DuplicateService.java`, `B/repository/DuplicateRepository.java`, tests | T-48 |
| T-51 | US-021 | Duplicates tab: find, sets with kind and reason, not a duplicate, empty message | `F/features/duplicates/*`, tests | T-50 |
| T-52 | US-022 | Group A, merge operation and screen: R-022-01..05, R-022-07..14; AC-022-01..03, AC-022-05..14; EX-022-01..08; ADR-11 (`/implement US-022 T-52`) | `B/service/MergeService.java`, `B/api/DuplicateController.java` (merge), `F/features/duplicates/MergePage.tsx`, tests | T-50, T-51, T-48 |
| T-53 | US-022 | Group B, History linking: R-022-06; AC-022-04 (`/implement US-022 T-53`) | `B/service/HistoryService.java`, `B/repository/HistoryRepository.java` (merge link), tests | T-52 |

## Checks that are not tasks

Phase 3 writes the acceptance tests (all `AC` and `EX`) and the eight end-to-end tests of `e2e-scenarios.md`; phase 4 implements the tasks above test-first; phase 5 raises coverage to 95 % and the adjusted mutation score to 90 % (`pitest-junit5-plugin` is added then, decision D-24); phase 6 verifies DoD-P01 to DoD-P11. US-007 and US-018 are verified by their acceptance tests only (D-18).
