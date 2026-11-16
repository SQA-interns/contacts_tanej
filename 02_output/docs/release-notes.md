# Release notes

> For: agent (writes), human (reads) · Written in: phase 7 (`/release`) · Agent: writes

Status of the run at release: phase 6 passed **with accepted limitations** (D-70, D-71). No Critical or High finding is open; one High finding (F-V01) and 12 Spring CVEs are accepted by the developer as known limitations. Commit tested: `f510756` (verify run 5, 2026-11-16); no production code changed after it.

## Delivered

**Increment 1 (MVP), US-001 to US-019**

| Stories | Content |
|---|---|
| US-001, US-018 | Registration, sign-in (e-mail/password, Google), e-mail verification, password reset; Slovenian (default) and English interface |
| US-002, US-003, US-004, US-015 | Groups, invitations, members and roles (Owner, Editor, Viewer), group in Trash and restore |
| US-005 to US-008 | Contacts with 11 predefined fields, tags (rename, delete group-wide) and free attributes; optimistic concurrency on save |
| US-009, US-010, US-012 | List, search (accent-insensitive, Slovenian collation), filters, sort and paging; contact Trash with 30-day retention |
| US-011 | Excel export (`.xlsx`) |
| US-013, US-014, US-019 | History of changes (written in the same transaction), contact version list |
| US-016 | Account deletion (re-authentication, sole-Owner check, e-mail removed from History) |
| US-017 | Secured API: Firebase token on every request, group access by path, rate limit, uniform errors |

**Increment 2, US-020 to US-022:** compare and restore of an earlier version (US-020), duplicate search with dismissals (US-021), merge of duplicates with History across merged contacts (US-022).

Also delivered: Docker images and Compose sets (local with emulator, production with TLS proxy), backup, restore and restart scripts, CI pipeline, OpenAPI description. Install and backup guide: `../README.md` ("Deploy").

**Evidence at release (run 3 to 5):** backend 118 unit and 492 integration tests pass (JaCoCo lines 97.12 %, mutation 91.1 %); frontend 377 of 377 unit tests (lines 97.03 %); Playwright 398 of 399 in one run (one WebKit timeout that passes alone); spotless, PMD, CPD, `tsc`, prettier, eslint, licence check, traceability and gitleaks clean; NFR-01 to NFR-06 and NFR-12 pass on the development and clean-host stacks (`../logs/`); production Compose set builds and starts healthy and its security headers and refusals were checked live (run 5).

## Known limitations

- **Hidden-suite failures (D-71, F-V01, F-V04), accepted:** "Hidden-suite failures (17 of 251) remain open: R-001-06, R-018-01 to -03, R-010-10 and 12 cases of no named rule. The app opens in Slovenian by default (R-018-01); a cause was not found." The local tests of those rules pass; the hidden tests were not read. The suite passes (234 of 251) only through this acceptance.
- **Spring Framework 6.2.19 advisories (D-70, F-V13), 12 CVEs accepted, each with evidence that its precondition is absent:** "Spring Framework 6.2.19 has known CVEs whose fixes are available only in 7.0.9 or with Enterprise Support; upgrade to Spring Boot 4 / Framework 7.0.9." The CVEs: 47884, 47885, 47886, 47888, 47889, 47890, 47891, 47892, 47893, 59282, 59283, 59313 (all 2026). `log4j-api` 2.24.3 CVE-2026-34479 (6.9) is below the project's limit.
- **Spring Security 6.5.11 advisories (D-56 (c), F-V13), accepted by the developer per CVE because none is reachable:** CVE-2026-59270 (embedded UnboundID LDAP container; the app uses no LDAP), CVE-2026-41707 (DPoP proof replay; the app accepts plain Bearer Firebase ID tokens, DPoP is not active and a test shows `Authorization: DPoP ...` is refused with 401), CVE-2026-47841 (WebAuthn; the app has none). Spring Security 6.5 has no open-source fix (OSS support ended 2026-06-30; fixes are in 7.0.7 and 7.1.1, which need Spring Boot 4).
- **Next version:** move to Spring Boot 4.x / Spring Security 7.1 / Spring Framework 7.0.9.
- **Frontend mutation score is below the 90 % target (D-66):** about 77.9 %, an estimate from per-file Stryker runs, not one full run (about 440 more kills needed; line coverage 97 %). Backend mutation 91.1 % meets the target.
- **Flaky tests under load (F-V07, D-69):** Vitest and Playwright (mostly WebKit) show timeouts when run with other load; the failing tests pass alone and no product defect was found. Run Playwright with one worker. E2E-06 was intermittent once (merge followed at once by a tab switch).
- **Old account tombstones are not removed (F-08 of the INC-1 review, ADR-08):** rows of `deleted_accounts` (a user id and a time, no name or e-mail) stay; the planned daily removal after 2 days is not implemented.
- **Low findings:** the web CSP allows `style-src 'unsafe-inline'` (F-V11); the filter dialog keeps its "Choose a condition." message after a choice (D-66); 38 of the 73 input files are checked out with CRLF line endings on some clones, hashes match only after normalising (F-V12).
- **Commit rules (INC-1 review F-06, F-07):** some `feat` commits lack `Red:` lines, 24 subjects exceed 72 characters, 18 commits exceed the size rule. History cannot be rewritten.
- **Contract gap (D-58):** `VersionDetail.snapshot` in the contract lacks `tags` and `attributes`; OQ-020-01 is open.
- **No ASVS checklist file** was produced; DoD-P07 is met by the scans, tests and live checks in `06_verification-report.md`.
- **Product name:** a neutral working name is used until the customer answers OQ-P-08.

