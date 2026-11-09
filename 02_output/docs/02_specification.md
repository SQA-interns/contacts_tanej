# Specification

> For: agent (writes), human (reads) · Written in: phase 2 (`/plan`) · Source: `project/01_requirements/*`, `project/02_design/*`, `project/00_setup/tech-stack.md`, `project/00_setup/environments.md`, `general/security/*`, `general/design/ui-baseline.md`, `docs/02_adr/` · Agent: writes

Reading guide: sections are numbered 1 to 10; each story has its own subsection 4.n. Every `R`, `AC`, `EX`, `BR`, `SR`, `SB` and `NFR` of the inputs appears in section 10, which points to the section that implements or checks it (`out/logs/traceability-check.log` proves nothing is missing). Decisions D-14 to D-25 are applied as written in `docs/decisions-log.md`; where this file uses a reading they settle, it names the decision. Interfaces and their contracts are in `docs/02_contracts/`; choices and their reasons in `docs/02_adr/ADR-01` to `ADR-12`.

## 1. Components and modules

| Component | Folder | Responsibility | Build and check |
|---|---|---|---|
| backend | `02_output/backend` | REST API, rules of every story, persistence, scheduled cleanup, export. Java 21, Spring Boot 3.5.16, Spring JDBC, Flyway, PostgreSQL | `./mvnw` (see its README); package root `app.contactgroups` |
| frontend | `02_output/frontend` | Web app in Slovenian and English; Firebase Authentication for sign-in only; calls the REST API | `npm` scripts (see its README) |
| database | PostgreSQL 17.6 (container) | Data of both components' users; schema by Flyway migrations of the backend | `V1__baseline.sql` and later |
| Authentication Emulator | `02_output/docker/emulator` | Test and local sign-in; not part of the delivered stack | `docker compose` (local) |

### 1.1 Backend modules (`app.contactgroups`)

| Package | Module (class) | Responsibility | Stories |
|---|---|---|---|
| `api` | `ApiExceptionHandler`, `ErrorResponse` | One error format for every failure (ADR-06) | US-017 |
| `api` | `GroupController`, `MemberController`, `InvitationController`, `AccountController` | Endpoints of groups, members, invitations, `/me` | US-002, US-003, US-004, US-015, US-016 |
| `api` | `ContactController`, `SearchController`, `TagController`, `TrashController` | Contact CRUD, list, tags, Trash | US-005 to US-010, US-012 |
| `api` | `ExportController`, `HistoryController`, `VersionController`, `DuplicateController`, `HealthController` (actuator `/health`) | Export, History, versions, duplicates and merge, health | US-011, US-013, US-014, US-017, US-019 to US-022 |
| `service` | `AccessService` | Membership, role and Trash check of every group request; the one 404 (ADR-01) | BR-01, BR-02, BR-03 |
| `service` | `GroupService`, `MemberService`, `InvitationService`, `AccountService` | Group lifecycle and name rule, roles and the last-Owner rule, invitations, account deletion | US-002, US-003, US-004, US-015, US-016 |
| `service` | `ContactService`, `TagService`, `SearchService`, `TrashService` | Save with conflict check, tags and attributes, list and filters, Trash, erasure | US-005 to US-010, US-012 |
| `service` | `ExportService`, `HistoryService`, `HistoryRecorder`, `VersionService`, `DuplicateService`, `MergeService` | Export, History read and write, versions, duplicates, merge (ADR-05, ADR-11, ADR-12) | US-011, US-013, US-014, US-019 to US-022 |
| `service` | `TrashCleanupService` | Daily erasure of expired Trash, expired invitations, old account tombstones (ADR-08) | BR-04 |
| `repository` | `UserRepository`, `GroupRepository`, `MembershipRepository`, `InvitationRepository`, `ContactRepository`, `TagRepository`, `HistoryRepository`, `VersionRepository`, `DuplicateRepository` | Spring JDBC, parameterised SQL, every method takes the group id (AR-02, AR-03) | all |
| `domain` | `Role`, `ErrorCode`, `ContactRules`, `TagName`, `EmailAddress`, `CountryCodes`, `ProjectTime`, `ErasureMarker`, records `Contact`, `Group`, `Member`, … | Glossary types and the value rules of section 3 | all |
| `config` | `AppProperties`, `RequiredConfigurationCheck`, `ClockConfig`, `SecurityConfig`, `FirebaseJwtDecoders`, `VerifiedEmailFilter`, `UserSyncFilter`, `RateLimitFilter`, `BodySizeLimitFilter`, `RequestLogFilter` | Configuration, security chain, request limits, log rules (ADR-02, ADR-06) | US-017 |

