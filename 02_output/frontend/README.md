# Web app

React 19, TypeScript, Vite, Tailwind CSS 4, shadcn/ui (Radix, Lucide), TanStack Query, react-router, i18next (Slovenian and English), Firebase Authentication. Design: `../docs/02_specification.md` (sections 1.2 and 9), `../docs/02_adr/ADR-07-frontend-structure.md`.

## Prerequisites

Node 24.13.0 and npm 11.6.2 (decision D-01). Install with `npm ci` (versions are exact; the lock file is committed; add a package with `npm install --save-exact`). Browser tests need the browsers: `npx playwright install chromium firefox webkit`.

## Commands (ES-05)

| Task                                                                            | Command                      |
| ------------------------------------------------------------------------------- | ---------------------------- |
| build                                                                           | `npm run build`              |
| test (unit and component, with coverage)                                        | `npm test`                   |
| check (format, lint, type check, duplication)                                   | `npm run check`              |
| run (development, API proxied from `/api` to `localhost:8080`)                  | `npm run dev`                |
| browser tests (Chromium, Firefox, WebKit, screenshots in `../logs/screenshots`) | `npm run test:e2e`           |
| format                                                                          | `npm run format`             |
| API types from the contract                                                     | `npm run api:types`          |
| validate every contract                                                         | `npm run contracts:validate` |
| dependency scan (allow-list: D-08)                                              | `npm run scan`               |
| mutation (phase 5)                                                              | `npm run mutation`           |

## Configuration

Runtime values are in `/config.js` (`public/config.js` for development against the emulator; written from environment variables when the container starts): `FIREBASE_PROJECT_ID`, `FIREBASE_WEB_API_KEY`, `FIREBASE_AUTH_DOMAIN`, `FIREBASE_AUTH_EMULATOR_URL` (empty in production). Nothing is baked into the build.

## Deploy

The image is built from `Dockerfile` (the Vite build served by nginx, non-root) by `../docker-compose.yml` and `../docker-compose.prod.yml`; `/config.js` is written from the environment at container start, so one image serves every environment. In the delivered set the `proxy` service terminates TLS in front of it. Steps for the customer's server: `../README.md` ("Deploy").

## Rules the tests enforce

No raw colour and no literal text in a component, colours only in `src/styles/tokens.*.css`, the same keys in `sl.json` and `en.json`, Firebase used for Authentication only (`src/architecture.test.ts`, `src/locales/locales.test.ts`). The wireframe theme is active (`data-theme="wireframe"` in `index.html`); the final theme is switched on by `/design`.

## Troubleshooting

- `npm run test:e2e` cannot start a browser: run the Playwright install command above.
- Types out of date: run `npm run api:types` after any change of `../docs/02_contracts/openapi.yaml`; CI fails on a difference.
- Two `playwright-core` versions: `package.json` forces 1.55.1 with `overrides` (decision D-24).
