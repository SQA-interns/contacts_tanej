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

Language checks: code, API, tables and tests use these words; the Do-not-use words of the glossary appear nowhere (a lint test greps the sources for them, task T-X01).

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

### 4.12 US-012 Trash and "Delete forever" (`TrashController`, `TrashService`, `TrashCleanupService`)

- R-012-01 `POST /contacts/{id}/trash` (Owner, Editor): row lock, state `TRASHED`, `trashed_at = now`, out of list, search, filters, export and duplicates by the `state = 'ACTIVE'` predicate, data kept; a second request on a trashed contact → 409 `ALREADY_IN_TRASH` (EX-012-03). R-012-02 `GET /trash-contacts` (Owner, Editor) with `daysLeft = 30 - full days` (never negative); Viewers 403. R-012-03 `POST /restore` back to `ACTIVE`, 30 days restart at the next trash (EX-012-05); not in Trash → 409 `NOT_IN_TRASH`; erased → 404 (EX-012-04); no version (EX-019-02). R-012-04 `DELETE /contacts/{id}` (Owner) on a contact in Trash: erasure of ADR-05 at once (a contact not in Trash → 409 `NOT_IN_TRASH`). R-012-05 403 for Editor and Viewer. R-012-06 `TrashCleanupService.run` erases contacts with `trashed_at <= now - 30 days`. R-012-07 edits refused (US-005). R-012-08 403 for Viewers on delete and restore. R-012-09 History `CONTACT_DELETED`, `CONTACT_RESTORED`, `CONTACT_ERASED`. EX-012-08 orphan tags are removed in the erasing transaction. SR-15 is checked by a database inspection test after erasure.
- Covers: R-012-01..09, AC-012-01..09, EX-012-01..08.

### 4.13 US-013 History (`HistoryRecorder`, `HistoryService`, `HistoryController`)

- Foundation first (D-22 reading 3, task T-07): `HistoryRecorder.record(ChangeSet)`, the guard triggers, `HistoryRepository`, the read model; no endpoint yet. R-013-01 `CONTACT_CREATED`, `CONTACT_EDITED`, `CONTACT_DELETED`, `CONTACT_RESTORED`, `CONTACT_ERASED` entries (clauses: one test per event). R-013-02 `TAG_CHANGED`, `ATTRIBUTE_CHANGED` with tag name or key and value. R-013-03 `TAG_RENAMED`, `TAG_DELETED` per affected contact. R-013-04 `EXPORTED` with count. R-013-05 group-level entries for invite (role only, never the e-mail, D-16), role change, removal (a leaving is a removal by the member), cancellation, rename (old and new name), delete, restore (clauses: one test per event; no entry for joining or creating, D-22). R-013-06 `CONTACTS_MERGED`, `VERSION_RESTORED` (US-022, US-020). R-013-07 each entry stores name, e-mail, UTC time and per changed field name, old and new value; one save shares one `changeSetId`; the web app formats the time in `Europe/Ljubljana`. R-013-08 no entry for viewing or searching. R-013-09 `GET /contacts/{id}/history` (Owner, Editor), newest first, with the merged-away contacts' entries linked in (R-022-06). R-013-10 no change or delete endpoint exists (a test over every operation of the contract, SR-14) and the triggers refuse updates other than the anonymisations. R-013-11 entries live with the group. R-013-12 erasure sets `erased`, removes values and label (ADR-05). R-013-13 403 for Viewers.
- Covers: R-013-01..13, AC-013-01..12, EX-013-01..08. Each earlier story verifies its own entries with the foundation (D-22); US-013 owns the tab, content rules and group-level events.

### 4.14 US-014 Group-wide History (`HistoryController`, `HistoryService`)

- R-014-01 `GET /history` (Owner, Editor) newest first, 25 per page, entries of this group only (R-014-08; `WHERE group_id = ?`). R-014-02 `actorId` filter; `GET /history/actors` lists people with entries, also those who left (EX-014-03). R-014-03 `types` filter (multi-select). R-014-04 `from`/`to` are dates in the project time zone converted to a UTC half-open range (EX-014-01). R-014-05 AND of the three. R-014-06 empty result: "No entries match" and "Clear filters". R-014-07 403 for Viewers. EX-014-02 `to` before `from` → 400 `INVALID_DATE_RANGE`; EX-014-04 25 and 26 entries; EX-014-05 empty state text.
- Covers: R-014-01..08, AC-014-01..08, EX-014-01..05.

### 4.15 US-015 Delete and restore a group (`GroupController`, `GroupService`, `TrashCleanupService`)