## Must be tested manually by a human

None of these could be done by the run; each needs a person, a real service or a real device.

1. **Real Firebase project (environments.md; DoD-P06, DoD-P09 partly met):** with the production configuration (`docker-compose.prod.yml`, real project, TLS) sign up with e-mail/password, verify the e-mail, sign in, sign in **with Google**, reset a password, then run the five demo journeys J1 to J5 of `interview.md` (first use, team work, tags and export, isolation between two Owners, conflict and Trash). The production set was started and its headers and refusals checked live, but nobody could sign in without a real project, so the core flows and the load tests were run only on the development and clean-host stacks (emulator).
2. **Safari and Edge (NFR-07):** open and use the main flows in current Safari and Edge, including the language switch. Chromium and Firefox pass in the tests; WebKit passes except for timing flakes.
3. **Excel (DoD-P04):** open the exported `.xlsx` in Microsoft Excel and check that it opens without a repair prompt and that accents (Š, Č, Ž) and columns look right. Tests read the file back with a library only.
4. **First-contact walkthrough:** have a person who was not in the project use the app for a task without help and note where they stop (wording, Slovenian texts, the invitation flow).
5. **Backup on the real server:** install the cron entry of the README, check that a dump appears, and do one restore test.
6. **TLS and proxy:** certificate readable by uid 101 (`chmod 644 certs/tls.key`), HTTP redirects to HTTPS, the headers in `06_verification-report.md` (run 5) are present on the real DNS name.
7. **Customer confirmations (D-14):** see "Decisions pending review" below.

## Decisions pending review

Decisions the agent recorded as non-blocking readings "pending review" (the run went on with the proposed option). Read them in `decisions-log.md` and change what you do not accept; a change after delivery means a new session.

- **Customer confirmations still outstanding (D-14, D-15):** many open questions were answered by the developer only (OQ-P-03, OQ-P-09, OQ-002-01 to OQ-022-01 and others; "needs customer": OQ-001-02, OQ-005-03, OQ-012-03, OQ-017-01, OQ-P-04 to OQ-P-08, OQ-P-10, OQ-P-11). The customer has not seen the MVP (D-49). The quality numbers and the ASVS level are developer proposals.
- **Readings of the specification:** D-19 (stale save after group-wide tag change), D-20 (e-mail format, Google name, trashed group rename, invitation matching), D-21 (export file details), D-22 (History and version readings), D-23 (glossary homonyms), D-25 (planning readings), D-28 (CSP allows Google's sign-in script).
- **Readings per story:** D-29 (US-017), D-30 (US-002), D-31 (two frozen browser tests, US-002), D-32 (US-003), D-33 (US-005), D-34 (US-006), D-35 (US-008), D-36 (US-019), D-37 (US-007), D-38 (US-009), D-39 (US-010), D-40 (US-012), D-41 (US-011), D-42 (US-015), D-43 (US-013), D-45 (US-016, account deletion).
- **Dependencies and licences:** D-24 (images and packages beyond `tech-stack.md`), D-47 (build-only npm packages with a licence outside the allowed list; the MIT licences of `fuzzy` and `valid-url` still have to be confirmed from a file).
- **Quality:** D-66 (frontend mutation shortfall accepted for this run).

Decisions with a recorded human answer (for example D-44, D-50 to D-53 changes to frozen tests, D-56, D-59, D-60, D-70, D-71) are not pending.

## Data retention duties

Source: `security-requirements.md` (Personal data), BR-04, BR-06, ADR-08. The operator of the installation (the customer) is responsible; legal questions stay with the customer outside the software.

| Data | Duty |
|---|---|
| Contacts | Erased by the nightly cleanup (`TRASH_CLEANUP_CRON`, default 03:30) 30 days after they were put in Trash, or at once by "Delete forever"; versions go with the contact. Groups in Trash likewise after 30 days, with all their data. |
| History entries | Kept while the group exists; contact values show "[izbrisano]" / "[erased]" after the contact is erased. After account deletion the actor's e-mail is removed, the name stays. |
| User accounts | In Firebase until the person deletes the account in the app (the customer may delete accounts in the Firebase console too). |
| Invitations | Until accepted, cancelled or expired (14 days); removed with the group. |
| Database backups | Dumps are kept 14 days (`RETENTION_DAYS`) and contain personal data. A contact erased in the app **stays in dumps until they expire**; if an erasure request must reach the backups, delete the dumps made since. Protect the backup folder like the database. |
| Logs | No passwords, tokens or e-mail addresses are logged; keep container logs for as long as the customer's own rules say. |
| `deleted_accounts` | Rows (user id and time) are not removed yet; see Known limitations. |

## How to deploy

See `../README.md`, section "Deploy": prerequisites (server, real Firebase project, DNS, TLS certificate), the five required values in `.env`, `chmod 644 certs/tls.key`, `docker compose -f docker-compose.prod.yml --env-file ../.env up --build -d --wait`, daily backup with 14-day retention, restore test, update.
