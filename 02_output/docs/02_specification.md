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

## 4. Behaviour per story

Each subsection says which module implements which rule. A rule that joins several statements is split into clauses and each clause has its own test (F-16). "History" means a call of `HistoryRecorder` in the same transaction (AR-09); "version" means `VersionService.record`. Increment 1 holds US-001 to US-019, increment 2 holds US-020 to US-022.

### 4.1 US-001 Register and sign in (frontend `features/auth`, Firebase SDK)

- R-001-01 `SignInPage` offers e-mail/password and "Sign in with Google" (`signInWithEmailAndPassword`, `signInWithPopup(GoogleAuthProvider)`); no other provider, no MFA. R-001-02 `RegisterPage` requires a name of 1 to 100 characters, then `updateProfile({displayName})` (the name reaches the token's `name` claim after a token refresh, which the page forces); Google uses the account name; a Google account with no or an over-long name is handled by the backend (D-20). R-001-03 password under 8 characters is refused by the form before Firebase is called. R-001-04 `sendEmailVerification` after `createUserWithEmailAndPassword`. R-001-05 `VerifyEmailPage` (send again, sign out) is the only screen while `emailVerified` is false; the guard blocks every group route; the API independently answers 403 `EMAIL_NOT_VERIFIED` (BR-03). R-001-06 `sendPasswordResetEmail`. R-001-07 Firebase's error is shown with the text of `auth.<firebase code>` in the language files; nothing opens. R-001-08 `browserLocalPersistence` (the SDK's own store, SR-20); "Sign out" calls `signOut` and routes to `/sign-in`. R-001-09 a verified user whose `GET /groups` is empty sees the empty state with one button "Create a group".
- Covers: R-001-01..09, AC-001-01..08, EX-001-01..07. Tests: Vitest component tests with a fake `Auth`; E2E-01 against the emulator.

### 4.2 US-002 Create a group and list my groups (`GroupController`, `GroupService`, `AccessService`)

- R-002-01 `GroupService.create`: verified user, valid name → insert `groups` and an OWNER `memberships` row in one transaction, return the group. R-002-02 `ContactRules`-style `GroupName` rule (trim, 1 to 100). R-002-03 name rule: under the advisory locks of ADR-03, refuse (409 `GROUP_NAME_TAKEN`) when any Owner of the group has another group not in Trash with an equal name (`lower()`); applied on create (the creator), rename (every Owner, the group itself excluded, so a capitalisation change passes) and restore (R-015-10); promotion never calls it. R-002-04 `GroupRepository.findByMember(userId)` returns groups with state `ACTIVE` and the user's role. R-002-05 no limit. R-002-06, R-002-07 `GET /groups/{groupId}` and every other group route start with `AccessService`: a non-member gets the one 404. R-002-08 `PATCH /groups/{groupId}`: Owner only (403 else), same rules, refused for a group in Trash (D-20 reading 3: the group is invisible to non-Owners and the Owner is refused with 404 `NOT_FOUND`, because only restore is allowed for a trashed group, D-17); History `GROUP_RENAMED` with old and new name.
- Covers: R-002-01..08, AC-002-01..10, EX-002-01..05.

### 4.3 US-003 Invite people to a group (`InvitationController`, `InvitationService`)

- R-003-01 `POST /invitations` (Owner): insert row (lowercase e-mail, role any of three). R-003-02 e-mail rule of section 3.1. R-003-03 refuse 409 `ALREADY_MEMBER` (e-mail of a member, compared `lower()` with `users.email`) and 409 `INVITATION_PENDING`. R-003-04 count of pending (not expired) rows at 20 → 409 `INVITATION_LIMIT`; counted under the group row lock so two requests cannot pass at 19. R-003-05 `InvitationService.acceptFor(user)` runs at the start of `GET /me` and `GET /groups` for a user with verified e-mail: for each pending, non-expired invitation with the user's e-mail, create the membership with the invited role and delete the invitation (D-20 reading 4); a user already a member keeps the existing role and the invitation is deleted. R-003-06 pending means `created_at > now - 14 days` (ADR-08). R-003-07 `DELETE` removes the row. R-003-08 `GET /invitations` (Owner) lists e-mail, role, `expiresAt`. R-003-09 no mail code exists (SR-17; a dependency test). R-003-10 the verified-e-mail filter stops everything before `acceptFor`. R-003-11 403 for non-Owners; History `MEMBER_INVITED` and `INVITATION_CANCELLED` come with US-013.
- Covers: R-003-01..11, AC-003-01..10, EX-003-01..08.

### 4.4 US-004 Manage members and roles (`MemberController`, `MemberService`)

- R-004-01 `GET /members` for every member (name, e-mail, role). R-004-02 `PUT /members/{userId}` (Owner): update role; applies at the next request because the role is read per request; same role is a no-op. R-004-03 several Owners are allowed. R-004-04 `DELETE /members/{userId}` removes the membership only; contacts and History keep the name. R-004-05 afterwards `AccessService` answers 404. R-004-06 `POST /leave` is a removal of oneself by oneself. R-004-07 inside the group row lock, count Owners after the change; zero → 409 `LAST_OWNER`; two parallel requests serialise on the lock so one fails (AC-004-06). R-004-08 403 for non-Owners. R-004-09 no system administrator: no endpoint lists other users' groups. History `ROLE_CHANGED`, `MEMBER_REMOVED` (a leaving recorded as a removal by the member) come with US-013.
- Covers: R-004-01..09, AC-004-01..08, EX-004-01..05.

### 4.5 US-005 Create, view and edit a contact (`ContactController`, `ContactService`, `ContactRules`)

- R-005-01 `POST /contacts` (Owner, Editor): validate, insert, record version 1, History `CONTACT_CREATED`. R-005-02 one-of-four. R-005-03 all broken fields in one 400 (EX-005-10). R-005-04 lowercase e-mail. R-005-05 no uniqueness. R-005-06 `GET /contacts/{id}` for every member returns fields, tags, attributes, `revision`, `versionNumber`. R-005-07 `PUT` saves with the revision (ADR-03): row lock, state check, revision check, no-op check, update, `revision + 1`, version, History, one transaction. R-005-08 a different revision → 409 `CONFLICT` with the current contact and nothing written (also for a stale save after a group-wide tag change, D-19). R-005-09 403 for Viewers on create and save. R-005-10 a contact in Trash → 409 `CONTACT_IN_TRASH` (EX-005-12). A contact of another group or a malformed id → 404 (EX-005-11).
- Covers: R-005-01..10, AC-005-01..10, EX-005-01..12.

### 4.6 US-006 Tag contacts (`ContactService` save path, `TagService`)

- R-006-01, R-006-02 the save takes tag names; `TagService.resolve(groupId, names)` finds tags by `lower(name)` or creates with the typed spelling; duplicates on one contact collapse (EX-006-05). R-006-03, R-006-04 `TagName` rule and 50 limit, refusing the whole save. R-006-05 tags are per group by the foreign key and every query filters by group. R-006-06 tags missing from the saved list are detached from this contact only. R-006-07 after every save, trash erasure and tag operation, tags with no `contact_tags` row are deleted. R-006-08 no code reads tags except display, search, filters, export (no behaviour, AC-006-08). R-006-09 Viewers read tags; saving is 403. R-006-10 `GET /tags?q=` suggests tags of the group, never forced. History `TAG_CHANGED` per added or removed tag (US-013); version recorded when the tag set changes (US-019).
- Covers: R-006-01..10, AC-006-01..08, EX-006-01..08.

### 4.7 US-007 Rename and delete tags (`TagController`, `TagService`)

- R-007-01 `PATCH /tags/{tagId}` (Owner, Editor): rename the tag row, which renames it on every contact including those in Trash. R-007-02 another tag with an equal lower-case name → 409 `TAG_NAME_TAKEN`. R-007-03 capitalisation-only change updates the spelling. R-007-04 `TagName` rule. R-007-05 `DELETE` removes the tag and its links; no contact is deleted. R-007-06 no version; `revision` of each affected contact is incremented (D-19). R-007-07 403 for Viewers. R-007-08 one History entry per affected contact (`TAG_RENAMED`, `TAG_DELETED`) with the names, written in the same transaction (ADR-05). EX-007-01 same name exactly: nothing changes, no entry.
- Covers: R-007-01..08, AC-007-01..08, EX-007-01..05.

### 4.8 US-008 Attributes (`ContactService` save path)

- R-008-01 saved with the contact. R-008-02 duplicate key per contact ignoring case → `DUPLICATE_KEY`. R-008-03 key 1 to 50 after trimming, value up to 255, empty value allowed. R-008-04 at most 50. R-008-05 `GET /attribute-keys?q=` suggests keys of the group. R-008-06 values are text only. R-008-07 attributes absent from the saved list are removed from this contact only. R-008-08 Viewers read them; saving is 403. History `ATTRIBUTE_CHANGED`; version on change.
- Covers: R-008-01..08, AC-008-01..07, EX-008-01..06.

### 4.9 US-009 List and search (`SearchController`, `SearchService`)

- R-009-01 `POST /contacts/search`: active contacts of the group, 25 per page, default sort last name then first name with `COLLATE "sl-SI-x-icu"`, total of matches; a page past the last returns the last (EX-009-09). R-009-02 sortable columns last name, first name, e-mail, phone, company, city, country, both directions, from the fixed list (SR-07); empty last name sorts last ascending (EX-009-07). R-009-03 every word contained in a folded field, tag or attribute value (ADR-04); several words must all match. R-009-04 phone and notes never searched. R-009-05 the response has `total: 0`; the web app shows "No contacts match" and "Clear filters". R-009-06 empty group: empty state, "Add contact" only for Owner and Editor (the web app knows the role from `GET /groups/{id}`). R-009-07 no Trash, no other group. R-009-08 the web app keeps search text and filters while paging or sorting (it sends them in every request). R-009-09 a row opens the contact screen. R-009-10 same endpoint and result for Viewers.
- Covers: R-009-01..10, AC-009-01..11, EX-009-01..09.

### 4.10 US-010 Filters (`SearchService`)

- R-010-01 `TAGS_ALL`, R-010-02 `TAGS_ANY`, R-010-03 `TAG_NOT` (includes untagged contacts), R-010-04 `ATTRIBUTE_EQUALS` (key `lower`, value folded), R-010-05 `ATTRIBUTE_EXISTS` (also an empty value), R-010-06 `COUNTRY`, R-010-07 `COMPANY` (folded equality; `GET /companies?q=` suggests). R-010-08 all conditions and the search text combine with AND in one parameterised query. R-010-09 the contract has no other condition type and no saved filter. R-010-10 the web app re-sends the list request when a condition is removed or cleared. R-010-11 active conditions are shown as removable chips. Unknown tag names match nothing (EX-010-02, EX-010-05).
- Covers: R-010-01..11, AC-010-01..10, EX-010-01..08.

### 4.11 US-011 Export (`ExportController`, `ExportService`)

- R-011-01 the list shows tick boxes, "select all on this page" and "select all N results" for Owner and Editor only. R-011-02 selection survives paging and sorting and is cleared when search text or filter changes (web app `Selection` state). R-011-03 Export disabled without a tick. R-011-04 one sheet, header plus one row per contact in the current sort, own group only. R-011-05 columns, headers, country names in the request language, alphabetical attribute keys (D-21). R-011-06 file name and characters (D-21). R-011-07 header bold and frozen, e-mail and phone cells as text. R-011-08 a value starting `=`, `+`, `-`, `@` is stored as a string with the quote-prefix style. R-011-09 over 10,000 → 400 `TOO_MANY_CONTACTS`, no file. R-011-10 403 for Viewers, no tick box or button. R-011-11 UTF-8 package. R-011-12 only active contacts of the group. History `EXPORTED` with the count (R-013-04).
- Covers: R-011-01..12, AC-011-01..13, EX-011-01..08.

<!-- next: 4.12 -->