- R-015-01 `POST /trash` (Owner) with the typed name; R-015-02 the name is trimmed and compared case-sensitively, else 400 `NAME_MISMATCH`, nothing deleted; state `TRASHED`. R-015-03 `AccessService` answers 404 to every request of a non-Owner member, and (D-17) to an Owner for everything except `GET /deleted-groups` and `POST /restore`. R-015-04 `GET /deleted-groups` lists name, `trashedAt`, `daysLeft` for the user's Owner groups in Trash and nothing more. R-015-05 restore returns state `ACTIVE` with members, contacts, tags, attributes, History and invitations untouched (nothing was removed). R-015-06 cleanup deletes the group and cascades (ADR-08). R-015-07 403 for non-Owners; a non-member gets 404. R-015-08 changes to contacts, members, invitations of a group in Trash get 404 (D-17) (the story's "refuse"). R-015-10 restore applies the name rule for every Owner, 409 `GROUP_NAME_TAKEN`. R-015-09 History `GROUP_DELETED`, `GROUP_RESTORED`. EX-015-05 two Owners delete at once: the second gets 409 `GROUP_IN_TRASH`.
- Covers: R-015-01..10, AC-015-01..08, EX-015-01..06.

### 4.16 US-016 Delete my account (`AccountController`, `AccountService`; web app `features/account`)

- R-016-01 `DELETE /me` when the user is not the only Owner of any group; removes memberships. R-016-02 otherwise 409 `SOLE_OWNER` with `groups` (also groups in Trash, EX-016-01; every such group named, EX-016-05). R-016-03 `actor_email = NULL` on the user's entries (name stays). R-016-04 contacts created by the user stay (`created_by` has no foreign key). R-016-05 the user leaves every member list; invitations stay (EX-016-03). R-016-06 the user id goes to `deleted_accounts`; the next request with an old token is 401. R-016-07 the web app asks for confirmation; the backend requires `auth_time` within 5 minutes, else 403 `RECENT_LOGIN_REQUIRED`, after which the web app asks for a new sign-in (`reauthenticateWithCredential`) and repeats; then the web app calls `deleteUser`. A user with no group or only Viewer roles is allowed (EX-016-02, EX-016-04).
- Covers: R-016-01..07, AC-016-01..06, EX-016-01..05.

### 4.17 US-017 Secured API and OpenAPI (`config` filters, `ApiExceptionHandler`, springdoc)

- R-017-01, R-017-02 `FirebaseJwtDecoders` (ADR-02): missing, malformed, expired, foreign-project and non-Bearer tokens → 401 in the error format. R-017-03 `VerifiedEmailFilter` → 403 `EMAIL_NOT_VERIFIED`. R-017-04 group id only from the path (ArchUnit-style source test: no `groupId` in a request body type, AR-04). R-017-05 one 404. R-017-06 403 by role. R-017-07 400 with the rule code and field. R-017-08 `RateLimitFilter`. R-017-09 `BodySizeLimitFilter`. R-017-10 `ApiExceptionHandler`, 405 for a wrong method. R-017-11 `/v3/api-docs` and the drift test of ADR-09. R-017-12 `/health` 200, and 503 when the database check fails. R-017-13 UTF-8 JSON, English field names. R-017-14 no API key code. R-017-15 409 `CONFLICT` with `current`.
- Covers: R-017-01..15, AC-017-01..14, EX-017-01..09. The isolation matrix (AC-017-07) is generated from the contract (ADR-09).

### 4.18 US-018 Language (frontend `lib/i18n.ts`, `lib/format.ts`, `locales/`)

- R-018-01 Slovenian by default, whatever the browser language (EX-018-01). R-018-02 the header switch changes the language at once; React state of forms is untouched, so unsaved input stays (AC-018-02). R-018-03 `localStorage` key `language`. R-018-04 `formatDateTime(iso, lang)` gives `d. M. yyyy HH:mm` or `yyyy-MM-dd HH:mm`; decimals follow the language; formal "vi" in the Slovenian texts. R-018-05 always `Europe/Ljubljana`, summer time included (AC-018-04, EX-018-03). R-018-06 `Intl.DisplayNames` for countries. R-018-07 sorting and search are the backend's Slovenian rules in both languages (AC-018-06). R-018-08 contact data is never translated. EX-018-02 server errors show `error.<code>`. EX-018-04 the locale test fails on a missing key, and a missing key is never shown.
- Covers: R-018-01..08, AC-018-01..07, EX-018-01..04.

### 4.19 US-019 Versions are recorded (`VersionService`, `ContactService`)

