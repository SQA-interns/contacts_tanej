# ADR-01: Backend layering and the access check

- Status: accepted · Phase 2 · Drivers: AR-01, AR-02, AR-03, AR-04, AR-05, SR-02, SR-16, BR-01, BR-02, PP-01

## Context

Every operation on group data must be limited to members of that group, by role, and a non-member must learn nothing (BR-01, BR-02, SR-03). A forgotten check in one endpoint breaks PP-01. The stack is fixed: Spring Boot with Spring JDBC, no JPA (tech-stack.md).

## Options

1. Controllers call repositories and check access inline. Fewest classes; every endpoint repeats the check.
2. Layers `api` → `service` → `repository` → `domain` with one `AccessService` that every group service calls first, returning the user's role or throwing "not found" / "forbidden".
3. Method security annotations (`@PreAuthorize`) with a custom expression. Moves the check out of sight and needs the database inside the security layer.

## Decision

Option 2. Packages under `app.contactgroups`: `api` (controllers, request and response records, `ApiExceptionHandler`), `service`, `repository` (JdbcTemplate), `domain` (records, enums, value rules, `ErrorCode`), `config`. `api` never calls `repository`; `repository` never calls `api` or `service`; `domain` depends on nothing of the application (ArchUnit tests in `ArchitectureTest`).

- The group id is a path variable only (AR-04). A controller method takes `@PathVariable UUID groupId`; a body or query never carries one. A malformed id is mapped to the same 404 as an unknown group (EX-017-02).
- `AccessService.require(groupId, user, minimum role)` runs first in every service method that touches a group. It reads membership and role from the database on every request (never from the token) and throws `NOT_FOUND` for a non-member, a missing group and a group in Trash for a non-Owner (D-17), and `FORBIDDEN` when the role is too low. The one 404 body is built in one place so that all cases are identical (SR-03).
- Every repository method that reads or writes group data takes `groupId` and filters by it in the SQL (AR-03). Contact, tag, history and version queries join through `groups`/`contacts.group_id`; an id of another group yields no row, so the service answers 404 (EX-012-06, EX-017-08).
- One service class per area: `GroupService`, `MemberService`, `InvitationService`, `ContactService`, `TagService`, `SearchService`, `ExportService`, `HistoryService`, `VersionService`, `DuplicateService`, `MergeService`, `AccountService`, `TrashCleanupService`.
- Transactions are opened in the service (`TransactionTemplate`/`@Transactional`), never in a controller or repository, so a change and its History entry commit together (AR-09, ADR-05).
- No server session (AR-05): `SessionCreationPolicy.STATELESS`; an ArchUnit rule forbids `HttpSession`.

## Consequences

- One place to review the access rule; a test per OpenAPI operation with a non-member (AC-017-07) is generated from the contract and proves it.
- Slightly more classes than option 1. Accepted: ES-11 asks for one responsibility per module.
- Rate limiting and body-size limiting are servlet filters in `config`, before the controllers (ADR-06).
