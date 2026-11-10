# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 implement US-002
- Result: done (T-07, T-10, T-11, T-12 built)
- Gate: not passed (DoD-S-01: frozen AC-002-01 needs search (US-009), AC-002-07 and EX-002-05 need Trash (US-015), AC-002-10 and EX-002-05 need invitations (US-003); browser AC-002-03 fails by a race in the frozen `signIn` helper, D-31)
- Notes for the next session: US-002 built: T-07 History foundation (`HistoryRecorder.record(ChangeSet)`, MANDATORY transaction, `HistoryRepository.findByGroup`), `GroupService`/`GroupController` (`/groups`, `/groups/{id}`, PATCH rename), `AccessService.requireMember/requireOwner/requireOwnerLocked` (group row lock + advisory locks of all Owners for the name rule; restore in US-015 must reuse `GroupRepository.ownerHasName`), web: `GroupsHome` (list, create, rename), `GroupPage` (name, role, Contacts tab, "No contacts yet" placeholder for US-009), `GroupNameDialog`. Backend: 11 of 15 frozen US-002 tests pass; full run `out/logs/us002-full.log`: 305 of 443 fail, all later stories (before: Us017 14 still wait on US-003/005/015). Vitest 95 pass, tsc, eslint, prettier clean; spotless, PMD, CPD clean. Browser: AC-002-01/04/05 pass; AC-002-03 and US-001 AC-001-04 (Google popup) fail, see D-31. D-30 lists readings. Next: US-003 (T-13..T-15); `join()` in the tests needs invitations. Previous session (US-018): US-018 built: `lib/format.ts` (`formatDateTime` in `Europe/Ljubljana`, `formatDecimal`, `countryName`), `errorText(t, code)` in `lib/i18n.ts` (unknown code gives `error.INTERNAL_ERROR`, never a key); switch, i18n setup and locale key-parity test existed from the skeleton. Screens must call `formatDateTime`, `countryName` and `errorText` and never format dates themselves. Vitest 70 pass, tsc, eslint, prettier clean. Re-run `us018-language.spec.ts` after US-002, US-005 and US-013 screens exist. Previous session (US-017): Built: `GET /me`, identity filters (verified e-mail 403, deleted account 401, user upsert, D-20 name), emulator-host guard, `ApiException`/`FieldProblem` and the error mapping (404 sameness, 400, 405, 500), `RequestLogFilter`, rate limit, 1 MB limit, own `/health`, `AccessService.notFound()` (membership and role checks are still to build with US-002). Filter order: log, bearer, verified e-mail, user sync, authorization, rate limit, body size. Tests run under profile `test` (pom). Own tests in `src/test/.../it/` (IdentityIT, LimitsIT, RequestLogIT, IsolationMatrixIT, OpenApiContractIT, SecurityHeadersIT) pass; PMD, CPD, spotless clean; log `out/logs/us017-full.log` (325 other frozen tests fail: later stories). Each controller must carry `@Operation(operationId=…)` and `@ApiResponses` equal to the contract (drift test). The JaCoCo report step fails in the Maven image ("malformed input"), not investigated. D-29 lists readings. Next story: US-002 must make AC-017-01, 02, 04..07, 10, 13 and EX-017-02, 03, 06..09 pass; re-run `Us017*` and `Us001` browser tests after it.

## Next
- Command: /implement US-003
- Waiting for: nothing

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 | 1 (MVP) | in progress (web app done; 5 frozen browser tests wait for US-002) | DoD-S-01 not met; 02 to 07 met |
| US-017 | 1 (MVP) | in progress (T-03 to T-06 done; frozen tests wait for US-002 and later stories) | DoD-S-01 not met; 02 to 07 met |
| US-018 | 1 (MVP) | in progress (T-09 done; frozen browser tests wait for later screens) | DoD-S-01 not met; 02 to 07 met |
| US-002 | 1 (MVP) | in progress (T-07, T-10 to T-12 done; 4 frozen API tests wait for US-003, US-009, US-015; browser AC-002-03 D-31) | DoD-S-01 not met; 02 to 07 met |
| US-003 to US-016, US-019 | 1 (MVP) | ready | |
| US-020 to US-022 | 2 | ready | |