- R-019-01 version 1 on create. R-019-02 a save with a changed field, tag or attribute records `current_version + 1`; a save with no change, a refused save, trash and restore record none (AC-019-03, AC-019-08, EX-019-02, EX-019-04). R-019-03 tag rename and delete record none. R-019-04 merge records one on the survivor if its content changed (D-22); a version restore records a new version. R-019-05 numbers are never reused or lowered (PK and the increment under the row lock). R-019-06 `GET /versions` (Owner, Editor), 403 for Viewers. R-019-07 versions cascade with the contact. R-019-08 no limit (EX-019-01). A deleted author's version keeps the name (EX-019-03).
- Covers: R-019-01..08, AC-019-01..08, EX-019-01..04.

### 4.20 US-020 View, compare and restore versions (`VersionController`, `VersionService`; `features/versions`)

- R-020-01 Versions tab lists number, who, when, newest first (Owner, Editor). R-020-02 `GET /versions/{number}` read-only; a number that does not exist or a version of another contact → 404. R-020-03 the web app compares the version with the current contact and highlights differing fields, tags and attributes. R-020-04 `POST /versions/{number}/restore` with the revision: row lock, the snapshot becomes the contact's fields, tags and attributes (tags resolved or recreated, R-006-01), new version, `revision + 1`, History `VERSION_RESTORED` naming the number and changed fields. R-020-05 contact in Trash → 409 `CONTACT_IN_TRASH`. R-020-06 equal to the current → 409 `NOTHING_TO_CHANGE`, nothing written. R-020-07 403 for Viewers. R-020-08 stale revision → 409 `CONFLICT`. R-020-09 confirmation dialog.
- Covers: R-020-01..09, AC-020-01..09, EX-020-01..06.

### 4.21 US-021 Find duplicates (`DuplicateController`, `DuplicateService`; `features/duplicates`)

- ADR-11. R-021-01 `POST /duplicates/search` over the whole group, not in Trash, whatever the list filter. R-021-02 e-mail equal ignoring case, non-empty. R-021-03 first and last name both non-empty and equal folded, plus company equal folded or phone equal after keeping digits and `+`. R-021-04 name alone never matches. R-021-05 a contact with no e-mail, first name, last name and company never matches. R-021-06 one group. R-021-07 sets with `kind` and `reasons`. R-021-08 read-only. R-021-09, R-021-10 `POST /duplicates/dismissals` stores the set key; a found set is hidden only if its key equals a stored one. R-021-11 403 for Viewers. R-021-12 empty result: "No duplicates found".
- Covers: R-021-01..12, AC-021-01..13, EX-021-01..08.

### 4.22 US-022 Merge (`MergeService`; `features/duplicates` merge screen)

- ADR-11, split into two sessions (F-15). R-022-01 the merge screen shows the picked contacts side by side. R-022-02 first non-empty value of the oldest contact first, then the next. R-022-03 tags combined, each once. R-022-04 attributes combined; a key with different values needs a choice and "Confirm" stays disabled until chosen; the server answers 400 `ATTRIBUTE_CHOICE_REQUIRED` otherwise. R-022-05 survivor updated, others to Trash, one History entry, one version if changed. R-022-06 survivor's History includes merged-away contacts' entries (`contact_merges`). R-022-07 restoring a merged-away contact touches only it. R-022-08 the screen says the survivor can be reverted by restoring an earlier version. R-022-09 result validated by `ContactRules` (50 tags, 50 attributes, formats). R-022-10 fewer than two, repeated id, a contact in Trash → 400 `TOO_FEW_CONTACTS`; another group's id → 404. R-022-11 never automatic. R-022-12 stale revision → 409 `CONFLICT`. R-022-13 403 for Viewers. R-022-14 cancel changes nothing.
- Covers: R-022-01..14, AC-022-01..14, EX-022-01..08.

## 5. Error model

One format for every failure (`ErrorResponse`, contract `schemas.yaml`): `{status, code, message, errors?, groups?, current?}`; no stack trace, class name or SQL (SR-10, R-017-10). The web app shows the text of `error.<code>` in the active language (EX-018-02); `errors[].code` selects the field message.

