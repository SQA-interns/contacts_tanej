# Business rules

> For: agent (reads), human (reviews) · Owner: Product owner · Agent: read-only

Project-wide facts, rules shared by more than one story, the data model and the glossary. A rule that belongs to one story lives in that story. Format: `general/requirements-standard.md`.

## Project-wide facts

Facts that hold for the whole project, written once. Stories, rules and tests use these values and never repeat or override them.

| Fact | Value | Source |
|---|---|---|
| Time zone | All times are stored in UTC and shown in `Europe/Ljubljana` (CET/CEST, with summer time) for every user. [ASSUMPTION] The customer's country was not stated; the customer text is Slovenian. Dates (day boundaries, file name date, History date filter) use this zone. | Q-26, Q-11b |
| Currency | None. The product handles no money. | Q-11a |
| Tax (included in prices or not, rate) | None. The product handles no prices or tax. | Q-11a |
| Date and time format | Slovenian UI: `d. M. yyyy` and `HH:mm` (24 hours). English UI: `yyyy-MM-dd` and `HH:mm`. | Q-11a |
| Number format | Slovenian UI: decimal comma. English UI: decimal point. [ASSUMPTION] English decimal point (not stated; the product shows few numbers, e.g. counts). | Q-11a |
| Languages of the product | Slovenian (default) and English, switch in the header; API field names in English; formal "vi" in Slovenian texts. Excel headers and country names follow the active UI language. | Q-11, Q-34a |
| Text encoding | UTF-8 everywhere, including the API and the Excel file. | default, Q-11a |
| Units of measure | None. | — |
| Other: sorting and matching | Sorting follows Slovenian rules (č, š, ž after c, s, z). Search ignores case and accents. | Q-11a, Q-23 |
| Other: country list | The ISO 3166-1 list of countries, names in the active language. [ASSUMPTION] The interview asks for "a standard country list" and does not name one (Q-08b). | Q-08b |
| Other: retention in Trash | 30 days for a contact and for a group (BR-04). | Q-06c, Q-06f |
| Other: page size | 25 rows per page in lists. | Q-23b |

## Shared rules

| ID | Rule (EARS) | Stories | Source |
|---|---|---|---|
| BR-01 | The system shall return to a user data of a group only when the user is a member of the group; for a group where the user is not a member, or which does not exist, the system shall answer "not found" and shall reveal nothing about it. A group in Trash counts as not existing for members who are not Owners of it. | US-002, US-003, US-004, US-005, US-009, US-010, US-011, US-012, US-015, US-016, US-017, US-021 | Q-13, Q-12, Q-32a |
| BR-02 | The system shall allow an operation only to the roles marked in the permission matrix below; if a member's role is not marked, then the system shall refuse it (403); the role is checked at every request. | US-003 to US-007, US-008, US-011 to US-015, US-017, US-019 to US-022 | Q-06, Q-06b, Q-06d |
| BR-03 | While a user's e-mail is not verified, the system shall give the user access to no group and no contact, and shall match no invitation. | US-001, US-002, US-003, US-017 | Q-20 |
| BR-04 | The system shall keep a contact or a group in Trash for 30 days from the moment it was moved there and shall restore it on request until then; once the 30 days are over the system shall delete it permanently, with its personal data, at the next run of its cleanup, which runs at least once a day. [ASSUMPTION] "at least daily". | US-012, US-015, US-022 | Q-06c, Q-06f |
| BR-05 | If a contact breaks a constraint of the data model for a contact (formats, limits, required one-of-four, country list), then the system shall refuse the whole save and store nothing. | US-003, US-005, US-022 | Q-08a, Q-08b |
| BR-06 | When a contact is erased permanently (after 30 days in Trash or by "Delete forever"), the system shall delete the contact with its tags, attributes and versions, and shall keep its History entries with the contact's personal data replaced by the erasure marker. | US-012, US-013, US-019, US-022 | Q-06c, Q-06e, Q-29b |
| BR-07 | The system shall compare texts in two ways: *ignoring case* (tag names, attribute keys, e-mail addresses of invitations and duplicates) and *ignoring case and accents* (search, attribute values and company in filters, names and company in duplicate detection). | US-003, US-005, US-006, US-007, US-008, US-009, US-010, US-021, US-022 | Q-10, Q-23, Q-27 |

