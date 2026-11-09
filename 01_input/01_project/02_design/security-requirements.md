# Security requirements

> For: agent (reads), human (reviews) · Owner: Security officer · Agent: read-only

Adds to `general/security/security-baseline.md` (SB-01 … SB-14); do not repeat those here.

## Verification standard

| Component | Standard | Level | Reason if above the default |
|---|---|---|---|
| backend | OWASP ASVS 5.0 | Level 2 | The product stores personal data of many people and separates tenants (groups). Chosen by the developer (Q-12d); the customer named no standard. |
| frontend | OWASP ASVS 5.0 | Level 2 | Same reason; the web app holds the Firebase session and shows personal data. |

The customer must confirm the level (listed in the review checklist).

## Authentication and authorization

- Users: Owner, Editor and Viewer of a group (`business-rules.md`, BR-02). A user has an account at Firebase; sign-in with e-mail and password or Google; no multi-factor authentication in the MVP (Q-20).
- Authentication: Firebase. The web app keeps the Firebase session; the backend verifies the ID token on every request (signature, issuer, audience = the Firebase project, expiry) and requires a verified e-mail. No service-account key is needed to verify tokens.
- Authorisation: own (custom). Membership and role are read from the database on every request, never taken from the token or the request body; the group id comes only from the path (Q-12). A role change or removal applies at the next request.
- No system administrator inside the product: nobody sees or manages a group they are not a member of (Q-06a).

## Personal data

| Data item | Purpose | Retention | Shown/exported to |
|---|---|---|---|
| Contact: 11 predefined fields (names, e-mail, phone, company, job title, address, country, notes), tags, attributes | The customer's contact list | Until the contact is erased: 30 days in Trash, or "Delete forever" (BR-04, BR-06); deleted with the group | Members of the group by role (Viewers see contact data); Owners and Editors by export |
| Versions of a contact | Compare and restore | As long as the contact exists; erased with it | Owners and Editors |
| History entries (actor name and e-mail, old and new values of contact data) | Audit trail | As long as the group exists; contact values replaced by "[izbrisano]" or "[erased]" (active UI language) after erasure; actor e-mail removed after account deletion, the name stays (Q-20b) | Owners and Editors |
| User name and e-mail | Identify who made a change; invitations | Kept by Firebase until the account is deleted; name stays in History after deletion | Shown in the History to Owners and Editors, and in the member list to all members |
| Invitation e-mail address | Match an invited person | Until accepted, cancelled or expired (14 days); removed with the group | Owners |
| Logs | Operation and security | The logs hold no contact data, tokens or passwords (SR-11) | The operator |
| Backups of the database | Recovery | 14 days (NFR-06); hold data erased in the meantime until they expire | The operator |

Legal matters (controller or processor role, privacy notice, consent, hosting region) are handled by the customer outside the software (Q-33).

## Project requirements

| ID | Requirement |
|---|---|
| SR-01 | The backend shall verify the Firebase ID token on every request except the health check: signature, issuer, audience (the Firebase project ID from the environment), expiry. Check: tests with a missing, expired, tampered and foreign-project token (AC-017-01, AC-017-02). |
| SR-02 | Every database read and write of group data shall be limited by the group id and the user's membership in the same operation; a single repository-level test per operation shows that another group's data cannot be reached. Check: AC-017-07 (one test per API operation), E2E-04. |
| SR-03 | A non-member, a nonexistent group, a malformed group id and a group in Trash (for non-Owners) shall give the same 404 response with the same body and no measurable difference in content. Check: AC-017-04, EX-017-02, EX-017-06. |
| SR-04 | The API shall accept HTTPS only; plain HTTP shall be refused or redirected to HTTPS by the application or its documented front proxy, and HSTS shall be set. Check: test against the Compose setup with the front proxy; the install guide says how to enable TLS. |
| SR-05 | The API shall allow only the web app's own address (environment setting) as CORS origin. Check: a request with another Origin gets no CORS permission headers. |
| SR-06 | The web app and API shall set the standard security headers (Content-Security-Policy without inline scripts, X-Content-Type-Options, frame-ancestors/X-Frame-Options, Referrer-Policy, Permissions-Policy). Check: header test on both. |
| SR-07 | Every input shall be validated on the server by the rules of the stories; all SQL shall use parameterised queries (Spring JDBC); search, filter and sort inputs shall never be inserted into SQL as text, and sort columns shall come from a fixed list. Check: tests with injection strings (EX-009-03, EX-010-06), static analysis for string-built SQL. |
| SR-08 | The Excel export shall write cells starting with `=`, `+`, `-` or `@` as plain text. Check: AC-011-08. |
| SR-09 | The API shall limit each user to a configurable number of requests per minute (default 100; answered with 429 and `Retry-After` above it) and each request body to 1 MB (answered with 413). Check: AC-017-08, AC-017-09. |
| SR-10 | Errors shall use one format with a code and a short message and shall show no stack trace, class name or SQL. Check: AC-017-10; a test provoking an internal error. |
| SR-11 | The server shall log failed token checks, 401, 403 and 404 responses and rate-limit hits, and shall never log contact data, tokens, passwords or full request bodies. Check: a test with known values in the request shows none of them in the log output. |
| SR-12 | Secrets (database password, Firebase settings) shall live only in environment variables or the `.env` file; none shall be in the repository, images, logs or the chat. Check: secret scan in phase 6. |
| SR-13 | Data at rest shall be encrypted by the hosting (database and backups); the install guide shall say so. The product does not encrypt fields itself. Check: the install guide lists the requirement (documentation check). |
| SR-14 | No API endpoint or screen shall change or delete a History entry; the only change shall be the anonymisation after an erasure (BR-06) or an account deletion (US-016). Check: AC-013-09; a test over every operation of the OpenAPI description. |
| SR-15 | After a permanent erasure (BR-06) and after the purge of a group, no personal data of the erased contact or group shall remain in the database tables outside the anonymised History. Check: a database inspection test after AC-012-03 and AC-015-05. |
| SR-16 | The roles shall be enforced on the server for every operation of BR-02; hiding a control in the UI is not protection. Check: one test per row of the permission matrix with each role (AC-005-08, AC-011-10, AC-012-04, AC-013-08). |
| SR-17 | The product shall send no e-mail and no message to a person on its own; only Firebase sends e-mails (verification, password reset). Check: the code holds no mail-sending dependency (dependency list review); AC-003-01. |
| SR-18 | Automated tests and CI shall use the Firebase Authentication Emulator with fixed test users; they shall use no real account and no real secret. Check: the test configuration points to the emulator; secret scan. |
| SR-19 | The dependency scan shall find no Critical or High vulnerability without a recorded decision. Check: the dependency scan in phase 6. |
| SR-20 | The web app shall not put tokens or contact data in URLs, in the browser's local storage beyond the Firebase SDK's own session store, or in analytics. The web app shall use no third-party tracker or analytics. Check: code search and a network trace in the main flows. |

## Overrides

None.