| Code | Status | When | Extra | Rules |
|---|---|---|---|---|
| `UNAUTHENTICATED` | 401 | token missing, malformed, not Bearer, expired, foreign project, or of a deleted account | — | R-017-01, R-017-02, R-016-06, EX-017-01 |
| `EMAIL_NOT_VERIFIED` | 403 | valid token, e-mail not verified; every endpoint except `/health` | — | R-017-03, BR-03 |
| `RECENT_LOGIN_REQUIRED` | 403 | account deletion without a sign-in of the last 5 minutes | — | R-016-07 |
| `FORBIDDEN` | 403 | member's role does not allow the operation | — | R-017-06, BR-02 |
| `NOT_FOUND` | 404 | not a member, no such group or id, malformed id, group in Trash for a non-Owner (and for Owners except restore, D-17), contact or version of another group | none: same body in every case | R-017-05, SR-03, BR-01 |
| `METHOD_NOT_ALLOWED` | 405 | wrong HTTP method on a path | — | EX-017-07 |
| `VALIDATION_FAILED` | 400 | a rule of a story is broken | `errors[]` with field, rule code, message | R-017-07, BR-05 |
| `CONFLICT` | 409 | stale `revision` on save, version restore, merge | `current` = the contact | R-005-08, R-017-15, R-020-08, R-022-12 |
| `ALREADY_IN_TRASH`, `NOT_IN_TRASH`, `CONTACT_IN_TRASH`, `GROUP_IN_TRASH` | 409 | state does not fit the operation | — | EX-012-03, R-005-10, R-020-05, EX-015-05 |
| `LAST_OWNER` | 409 | removal, demotion or leaving would leave no Owner | — | R-004-07 |
| `SOLE_OWNER` | 409 | account deletion while sole Owner of a group | `groups[]` | R-016-02 |
| `GROUP_NAME_TAKEN` | 409 | name rule on create, rename, restore | — | R-002-03, R-015-10 |
| `ALREADY_MEMBER`, `INVITATION_PENDING`, `INVITATION_LIMIT` | 409 | invitation rules | — | R-003-03, R-003-04 |
| `TAG_NAME_TAKEN` | 409 | rename onto another tag's name | — | R-007-02 |
| `NOTHING_TO_CHANGE` | 409 | restore of a version equal to the current contact | — | R-020-06 |
| `PAYLOAD_TOO_LARGE` | 413 | body over 1 MB | — | R-017-09 |
| `RATE_LIMITED` | 429 | more than the configured requests a minute | header `Retry-After` | R-017-08 |
| `INTERNAL_ERROR` | 500 | anything unexpected; fixed message, class name only in the log | — | R-017-10 |
| `UNAVAILABLE` | 503 | `/health` when the database does not answer | — | R-017-12 |

Field rule codes (`errors[].code`): `REQUIRED`, `REQUIRED_ONE_OF`, `TOO_SHORT`, `TOO_LONG`, `INVALID_FORMAT`, `INVALID_CHARACTERS`, `UNKNOWN_COUNTRY`, `TOO_MANY`, `DUPLICATE_KEY`, `INVALID_DATE_RANGE`, `NAME_MISMATCH`, `TOO_MANY_CONTACTS`, `TOO_FEW_CONTACTS`, `ATTRIBUTE_CHOICE_REQUIRED`, `UNKNOWN_VALUE`. Check order inside one request: authentication (401) → verified e-mail (403) → rate limit (429) and body size (413) → path and membership (404) → role (403) → body validation (400) → state and concurrency (409).

## 6. Security controls

| Control | Where | Check |
|---|---|---|
| SB-01 input validated on the server | `ContactRules`, `GroupName`, `TagName`, `EmailAddress`, field rules of every request record; the database CHECKs repeat them | unit tests per rule; BR-05 tests with the web app bypassed |
| SB-02 authentication and authorisation on every non-public operation | `SecurityConfig` (`/health` only is public), `AccessService` per group request | AC-017-01, AC-017-07, one role test per permission-matrix row |
| SB-03 credentials | Firebase stores passwords; the product stores none; the only secrets are `DB_PASSWORD` and the web API key in `.env` | no password field in the schema; secret scan |
| SB-04 TLS | `docker-compose.prod.yml` proxy with certificate, HTTP redirects to HTTPS; plain HTTP only on localhost (SR-04) | header and redirect test against the prod Compose set-up |
| SB-05 output encoded, queries parameterised | React escapes output; no `dangerouslySetInnerHTML` (lint rule); JDBC named parameters only; the export writes strings | static analysis; EX-009-03, EX-010-06 |
| SB-06 rate limit | `RateLimitFilter` per user, configurable; the only public endpoint is `/health` | AC-017-08 |
| SB-07 no internals in errors or logs | `ApiExceptionHandler`, `RequestLogFilter` | AC-017-10; log test with known values (SR-11) |
| SB-08 dependency scan | `dependency-check-maven`, `npm audit` via `scripts/audit.mjs`; D-08, D-13 recorded | CI jobs |
| SB-09 static analysis and secret scan | PMD, ESLint with `jsx-a11y`, gitleaks | CI jobs |
| SB-10 security headers | `SecurityConfig` for the API; nginx for the web app | header tests (SR-06) |
| SB-11 least privilege | non-root users in all images (uid 10001 backend, 101 nginx, `node` emulator) | image inspection test |
| SB-12 data minimisation | only the fields of `business-rules.md`; no analytics | SR-20 code search |
| SB-13 purpose and retention | table "Personal data" of `security-requirements.md`; BR-04, ADR-08 | cleanup tests |
| SB-14 consent | none collected by the product; legal matters are the customer's (D-14) | — |