Dependencies point downwards only: `api → service → repository → domain`; `config` may use all; tested by `ArchitectureTest` (AR-01).

### 1.2 Frontend modules (`src/`)

| Folder | Content | Stories |
|---|---|---|
| `app/` | `App.tsx` (providers), `routes.tsx` (route table), route guards | US-001, US-017 |
| `features/auth/` | Sign-in, registration (name, e-mail, password), verify-e-mail screen, password reset | US-001 |
| `features/groups/` | Groups list and empty state, create and rename dialogs, delete with typed name, Deleted groups | US-002, US-015 |
| `features/members/` | Member list, invite, cancel invitation, change role, remove, leave | US-003, US-004 |
| `features/contacts/` | List with search, filters, sort, paging, selection; contact screen and form (fields, tags, attributes); conflict screen; Trash view | US-005 to US-010, US-012 |
| `features/tags/` | Manage tags (rename, delete) | US-007 |
| `features/export/` | Selection bar, Export button and download | US-011 |
| `features/history/` | History tab, group-wide History with filters | US-013, US-014 |
| `features/versions/` | Versions tab, compare, restore | US-020 |
| `features/duplicates/` | Find duplicates, set list, "not a duplicate", merge screen | US-021, US-022 |
| `features/account/` | Delete account, language | US-016, US-018 |
| `components/ui/`, `components/layout/` | shadcn/ui components, shell, language switch | all (UI-01) |
| `lib/` | `api/` (client, generated types), `firebase.ts`, `i18n.ts`, `config.ts`, `format.ts` | all |
| `locales/` | `sl.json`, `en.json` | all (NFR-10) |
| `styles/` | `index.css`, `tokens.wireframe.css`, `tokens.final.css` | UI-03 |

## 2. Domain model (glossary → code)