### Permission matrix (BR-02)

O = Owner, E = Editor, V = Viewer. A member's role is per group. "—" = not allowed.

| Operation | O | E | V |
|---|---|---|---|
| Create a group (any signed-in user with a verified e-mail) | yes | yes | yes |
| View, search, filter and sort contacts; open a contact; see its tags and attributes | yes | yes | yes |
| Create, edit, tag and give attributes to a contact | yes | yes | — |
| Rename or delete a tag of the group | yes | yes | — |
| Delete a contact to Trash; see Trash; restore from Trash | yes | yes | — |
| "Delete forever" | yes | — | — |
| Export selected contacts | yes | yes | — |
| See a contact's History and the group-wide History | yes | yes | — |
| See, compare and restore versions | yes | yes | — |
| Find duplicates; mark "not a duplicate"; merge | yes | yes | — |
| See the member list; invite; cancel an invitation; change a role; remove a member | yes | — | — |
| Leave the group (not the last Owner) | yes | yes | yes |
| Delete the group; see "Deleted groups"; restore a group | yes | — | — |

## Data model

Types are logical; the plan decides the storage. "Personal data" marks data about people that falls under erasure and access rules.

| Entity | Field | Type and format | Required | Constraints | Personal data |
|---|---|---|---|---|---|
| User | id | Firebase user id (text) | yes | unique | no |
| User | name | text | yes | 1 to 100 characters | yes |
| User | email | text | yes | valid e-mail, lowercase; removed from History entries on account deletion | yes |
| Group | id | generated id | yes | unique | no |
| Group | name | text | yes | 1 to 100 characters after trimming [ASSUMPTION]; not unique | no |
| Group | state | active or in Trash | yes | see lifecycle | no |
| Group | trashed_at | UTC timestamp | when in Trash | — | no |
| Group | created_at, created_by | UTC timestamp, User id | yes | — | no |
| Membership | group, user, role | ids, Owner / Editor / Viewer | yes | one membership per user and group; at least one Owner per group | no |
| Invitation | group, email, role | id, text, role | yes | e-mail valid format, compared ignoring case; at most 20 pending per group | yes (e-mail) |
| Invitation | created_at, status | UTC timestamp, pending / accepted / cancelled | yes | expires 14 days after created_at | no |
| Contact | id, group | ids | yes | the group never changes | no |
| Contact | first_name, last_name | text | no | at most 255 characters | yes |
| Contact | email | text | no | valid e-mail format when present, stored in lowercase, one address, at most 255 characters; not unique in the group | yes |
| Contact | phone | text | no | stored as typed; only digits, spaces and `+ ( ) - . /`, at most 30 characters [ASSUMPTION]; one phone | yes |
| Contact | company, job_title | text | no | at most 255 characters | yes |
| Contact | street, postal_code, city | text | no | at most 255 characters each | yes |
| Contact | country | ISO 3166-1 country code | no | chosen from the country list | yes |
| Contact | notes | free text | no | at most 5,000 characters; may hold anything | yes |
| Contact | (save rule) | — | — | at least one of first_name, last_name, email, company is not empty after trimming; trimming of spaces is applied to every text field | — |
| Contact | state, trashed_at | active or in Trash, UTC timestamp | yes | see lifecycle | no |
| Contact | created_at, current version number | UTC timestamp, whole number from 1 | yes | the number grows by 1 per version, never lowered | no |
| Tag | group, name | id, text | yes | 1 to 50 characters; letters (Unicode), digits, "-", "_"; unique per group ignoring case; keeps the spelling first typed | no |
| Contact tag | contact, tag | ids | — | at most 50 tags per contact | no |
| Attribute | contact, key, value | id, text, text | key yes | key 1 to 50 characters, unique per contact ignoring case; value at most 255 characters, may be empty; at most 50 per contact | yes (value) |
| Version | contact, number, snapshot, user, created_at | id, whole number, all predefined fields + tags + attributes, user, UTC timestamp | yes | number unique per contact; erased with the contact | yes |
| History entry | id, group, contact (optional), event type, user name, user e-mail, created_at | ids, text, UTC timestamp | yes | append-only; group-level entries have no contact | yes |
| History entry | field changes: field name, old value, new value | text | per changed field | old and new value kept in full; replaced by the erasure marker when the contact is erased | yes |
| Duplicate dismissal | group, set of contact ids, user, created_at | ids, UTC timestamp | yes | identified by exactly the contacts in the set | no |