| SR | Control | Where and check |
|---|---|---|
| SR-01 | token verification | `FirebaseJwtDecoders`; tests with missing, expired, tampered, foreign-project tokens (AC-017-01, AC-017-02) |
| SR-02 | group id and membership in the same query | every repository method takes `groupId`; AC-017-07; E2E-04; AR-03 architecture test |
| SR-03 | one 404 | `AccessService.notFound()`; test compares raw responses (AC-017-04, EX-017-02, EX-017-06) |
| SR-04 | HTTPS only, HSTS | prod proxy (ADR-10); install guide; test against the prod Compose set-up |
| SR-05 | CORS to one origin | `SecurityConfig.cors`; request with another `Origin` gets no CORS header |
| SR-06 | security headers | `SecurityConfig` and nginx template; header tests on both |
| SR-07 | parameterised SQL, fixed sort list | `SearchService` builder; injection-string tests (EX-009-03, EX-010-06); static analysis for string-built SQL |
| SR-08 | export formula safety | `ExportService` quote-prefix cells; AC-011-08 |
| SR-09 | rate and size limits | `RateLimitFilter`, `BodySizeLimitFilter`; AC-017-08, AC-017-09 |
| SR-10 | one error format | `ApiExceptionHandler`; AC-017-10 |
| SR-11 | log content | `RequestLogFilter`; known values absent from logs |
| SR-12 | secrets only in environment or `.env` | `.gitignore`, Compose `${VAR:?}`; gitleaks |
| SR-13 | encryption at rest by the hosting | install guide statement (phase 7) |
| SR-14 | History cannot be changed | no endpoint; triggers; test over every operation of the contract; AC-013-09 |
| SR-15 | no personal data after erasure or purge | `TrashService`, `TrashCleanupService`; database inspection test after AC-012-03 and AC-015-05 |
| SR-16 | roles on the server | `AccessService.require(role)`; one test per matrix row and role (AC-005-08, AC-011-10, AC-012-04, AC-013-08) |
| SR-17 | no mail sent | no mail dependency; test over the dependency list; AC-003-01 |
| SR-18 | tests use the emulator and fixed users | test configuration; secret scan |
| SR-19 | no open Critical or High without a decision | dependency scans; decisions D-08, D-13 list the exceptions |
| SR-20 | no token or contact data in URLs, storage or analytics | search and filter use POST; code search; network trace in the main flows |

## 7. Threat model (STRIDE)

Interfaces: **I1** web app → API (REST, bearer token); **I2** web app → Firebase Authentication; **I3** API → Google public keys; **I4** API → PostgreSQL; **I5** Excel export → browser; **I6** health check; **I7** browser UI. A threat without a control is an `[ASSUMPTION]`.

