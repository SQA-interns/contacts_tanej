# ADR-02: Authentication, the user record and account deletion

- Status: accepted · Phase 2 · Drivers: US-001, US-016, US-017, SR-01, SR-18, BR-03, R-003-05, R-016-06, R-016-07, D-20

## Context

Firebase Authentication signs users in; the backend only verifies ID tokens (no service account, security-requirements.md). The backend still needs: a user record (name and e-mail for the History and member list), invitation matching on sign-in, an account deletion that ends in a refused old token, and tests with no real account (SR-18).

## Options

1. Verify tokens with the Firebase Admin SDK. Needs a service-account key; also lets the backend delete the Firebase account. Rejected: an extra secret and a Google dependency beyond the stack.
2. Spring `oauth2-resource-server` with Google's published JWK set, issuer and audience validators (tech-stack.md lists the starter). The web app deletes the Firebase account itself.
3. Own JWT parsing. Rejected: security code written by hand.

## Decision

Option 2.

- **Verification.** `FirebaseJwtDecoders` checks signature (JWK set of securetoken@system.gserviceaccount.com), expiry, issuer `https://securetoken.google.com/<FIREBASE_PROJECT_ID>` and audience `<FIREBASE_PROJECT_ID>` (SR-01). The claims used are in `docs/02_contracts/id-token-claims.schema.json`.
- **Emulator.** When `FIREBASE_AUTH_EMULATOR_HOST` is set, the decoder accepts the emulator's unsigned tokens (`alg: none`) with the same issuer, audience and expiry checks. The variable must be empty in production (environments.md). Tests craft such tokens; the end-to-end tests use the real emulator.
- **Verified e-mail.** After verification, a filter reads `email_verified`. If false, every endpoint except `/health` answers 403 `EMAIL_NOT_VERIFIED` and nothing else runs, so no group is opened and no invitation matched (BR-03, R-017-03, R-003-10).
- **User record.** On each request the filter upserts `users(id, name, email)` from the token. Name: the token's `name`; if empty, the part of the e-mail before `@`; cut to 100 characters (D-20). E-mail is stored in lowercase.
- **Invitations.** `GET /me` and `GET /groups` first accept pending invitations of the user's verified e-mail (case-insensitive, not older than 14 days): one transaction creates the membership with the invited role and deletes the invitation (D-20 reading 4: matched whenever the user requests the groups list, which includes the first request after sign-in).
- **Account deletion.** `DELETE /me` needs `auth_time` within 5 minutes (else 403 `RECENT_LOGIN_REQUIRED`, R-016-07) and refuses with 409 `SOLE_OWNER` naming the groups (also groups in Trash, EX-016-01). Otherwise one transaction removes the memberships, sets `actor_email = NULL` on every History entry where the user is the actor (name stays, D-16), inserts the user id in `deleted_accounts` and deletes the `users` row. The web app then calls Firebase's `deleteUser`. A token whose `sub` is in `deleted_accounts` gets 401 (R-016-06, AC-016-04); the rows are removed after 2 days (longer than a token lives) by the cleanup (ADR-08). Pending invitations for the e-mail stay (EX-016-03).
- **Rate limit key.** The user id from the verified token (SR-09); unauthenticated requests are limited by remote address.

## Consequences

- No Google secret in the backend; the Compose setup needs only the project id and the web API key.
- If the Firebase deletion fails after the backend deletion, the user can retry; a later sign-in with the same account creates a fresh user record without memberships. Accepted and documented in the user guide.
- An emulator host left set in production would accept forged tokens. Mitigation: the startup check refuses `FIREBASE_AUTH_EMULATOR_HOST` when the profile is not `local` or `test` (task T-03).