### Lifecycle

| Entity | States and allowed transitions |
|---|---|
| Group | active → in Trash (Owner, typed name) → active (Owner restores within 30 days) or gone (30 days over). |
| Contact | active → in Trash (Owner or Editor deletes) → active (Owner or Editor restores within 30 days) or gone (30 days over, or Owner "Delete forever"). A contact in Trash cannot be edited. |
| Invitation | pending → accepted (the invited person signs in with the verified e-mail) or cancelled (Owner) or expired (14 days, treated as not pending). |
| Membership | created on acceptance or group creation → removed (Owner removes, member leaves, account deleted). The last Owner's membership cannot be removed. |
| History entry | written once; never changed; only anonymised (BR-06, US-016); deleted with the group. |

## Glossary

The ubiquitous language of the project (`general/requirements-standard.md`): every requirement, the code, the API, the tests and the UI use these terms.

| Term | Meaning | Name in code | Do not use |
|---|---|---|---|
| user | A person with an account (Firebase) who has signed in. | `User` | account holder, customer, client |
| group | The unit of isolation and sharing: a named set of contacts with its members. | `Group` | project, workspace, team, tenant, organisation, folder |
| member | A user who belongs to a group, with exactly one role in it. | `Membership` | participant, collaborator |
| Owner | Role: may do everything in the group, including managing members and deleting the group. A group has one or more. | `OWNER` | admin, administrator, manager |
| Editor | Role: may change contacts, export, see History and versions. | `EDITOR` | contributor, writer |
| Viewer | Role: may only view, search and filter contacts. | `VIEWER` | reader, read-only user, guest |
| invitation | An offer of membership with a role, addressed to an e-mail address; there is no Accept step. | `Invitation` | invite link, request |
| contact | A person or company entry in a group, with predefined fields, tags and attributes. | `Contact` | record, lead, customer, entry, person |
| predefined field | One of the 11 fixed fields of a contact. | `ContactField` | standard field, column |
| tag | A free label on contacts of a group, used as a category; no meaning for the system. | `Tag` | category, label, keyword, flag |
| attribute | A free pair of a key and a value on one contact. | `Attribute` | custom field, key-value field, property, metadata |
| History | The append-only list of events (who, when, what, old and new value). | `HistoryEntry` | audit log, audit trail, log, changelog |
| version | A full snapshot of a contact with a running number. | `ContactVersion` | revision, snapshot, backup |
| Trash | The state of a deleted contact or group during its 30 days, restorable. | `TRASHED` (state), `trashedAt` | recycle bin, archive, soft delete |
| Delete forever | The Owner's immediate permanent erasure of a contact in Trash. | `eraseContact` | purge (by hand), hard delete |
| erasure | Permanent deletion of a contact's data, leaving the anonymised History. | `erase` | removal, wipe |
| erasure marker | The text "[erased]" in History values of an erased contact. | `ERASED_MARKER` | placeholder, redacted |
| selection | The contacts ticked in the list for export. | `Selection` | basket, cart |
| export | The Excel (.xlsx) file of the ticked contacts. | `Export` | download, report |
| duplicate set | Contacts reported as probably the same person, marked certain or probable. | `DuplicateSet` | duplicate group, cluster |
| certain duplicate | Contacts with the same e-mail address ignoring case. | `CERTAIN` | exact duplicate |
| probable duplicate | Contacts with the same name and the same company or phone. | `PROBABLE` | fuzzy duplicate, possible duplicate |
| merge | Joining two or more duplicates into the survivor by the user's choice. | `merge` | combine, consolidate |
| survivor | The oldest contact of a merge, which stays. | `survivor` | master, target, winner |
| conflict | A save refused because the contact was changed by someone else meanwhile. | `CONFLICT` | collision, lock error |