| Threat (STRIDE) | Interface or data | Control |
|---|---|---|
| Spoofing: forged or foreign token | I1 | SR-01 signature, issuer, audience, expiry; emulator tokens accepted only when the emulator host is set (ADR-02); startup refuses the host outside local and test |
| Spoofing: token of a deleted account | I1 | `deleted_accounts` check (R-016-06) |
| Spoofing: unverified e-mail claims an invitation | I1 | BR-03, R-003-10, `VerifiedEmailFilter` |
| Tampering: group id or role in the body | I1 | AR-04, SR-02; role read from the database per request (SR-16) |
| Tampering: SQL injection through search, filter, sort | I1, I4 | SR-07; fixed sort list; bind parameters |
| Tampering: change or delete of a History entry | I1, I4 | SR-14; append-only triggers |
| Tampering: lost update of a contact | I1 | AR-14 revision check; row locks (ADR-03) |
| Tampering: formula in an exported cell | I5 | SR-08 |
| Repudiation: a change cannot be attributed | I1 | History in the same transaction (AR-09); actor name stays after account deletion (D-16) |
| Information disclosure: reading another group's data | I1, I4 | BR-01, SR-02, SR-03 (identical 404), PP-01, AC-017-07 |
| Information disclosure: existence of a group probed by id | I1 | SR-03 identical 404 for non-member, missing, malformed, Trash |
| Information disclosure: personal data in logs, errors, URLs | I1 | SR-10, SR-11, SR-20 |
| Information disclosure: invited e-mail seen by Editors or kept in History | History, invitations | D-16: invitation entries hold role only; invitations visible to Owners only; row deleted on accept, cancel, expiry |
| Information disclosure: erased contact's data in History, tables, backups | History, versions | BR-06, SR-15; backups keep it up to 14 days (accepted, OQ-P-11, install guide) |
| Information disclosure: export by a Viewer | I5 | R-011-10, SR-16; every export logged (R-013-04) |
| Information disclosure: token or contact data in browser storage | I2, I7 | SR-20: only the Firebase SDK's own store and the language choice |
| Denial of service: request flood, large bodies, 10,000-contact export | I1, I5 | SR-09 rate limit and 1 MB; export capped at 10,000 and streamed (ADR-12) |
| Denial of service: expensive duplicate search | I1 | read-only, Owner or Editor, linear hash-map algorithm, rate limit |
| Elevation: Editor acts as Owner (invite, Delete forever, group delete) | I1 | SR-16 permission-matrix tests |
| Elevation: last Owner removed by two parallel requests | I1 | row lock (R-004-07) |
| Elevation: a removed member keeps using an old token | I1 | membership read per request (EX-017-03, EX-004-03) |
| Elevation: container breakout through a root process | deployment | SB-11 non-root images |
| Spoofing or tampering: emulator reachable in production | I2, I3 | production Compose has no emulator; startup guard; `FIREBASE_AUTH_EMULATOR_HOST` empty |
| Information disclosure: health check reveals internals | I6 | answers status only, `show-details: never` |
| Cross-site scripting through contact text | I7 | React output encoding; CSP without inline script (SR-06); no `dangerouslySetInnerHTML` |
| Clickjacking | I7 | `frame-ancestors 'none'` |
| Personal data items: contact fields, tags, attributes, notes | contacts | access by role (BR-02); retention BR-04; export logged; notes may hold special categories — `[ASSUMPTION]` the customer accepts this and handles legal matters outside the software (OQ-005-03, OQ-P-04) |
| Personal data items: user name and e-mail | users, History | member list to members, History to Owner and Editor; e-mail removed on account deletion (D-16) |
| Personal data items: versions | `contact_versions` | Owner and Editor only (R-019-06); erased with the contact |

## 8. NFR checks

Measured on the deployed (Compose) system with seeded data, results written to `docs/06_verification-report.md`; scripts live in `02_output/scripts/` and `backend/src/test` (tasks T-N01 to T-N12).

| NFR | How it is checked | Pass value |
|---|---|---|
| NFR-01 | Timing test: 10,000 seeded contacts, 20 parallel virtual users, list, search and the filters of US-010 | 95th percentile under 1,000 ms |
| NFR-02 | Same test opens contacts (`GET /contacts/{id}`) | 95th percentile under 500 ms |
| NFR-03 | Export of 1,000 contacts with tags and attributes | ready in under 10 s |
| NFR-04 | Load of NFR-01 plus 100 users and several groups | no 5xx and no 429 at normal load; 100 users and 20 at once |
| NFR-05 | Write data, restart the containers, read it back; call `/health` | data intact; `/health` 200; 99 % target written in the install guide |
| NFR-06 | Scripted backup, restore into a clean Compose stack and data comparison; daily backup and 14-day retention documented | identical row counts and checksums; log in `out/logs/` |
| NFR-07 | Playwright main flows in Chromium, Firefox and WebKit; manual Safari and Edge check once | all pass |
| NFR-08 | Screenshots at 320, 768 and 1280 px for list, filter, contact screens; assertion on horizontal scroll | no horizontal scroll at 320 px |
| NFR-09 | axe scan (WCAG 2.1 AA tags) per main screen in both themes and languages; keyboard-only run of the main flows | no Critical or Serious violation; visible focus |
| NFR-10 | `locales.test.ts` key parity; language switch on each screen of the main flows | same keys; no key shown |
| NFR-11 | Review of empty and error state screenshots against the wireframes; DoD-P02 timing | every screen has the states; first contact in under 120 s of script time |
| NFR-12 | Start the delivered Compose set-up on a clean host with only `.env`; E2E-01 | passes |
| NFR-13 | Start on an empty database and on the previous release's database; list the migrations | both start; migrations versioned, forward only |
| NFR-14 | Licence scan, dependency list review, search for `hibernate`, `jakarta.persistence`, SCM code, Google services other than Authentication | none forbidden; `ArchitectureTest` AR-02 |
| NFR-15 | Export of 10,000 contacts | completes with no error and no out-of-memory; time recorded |

Quality gate (`quality-requirements.md`): line coverage at least 95 %, adjusted mutation score at least 90 %, per component, measured in phase 5.