| Term | Module | Type | Table | API name |
|---|---|---|---|---|
| user | `domain.User`, `UserRepository` | record `User(id, name, email)` | `users` | `User`, `/me` |
| group | `domain.Group`, `GroupService` | record; `state` enum `ACTIVE`/`TRASHED` | `groups` | `GroupSummary`, `/groups` |
| member | `domain.Member`, `MemberService` | record `Member(groupId, userId, role)` | `memberships` | `Member`, `/groups/{groupId}/members` |
| Owner, Editor, Viewer | `domain.Role` | enum `OWNER`, `EDITOR`, `VIEWER` | `memberships.role` | `Role` |
| invitation | `domain.Invitation`, `InvitationService` | record; only pending rows exist | `invitations` | `Invitation`, `/groups/{groupId}/invitations` |
| contact | `domain.Contact`, `ContactService` | record with `state` `ACTIVE`/`TRASHED` | `contacts` | `Contact`, `ContactRow`, `/groups/{groupId}/contacts` |
| predefined field | `domain.ContactField` | enum of the 11 fields | columns of `contacts` | `ContactFields` properties |
| tag | `domain.Tag`, `TagService` | record | `tags`, `contact_tags` | `Tag`, `/groups/{groupId}/tags` |
| attribute | `domain.Attribute` | record `Attribute(key, value)` | `attributes` | `Attribute` |
| History | `domain.HistoryEntry`, `HistoryRecorder` | record + enum `HistoryEventType` | `history_entries`, `history_changes` | `HistoryEntry`, `/history` |
| ChangeSet (story term "Save group", D-23) | `domain.ChangeSet` | the entries that share one id | `history_entries.change_set_id` | `changeSetId` |
| version | `domain.ContactVersion`, `VersionService` | record; snapshot JSON | `contact_versions` | `Version`, `VersionDetail`, `/versions` |
| contact version number (story term "Contact number", D-23) | `domain.Contact.versionNumber` | integer from 1 | `contacts.current_version` | `versionNumber` |
| revision (conflict counter, D-19, D-25) | `domain.Contact.revision` | long from 1 | `contacts.revision` | `revision` |
| Trash | `TrashService`, `Contact.state`, `Group.state` | state `TRASHED` + `trashedAt` | `state`, `trashed_at` | `/trash`, `/trash-contacts`, `/deleted-groups`, `daysLeft` |
| Delete forever | `TrashService.eraseContact` | operation | deletes the `contacts` row | `eraseContact` = `DELETE /contacts/{id}` |
| erasure | `HistoryRecorder.erase`, `TrashService` | operation | `history_entries.erased` | — |
| erasure marker | `domain.ErasureMarker` (`ERASED_MARKER`) | UI text per language, never stored | `erased` flag | `erased: true` |
| selection | web app `Selection` | ids kept across pages | none | `ExportRequest.contactIds` |
| export | `ExportService` | operation, `.xlsx` | none (History entry only) | `Export`, `POST /exports` |
| duplicate set | `domain.DuplicateSet`, `DuplicateService` | record with `kind` and `reasons` | none | `DuplicateSet` |
| certain / probable duplicate | `DuplicateKind` | enum `CERTAIN`, `PROBABLE` | none | `kind` |
| duplicate dismissal | `DuplicateRepository` | record | `duplicate_dismissals` | `DismissalRequest` |
| merge | `MergeService.merge` | operation | `contact_merges` | `mergeContacts` = `POST /merges` |
| survivor | `MergeService` | the oldest picked contact | `contact_merges.survivor_id` | response of `mergeContacts` |
| conflict | `ErrorCode.CONFLICT` | 409 with the current contact | — | `CONFLICT` |

Language checks: code, API, tables and tests use these words; the Do-not-use words of the glossary appear nowhere (a lint test greps the sources for them, task T-02).

## 3. Data model and validation

Storage: `db/migration/V1__baseline.sql` (ADR-03). The database repeats every length and format limit as a CHECK so that no code path can store an invalid value; the service validates first and reports every broken field at once (ADR-06).

### 3.1 Value rules (`domain`)

