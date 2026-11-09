# ADR-07: Web app structure, routing, state and configuration

- Status: accepted · Phase 2 · Drivers: AR-07, AR-08, AR-13, UI-01, UI-03, UI-30, NFR-07..10, NFR-12, R-018-01..08, SR-06, SR-20

## Context

A React + TypeScript web app (Vite, Tailwind, shadcn/ui, Radix, Lucide) in Slovenian and English that talks to the API and to Firebase Authentication only (AR-07). Styles and colours must live in token files; every text in language files; the wireframe theme is active first and the final theme later with no component change (UI-03).

## Options

State: (a) React Query for server state plus component state; (b) a global store (Redux, Zustand). Routing: (c) React Router data routes with the group in the path; (d) a single-page state machine. Forms: (e) controlled components with the domain rules mirrored for fast feedback and the server's field errors shown; (f) a form library. Configuration: (g) values baked in at build time; (h) a `config.js` generated from environment variables when the container starts.

## Decision

(a), (c), (e), (h).

- **Folders** under `src/`: `app/` (providers, router, route table), `features/<area>/` (`auth`, `groups`, `members`, `contacts`, `tags`, `export`, `history`, `versions`, `duplicates`, `account`, each with its pages, hooks and components), `components/ui/` (shadcn/ui primitives, one file each), `components/layout/` (shell, language switch), `lib/` (`api/` client and generated `schema.d.ts`, `firebase.ts`, `i18n.ts`, `config.ts`, `format.ts`), `locales/{sl,en}.json`, `styles/` (`index.css`, `tokens.wireframe.css`, `tokens.final.css`), `test/`.
- **Routes.** `/sign-in`, `/register`, `/verify-email`, `/` (groups list and empty state), `/deleted-groups`, `/account`, `/groups/:groupId` (contact list), `/groups/:groupId/contacts/new`, `/groups/:groupId/contacts/:contactId` (tabs: `details`, `history`, `versions`), `/groups/:groupId/trash`, `/groups/:groupId/tags`, `/groups/:groupId/members`, `/groups/:groupId/history`, `/groups/:groupId/duplicates`, `/groups/:groupId/merge`, `*` (not found). A guard sends a signed-out user to `/sign-in` and an unverified one to `/verify-email` (R-001-05). A 404 from the API on any group route shows the same "not found" page as an unknown address and returns to the list (R-002-07, EX-004-03).
- **Server state** with TanStack Query: one query key per list, invalidated by mutations; a 401 signs the user out, a 409 `CONFLICT` shows the conflict screen with the returned current values (R-005-08).
- **Authentication.** `lib/firebase.ts` creates the Firebase `Auth` (and connects the emulator only when `firebaseAuthEmulatorUrl` is set). The API client asks `auth.currentUser.getIdToken()` per request. Firebase persistence is the SDK's own (SR-20); nothing else is kept in storage except the language choice (R-018-03).
- **Language.** `i18next` with `sl` default (fallback never to a key); the choice is saved in `localStorage` under `language`; `document.documentElement.lang` follows. Dates and numbers are formatted in `lib/format.ts` by the project facts (`d. M. yyyy HH:mm` vs `yyyy-MM-dd HH:mm`, always `Europe/Ljubljana`, R-018-04, R-018-05) using `Intl.DateTimeFormat` with `timeZone`; country names by `Intl.DisplayNames` in the active language (R-018-06).
- **Forms.** Plain controlled components; the same rules as the backend's for immediate feedback, the server's `errors[]` is authoritative and shown next to each field with `aria-invalid` and `aria-describedby`; entered values are kept on error (UI-13).
- **API types** are generated from `docs/02_contracts/openapi.yaml` (`npm run api:types`) and committed; `createApiClient` is the only caller of `fetch` for the API (AR-13).
- **Styling.** Tailwind 4 with semantic tokens in CSS variables (`@theme inline`). `data-theme="wireframe"` is set in `index.html`; `data-theme="final"` is switched on by `/design` by changing that one attribute (UI-03). No raw colour and no literal text in a component, tested by `architecture.test.ts` (AR-08).
- **Runtime configuration.** `index.html` loads `/config.js`, which the nginx container writes at start from `FIREBASE_PROJECT_ID`, `FIREBASE_WEB_API_KEY`, `FIREBASE_AUTH_DOMAIN`, `FIREBASE_AUTH_EMULATOR_URL`; `loadConfig` refuses a missing value (NFR-12). The API is reached at `/api` on the same origin (nginx proxies to the backend and strips the prefix; Vite proxies in development), so CORS is only a second line (SR-05).
- **Security headers (SR-06).** nginx sets CSP (`default-src 'self'`; `connect-src 'self'` plus the Firebase Authentication hosts; no inline script), `X-Content-Type-Options`, `frame-ancestors 'none'`, `Referrer-Policy`, `Permissions-Policy`, and HSTS behind TLS (ADR-10).

## Consequences

- No global store and no form library keeps the dependency list within `tech-stack.md`.
- The wireframe-to-final switch touches only token files and one attribute.