## 9. User interface

Follows `general/design/ui-baseline.md` (UI-01 to UI-30) with one override from `ui-design.md`: UI-08 is designed desktop first and responsive to 320 px (NFR-08). Working title "Contact Groups" / "Skupine kontaktov" lives in the language files only (OQ-P-08, D-14). The wireframes (`/wireframes`) draw every screen below at the three widths of UI-22 (mobile 375, tablet 768, desktop 1440) before any screen is built.

### 9.1 Tokens and components

- **Tokens** in `src/styles/tokens.wireframe.css` (greys, dashed 1 px borders, no shadow or animation, UI-26) and `tokens.final.css` (neutral greys, indigo accent, light and dark by system setting, UI-04, UI-05); the active set is `data-theme` on `<html>` (`wireframe` now, `final` after the last increment, UI-03). Semantic names: background, surface, surface-raised, border, text, text-muted, primary, primary-foreground, success, warning, danger, focus-ring. Type 12/14/16/20/24/32 px, body 16 px, weights 400 and 600, system font stack (UI-06); spacing on a 4 px grid, radius 8 px controls and 12 px cards, one shadow level (UI-07); breakpoints 640, 768, 1024, 1280 px, content at most 1200 px (UI-08, UI-09).
- **Components** (`components/ui`, shadcn/ui on Radix, Lucide icons, UI-01): `Button` (primary, secondary, ghost; hover, active, focus, disabled, busy, UI-12), `Input`, `Label`, `Card`, `Skeleton`, `StatePanel` (loading, empty, error, UI-14); to be added by the stories that need them, each as its own file: `Dialog`, `Select`, `Tabs`, `Table` (stacked cards below 768 px, UI-16), `Checkbox`, `Toast`, `Badge` (tag chip), `Pagination`, `DropdownMenu`. Icons always carry a text label or accessible name (UI-17).
- **Layout** (`AppShell`): header with logo placeholder, product name, navigation, language switch; one `main`; footer (UI-10, UI-19, UI-27). Mobile: one column, navigation collapses into a menu button, primary action reachable without scrolling or as a sticky bar, forms one field per row (UI-23); tablet two columns where content allows (UI-24); desktop multi-column with all table columns (UI-25).
- **States.** Every screen that loads data has loading, empty, error and success states (UI-14); destructive actions ask for confirmation (UI-15); forms show a label, hint, required marker and an inline error announced to assistive technology, and keep the entered values on error (UI-13); dates, times and numbers by `business-rules.md` (UI-18); every text from `src/locales` (UI-30).

### 9.2 Navigation

Header navigation: Groups (`/`), Deleted groups (`/deleted-groups`, shown to a user who owns a group in Trash), Account (`/account`); language switch (SL, EN). Inside a group the page title shows the group name with a tab bar: Contacts, Trash, Tags, History, Members, Duplicates; Owner and Editor see all, a Viewer sees Contacts and Members (tabs the role cannot use are not shown, and the API refuses them anyway, SR-16). A contact opens at `/groups/:groupId/contacts/:contactId` with tabs Details, History, Versions (History and Versions not shown to Viewers).

### 9.3 Screen inventory per story

| Story | Screens and states (each at the three widths of UI-22) |
|---|---|
| US-001 | Sign in (e-mail, password, "Sign in with Google", "Forgot password", link to register); Register (name, e-mail, password with rule hint); Verify your e-mail (send again, sign out); Reset password confirmation; empty state "Create a group". States: busy, error from Firebase |
| US-002 | Groups list (name, role); Create group dialog; Rename group dialog; Group not found page. States: loading, empty (US-001), error |
| US-003 | Invite form (e-mail, role) and pending invitations list with expiry and cancel, on the Members tab (Owner only). States: empty list, limit reached, errors |
| US-004 | Members tab (name, e-mail, role; role select, remove and leave controls by role); confirmation dialogs; last-Owner message |
| US-005 | Contact screen with form of the 11 predefined fields, Save, Cancel; conflict screen "this contact was changed by someone else" with the new values; Viewer read-only view; trashed contact notice |
| US-006 | Tag input with suggestions and chips on the contact form; read-only chips for Viewers |
| US-007 | Manage tags (name, number of contacts, rename, delete) with confirmation |
| US-008 | Attribute rows (key with suggestions, value, remove) on the contact form; read-only for Viewers |
| US-009 | Contacts list: search box, sortable columns, tags column, paging (25), total, empty group state, no-match state with "Clear filters"; stacked cards below 768 px |
| US-010 | Filter dialog (seven conditions only) and active-condition chips with remove, "Clear filters" |
| US-011 | Tick boxes, select-all on page and select-all N results, selection count, Export button (disabled without a tick), too-many message, download |
| US-012 | Trash tab (contact, days left, restore, Delete forever for Owners); delete confirmation; Delete forever confirmation |
| US-013 | History tab of a contact (newest first, change sets, old and new value, erasure marker in the active language) |
| US-014 | Group History screen (person, event types, date range, paging, no-match state) |
| US-015 | Delete group dialog with typed name; Deleted groups list (days left, restore); restore name-clash message |
| US-016 | Account screen: delete account with confirmation, sole-Owner refusal listing the groups, re-sign-in prompt |
| US-017 | No screen of its own; error and not-found pages, rate-limit and unavailable messages |
| US-018 | Language switch in the header on every screen |
| US-019 | None (recording only) |
| US-020 | Versions tab (number, who, when), version view read-only, compare view with highlighted differences, restore confirmation, nothing-to-change message |
| US-021 | Duplicates tab: "Find duplicates", list of sets marked certain or probable with the reason, "not a duplicate", "No duplicates found" |
| US-022 | Merge screen: contacts side by side, field choices, combined tags, attribute conflicts, hint on reverting, Confirm disabled until choices are made, limit message |

