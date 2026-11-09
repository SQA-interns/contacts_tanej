# ADR-08: Scheduled cleanup and the clock

- Status: accepted · Phase 2 · Drivers: AR-10, AR-11, BR-04, R-012-06, R-015-06, R-003-06, EX-012-02, EX-015-02, EX-003-03, EX-003-04, SR-15

## Context

Contacts and groups in Trash are erased after 30 days, at the latest at the next cleanup, which runs at least once a day (BR-04). Invitations expire after 14 days. Tests must be able to move the clock without waiting. The product's time zone matters only for display and for day boundaries (project-wide facts).

## Options

(a) Spring `@Scheduled(cron = ...)` in the backend; (b) an operating-system cron calling an endpoint; (c) `pg_cron`. Time: (d) `Instant.now()` where needed; (e) one injectable `Clock`.

## Decision

(a) and (e).

- **`Clock` bean** (`ClockConfig`, `Clock.systemUTC()`); no other class calls `now()`; `ArchitectureTest` enforces it (AR-11). Tests replace the bean with a mutable clock.
- **`TrashCleanupService.run()`** is a public method; `@Scheduled(cron = "${app.trash-cleanup-cron}")` calls it (default `0 30 3 * * *`, once a day, set by `TRASH_CLEANUP_CRON`); a test calls it directly with the clock set (AR-10, AC-012-06, AC-015-05). It runs in this order inside separate transactions per group, so one failure does not stop the rest:
  1. Groups in Trash for 30 days or more: delete the group; rows cascade (contacts, tags, versions, History, memberships, invitations, merges, dismissals) (R-015-06, SR-15).
  2. Contacts in Trash for 30 days or more (in groups that remain): the erasure of ADR-05 (History anonymised, BR-06).
  3. Invitations older than 14 days: delete (R-003-06; an expired invitation is also treated as not pending at read time, so the cleanup is hygiene, not the rule).
  4. `deleted_accounts` older than 2 days: delete (ADR-02).
- "30 days" means `trashed_at <= now - 30 days`, compared in UTC; a contact in Trash for 29 days 23 hours is restorable, exactly 30 days is erased by the next run (EX-012-01, EX-012-02). Restore stays possible after day 30 until the cleanup has run (BR-04: "until then" is the 30 days, F-21; a restore after day 30 but before the run is allowed and recorded as an Allow, not a rule to test).
- An invitation counts as pending while `created_at > now - 14 days`: 13 days 23 hours matches, exactly 14 days does not (EX-003-03, EX-003-04). The same predicate serves the 20-pending limit (R-003-04, EX-003-02).
- Several instances: not a requirement (one backend container). The cleanup takes `pg_try_advisory_lock` so two instances would not run it twice.
- **Day boundaries.** `ZoneId.of("Europe/Ljubljana")` is a constant in `domain` used for the export file name date, and for the History date filter (`from` 00:00 local to `to` 23:59:59.999 local, converted to UTC, R-014-04, EX-014-01). Daylight saving is handled by `java.time`.

## Consequences

- A cleanup test needs no sleeping. The 14-day backup window (NFR-06) still holds erased data; the install guide says so (OQ-P-11).
- If the backend is down at 03:30 the cleanup runs at the next scheduled time; the rule says "at least once a day", which the operator's availability target (NFR-05) supports.