| Value | Rule | Field error code | Source |
|---|---|---|---|
| Text field of a contact | Trimmed; empty is stored as NULL; at most 255 characters (first name, last name, company, job title, street, postal code, city) | `TOO_LONG` | BR-05, EX-005-03, EX-005-04 |
| Notes | Trimmed; at most 5,000 characters; may hold anything | `TOO_LONG` | EX-005-05 |
| E-mail | One `@`, a non-empty local part, a domain with at least one dot and no empty label, no spaces, at most 255 characters; stored in lowercase; not unique | `INVALID_FORMAT`, `TOO_LONG` | R-005-03, R-005-04, R-005-05, D-20 |
| Phone | Only digits, spaces and `+ ( ) - . /`, at most 30 characters, stored as typed (after trimming) | `INVALID_CHARACTERS`, `TOO_LONG` | EX-005-06, EX-005-07, F-17 |
| Country | ISO 3166-1 alpha-2 from `Locale.getISOCountries()`, upper case; names are shown by the language | `UNKNOWN_COUNTRY` | AC-005-10, EX-005-08 |
| One of four | After trimming at least one of first name, last name, e-mail, company is not empty | `REQUIRED_ONE_OF` (field `firstName`, with the other three named in the message) | R-005-02, EX-005-01, EX-005-02 |
| Tag name | 1 to 50 characters; Unicode letters, digits, `-`, `_`; unique per group ignoring case; first spelling kept; at most 50 tags per contact; a repeated tag on one contact is stored once | `TOO_SHORT`, `TOO_LONG`, `INVALID_CHARACTERS`, `TOO_MANY` | R-006-03, R-006-04, EX-006-05 |
| Attribute | Key trimmed, 1 to 50 characters, any characters, unique per contact ignoring case (not accents); value at most 255 characters, may be empty, never interpreted; at most 50 attributes | `REQUIRED`, `TOO_LONG`, `DUPLICATE_KEY`, `TOO_MANY` | R-008-02..04, EX-008-05, EX-008-06 |
| Group name | Trimmed, 1 to 100 characters; unique ignoring case among the Owner's groups not in Trash (checked for every Owner, on create, rename and restore; promotion is never refused) | `REQUIRED`, `TOO_LONG`; 409 `GROUP_NAME_TAKEN` | R-002-02, R-002-03, EX-002-01, EX-002-02 |
| Invitation | Valid e-mail (above), role, at most 20 pending, not a member, not already pending (ignoring case) | `INVALID_FORMAT`; 409 `ALREADY_MEMBER`, `INVITATION_PENDING`, `INVITATION_LIMIT` | R-003-02..04 |
| User name | 1 to 100 characters; at registration required; from Google or the e-mail local part (D-20) | `REQUIRED`, `TOO_LONG` (web app) | R-001-02, EX-001-03, EX-001-04 |
| Password | At least 8 characters; checked by the web app and Firebase | web app message | R-001-03, EX-001-01, EX-001-02 |
| Date range (History) | `to` not before `from`; days in `Europe/Ljubljana` | `INVALID_DATE_RANGE` | EX-014-02, R-014-04 |

The contact rules are one class (`ContactRules.validate(ContactInput)`) used by create, save, version restore (snapshots are valid by construction), and merge (the combined result is validated, R-022-09).

### 3.2 Entities and tables

| Entity | Table | Key facts (full DDL in `V1__baseline.sql`) |
|---|---|---|
| User | `users` | `id` = Firebase uid; name 1 to 100; e-mail lowercase; upserted from the token on each request (ADR-02) |
| deleted account | `deleted_accounts` | uid and time only; makes an old token 401 (R-016-06); removed after 2 days |
| Group | `groups` | `state`, `trashed_at` (both or neither); name trimmed 1 to 100 |
| Membership | `memberships` | PK (group, user); role; removed when the user is deleted or the group is purged |
| Invitation | `invitations` | only pending rows; lowercase e-mail; unique (group, e-mail); expires 14 days after `created_at` (read-time predicate); accepted and cancelled rows are deleted (retention table) |
| Contact | `contacts` | 11 fields (NULL for empty), `state`, `trashed_at`, `created_at`, `created_by`, `current_version`, `revision`, generated `search_fold`; BR-05 CHECK |
| Tag, Contact tag | `tags`, `contact_tags` | tag unique per group ignoring case; a tag row exists only while a contact carries it |
| Attribute | `attributes` | unique (contact, lower(key)) |
| Version | `contact_versions` | PK (contact, number); JSON snapshot; author id and name; cascade with the contact |
| History entry | `history_entries`, `history_changes` | append-only by trigger; `contact_id` without foreign key; `erased`; actor e-mail nullable |
| Duplicate dismissal | `duplicate_dismissals` | unique (group, `set_key`) |
| Merge link | `contact_merges` | merged contact → survivor |

Lifecycles are as in `business-rules.md`: Group active → in Trash → active or gone; Contact active → in Trash → active or gone; Invitation pending → accepted or cancelled (row deleted) or expired; Membership created on acceptance or group creation, removed by Owner, leaving, account deletion; the last Owner cannot be removed. History entries are written once and only anonymised.

<!-- next: 4. Behaviour per story -->