Wireframes are an input of `/implement`: a screen is done when it matches its approved wireframe at the three widths, has all four states, passes the accessibility scan with no Critical or Serious violation and shows no overflow (UI baseline, "Done for a screen").

## 10. Traceability

Every id of the inputs maps to the section that implements or checks it; `02_output/scripts/check-traceability.mjs` expands these ranges and fails on any id that is missing (log: `out/logs/traceability-check.log`). Tests name the ids they prove in their display names (phase 3 and 4).

### 10.1 Stories: rules, criteria, edge cases

| R / AC / EX | Section |
|---|---|
| R-001-01..09, AC-001-01..08, EX-001-01..07 | 4.1 |
| R-002-01..08, AC-002-01..10, EX-002-01..05 | 4.2 |
| R-003-01..11, AC-003-01..10, EX-003-01..08 | 4.3 |
| R-004-01..09, AC-004-01..08, EX-004-01..05 | 4.4 |
| R-005-01..10, AC-005-01..10, EX-005-01..12 | 4.5 |
| R-006-01..10, AC-006-01..08, EX-006-01..08 | 4.6 |
| R-007-01..08, AC-007-01..08, EX-007-01..05 | 4.7 |
| R-008-01..08, AC-008-01..07, EX-008-01..06 | 4.8 |
| R-009-01..10, AC-009-01..11, EX-009-01..09 | 4.9 |
| R-010-01..11, AC-010-01..10, EX-010-01..08 | 4.10 |
| R-011-01..12, AC-011-01..13, EX-011-01..08 | 4.11 |
| R-012-01..09, AC-012-01..09, EX-012-01..08 | 4.12 |
| R-013-01..13, AC-013-01..12, EX-013-01..08 | 4.13 |
| R-014-01..08, AC-014-01..08, EX-014-01..05 | 4.14 |
| R-015-01..10, AC-015-01..08, EX-015-01..06 | 4.15 |
| R-016-01..07, AC-016-01..06, EX-016-01..05 | 4.16 |
| R-017-01..15, AC-017-01..14, EX-017-01..09 | 4.17 |
| R-018-01..08, AC-018-01..07, EX-018-01..04 | 4.18 |
| R-019-01..08, AC-019-01..08, EX-019-01..04 | 4.19 |
| R-020-01..09, AC-020-01..09, EX-020-01..06 | 4.20 |
| R-021-01..12, AC-021-01..13, EX-021-01..08 | 4.21 |
| R-022-01..14, AC-022-01..14, EX-022-01..08 | 4.22 |

### 10.2 Shared rules, security, quality

| Id | Section |
|---|---|
| BR-01 | 4.2, 4.15, 5 (`NOT_FOUND`), 6 (SR-02, SR-03) |
| BR-02 | 6 (SR-16), 4.3 to 4.8, 4.11 to 4.16, 4.19 to 4.22 (role per operation) |
| BR-03 | 4.1, 4.3, 4.17, 5 (`EMAIL_NOT_VERIFIED`) |
| BR-04 | 4.12, 4.15, ADR-08 |
| BR-05 | 3.1, 4.5, 4.22 |
| BR-06 | 4.12, 4.13, ADR-05 |
| BR-07 | 3.1, 4.9, 4.10, 4.21, ADR-04 |
| SB-01..14 | 6 (first table) |
| SR-01..20 | 6 (second table), 7 |
| NFR-01..15 | 8 |




