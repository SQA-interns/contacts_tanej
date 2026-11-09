# ADR-05: History, versions and erasure

- Status: accepted · Phase 2 · Drivers: AR-09, PP-03, SR-14, SR-15, BR-06, R-013-01..13, R-019-01..08, R-016-03, D-16, D-22

## Context

The History is an append-only record written with every change (US-013), the versions are snapshots (US-019), and an erasure or an account deletion anonymises parts of both (BR-06, US-016). D-22 fixes three readings: no version when the survivor is unchanged; no entry for "member joined" or "group created"; the History append capability is built first so that stories that write entries can verify their own criteria.

## Options

History as (a) application-level audit log table written by a service after the change; (b) database triggers on every table; (c) rows written by the same service method inside the change's transaction.

## Decision

Option (c), with a database guard for append-only.

- **`HistoryRecorder`** (service package) is the only writer. A service method opens one transaction, applies the change, then calls `recorder.record(...)` in the same transaction; a failed change rolls both back, so a refused or failed save writes no entry (AR-09, EX-013-08, EX-013-01).
- **Entries and change sets.** One `history_entries` row per event (`event_type` from the contract's `HistoryEventType`), with `actor_id`, `actor_name`, `actor_email`, optional `contact_id` and `contact_label`, optional `subject_name` (a member of a role change or removal, by name only, D-16) and `role`, `created_at` from the `Clock`. `history_changes` holds one row per changed field (`field_name`, `old_value`, `new_value`, kept in full, EX-013-07). All entries written by one save share a `change_set_id` and are shown together (R-013-07). A save that changes phone, a tag and an attribute writes three entries (`CONTACT_EDITED`, `TAG_CHANGED`, `ATTRIBUTE_CHANGED`) in one change set.
- **Group-level entries** have no contact: `MEMBER_INVITED` (role only, never the invited e-mail, D-16), `INVITATION_CANCELLED`, `ROLE_CHANGED`, `MEMBER_REMOVED` (a leaving is a removal by the member), `GROUP_RENAMED` (old and new name), `GROUP_DELETED`, `GROUP_RESTORED`, `EXPORTED` (count of contacts), `CONTACTS_MERGED`, `VERSION_RESTORED` has a contact. No entry for viewing, searching, creating a group or joining by invitation (R-013-08, D-22).
- **Append-only (PP-03, SR-14).** There is no endpoint that changes or deletes an entry. In the database, triggers on `history_entries` and `history_changes` refuse every UPDATE except the anonymisations: `actor_email` to NULL, `contact_label` to NULL, `erased` to true, and values to NULL. DELETE happens only through the cascade when a group is purged (R-013-11). `SkeletonIT` proves the guard.
- **Erasure of a contact (BR-06, R-013-12).** In the erasing transaction: for the contact's entries set `erased = true`, `contact_label = NULL`, and `old_value`/`new_value` to NULL; then delete the contact (versions, tags links, attributes, merge links cascade); delete tags with no contact left (EX-012-08); then append a `CONTACT_ERASED` entry with `contact_label` NULL and `erased = true`. The API returns `erased: true` and no values; the web app shows `[izbrisano]` or `[erased]` in the active language (the marker is never stored, so the language can change, AC-013-10).
- **Account deletion (R-016-03).** `actor_email = NULL` on every entry where `actor_id` is the user; the name stays. Subject names stay (members are named by name only).
- **Versions.** `contact_versions` holds a JSON snapshot of the 11 fields, tag names and attributes plus the author's id and name, one row per version number. Version 1 is recorded on create; a save with a difference records `current_version + 1`; trash and restore, tag rename or delete, a refused save and a no-op record none (R-019-02, R-019-03, EX-019-02). A version restore records a new version (R-019-04) and writes `VERSION_RESTORED` naming the restored number and the changed fields (R-020-04). A merge records one version on the survivor only if the survivor's content changed (D-22 reading 1, EX-022-08).
- **Merge link.** `contact_merges(merged_id, survivor_id)`; the survivor's History tab lists its own entries and those of its merged-away contacts, each flagged `linkedFrom` (R-022-06).
- **Tag rename or delete** writes one `TAG_RENAMED` or `TAG_DELETED` entry per affected contact, also contacts in Trash, with the old and new name, and increments those contacts' `revision` (D-19) but not their version (R-007-08, R-013-03).
- **Order of work (D-22 reading 3).** Task T-02 builds `HistoryRecorder`, the guard and the History read model before the first story that writes an entry; each such story verifies its own History criteria with it; US-013 owns the tab, the content rules and the group-wide events.

## Consequences

- Entries and the change they record cannot drift apart. A bug that forgets to call the recorder is caught by a test per write operation ("one entry per event").
- Storing erasure as a flag keeps the marker translatable and the personal data truly gone (SR-15: a database inspection test checks no first name, e-mail, phone or note of the erased contact survives outside the anonymised entries).
