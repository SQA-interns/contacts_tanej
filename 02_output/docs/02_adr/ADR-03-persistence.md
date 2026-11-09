# ADR-03: Persistence, schema, concurrency and the conflict check

- Status: accepted · Phase 2 · Drivers: AR-02, AR-06, AR-14, NFR-13, R-004-07, R-002-03, R-005-08, D-19, D-22

## Context

PostgreSQL 17.6 with Spring JDBC and Flyway, no JPA (tech-stack.md). Several rules need serialisation: the last Owner (R-004-07, "also when two such requests arrive at the same time"), the group name per Owner (R-002-03), the contact version (R-005-08) and double trash requests (EX-012-03). A group-wide tag rename changes contacts without a version, yet a stale save must still be refused (D-19).

## Options

For concurrency: (a) optimistic only, retry on failure; (b) pessimistic row locks in the transaction; (c) serializable isolation everywhere. For the conflict check: (d) the version number only (leaves D-19 open); (e) a separate change counter; (f) a timestamp.

## Decision

- **Schema** is `V1__baseline.sql` (Flyway, forward only, run at start-up, AR-06): `users`, `deleted_accounts`, `groups`, `memberships`, `invitations`, `contacts`, `tags`, `contact_tags`, `attributes`, `contact_versions`, `history_entries`, `history_changes`, `duplicate_dismissals`, `contact_merges`. Table names are the glossary terms in the plural. Constraints that the data model states are also CHECK constraints, so a bug in the service cannot store an invalid contact (defence in depth for BR-05).
- **Empty text is NULL.** The service trims every text field and stores an empty one as NULL; the API treats absent and empty alike.
- **Locks (b).** Within the service transaction: `SELECT … FROM groups WHERE id = ? FOR UPDATE` before any change of membership (last-Owner rule, group trash and restore, rename); `pg_advisory_xact_lock(hashtext(user_id))` for each Owner of the group, in sorted order, before a create, rename or restore that applies the name rule (R-002-03); `SELECT … FROM contacts WHERE id = ? AND group_id = ? FOR UPDATE` before a save, trash, restore, version restore, merge (the other request waits, then sees the new state and fails with the proper code, EX-012-03).
- **Conflict check (e).** `contacts.revision` starts at 1 and grows by 1 with every change of content: a save, a version restore, a merge into the survivor, and a group-wide tag rename or delete (which records no version, R-007-06, R-019-03). The API field `revision` is what a save, a version restore and a merge carry (AR-14: "carries the contact's current version number" is satisfied by a counter that never falls behind it). A save with a different revision is refused with 409 `CONFLICT` and the current contact (R-005-08, R-017-15). `contacts.current_version` is the separate version number of US-019. Trashing and restoring a contact do not change `revision` (the state is checked separately, R-005-10).
- **No-op saves.** The revision is checked first and always: a different revision is a conflict (AR-14), whatever the values. With the right revision, the service compares the new values with the stored ones; with no difference nothing is written, no version, no History entry, and the contact is returned unchanged (EX-005-09, EX-013-01, R-019-02).
- **Row mapping.** `JdbcClient`/`NamedParameterJdbcTemplate` with records and explicit `RowMapper`s. Dynamic SQL (search, filter, sort, History filters) is assembled from fixed fragments only; user input is always a bind parameter (SR-07, ADR-04).
- **Time** is stored as `timestamptz` in UTC; the `Clock` bean supplies "now" (AR-11, ADR-08).

## Consequences

- Correct under concurrency with simple code; the price is brief row locks, acceptable for 20 concurrent users (NFR-04).
- Tests that need two parallel requests use a `CountDownLatch` against the Testcontainers database (AC-004-06, EX-012-03, EX-015-05).
- `revision` and `current_version` are two numbers; the API and tests name them apart (`revision`, `versionNumber`), and the UI shows only the version number.
