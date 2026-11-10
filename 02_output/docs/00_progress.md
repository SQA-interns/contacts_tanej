# Progress

> For: agent (writes), human (reads) · Written in: every session · Format: `AGENTS.md` ("Handover format") · Agent: writes

The handover between sessions. Every session reads it first and updates it last.

## Last session
- Session: S5 implement US-018
- Result: done (T-09 built; frozen browser tests of US-018 wait for the screens of later stories)
- Gate: not passed (DoD-S-01: `us018-language.spec.ts` needs the contact form, list, History and group dialog of US-002 and later; AC-018-01 and AC-018-03 depend only on US-001 screens and the switch, which exist)
- Notes for the next session: US-018 built: `lib/format.ts` (`formatDateTime` in `Europe/Ljubljana`, `formatDecimal`, `countryName`), `errorText(t, code)` in `lib/i18n.ts` (unknown code gives `error.INTERNAL_ERROR`, never a key); switch, i18n setup and locale key-parity test existed from the skeleton. Screens must call `formatDateTime`, `countryName` and `errorText` and never format dates themselves. Vitest 70 pass, tsc, eslint, prettier clean. Re-run `us018-language.spec.ts` after US-002, US-005 and US-013 screens exist. Previous session (US-017): Built: `GET /me`, identity filters (verified e-mail 403, deleted account 401, user upsert, D-20 name), emulator-host guard, `ApiException`/`FieldProblem` and the error mapping (404 sameness, 400, 405, 500), `RequestLogFilter`, rate limit, 1 MB limit, own `/health`, `AccessService.notFound()` (membership and role checks are still to build with US-002). Filter order: log, bearer, verified e-mail, user sync, authorization, rate limit, body size. Tests run under profile `test` (pom). Own tests in `src/test/.../it/` (IdentityIT, LimitsIT, RequestLogIT, IsolationMatrixIT, OpenApiContractIT, SecurityHeadersIT) pass; PMD, CPD, spotless clean; log `out/logs/us017-full.log` (325 other frozen tests fail: later stories). Each controller must carry `@Operation(operationId=…)` and `@ApiResponses` equal to the contract (drift test). The JaCoCo report step fails in the Maven image ("malformed input"), not investigated. D-29 lists readings. Next story: US-002 must make AC-017-01, 02, 04..07, 10, 13 and EX-017-02, 03, 06..09 pass; re-run `Us017*` and `Us001` browser tests after it.

## Next
- Command: /implement US-002
- Waiting for: nothing

## Stories
| Story | Increment | Status (ready, in progress, done) | DoD-S |
|---|---|---|---|
| US-001 | 1 (MVP) | in progress (web app done; 5 frozen browser tests wait for US-002) | DoD-S-01 not met; 02 to 07 met |
| US-017 | 1 (MVP) | in progress (T-03 to T-06 done; frozen tests wait for US-002 and later stories) | DoD-S-01 not met; 02 to 07 met |
| US-018 | 1 (MVP) | in progress (T-09 done; frozen browser tests wait for later screens) | DoD-S-01 not met; 02 to 07 met |
| US-002 to US-016, US-019 | 1 (MVP) | ready | |
| US-020 to US-022 | 2 | ready | |
