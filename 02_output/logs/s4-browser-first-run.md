# First run of the browser acceptance and end-to-end tests (S4, phase 3)

Command (from `02_output/frontend`, stack started with `docker compose --env-file ../.env up --build -d`):
`npx playwright test --project=chromium --timeout=8000 --reporter=list acceptance e2e`

Result on the skeleton: 104 tests, 103 failed, 1 passed, 7.6 minutes.

| Failures | First error of the test |
|---|---|
| 82 | `POST /groups gave 500: {"status":500,"code":"INTERNAL_ERROR",...}` in the set-up of the scene (the group endpoints do not exist) |
| 9 | `locator.fill` timeout on `getByLabel('E-mail')` (no sign-in or register screen) |
| 9 | `locator.fill` timeout on `getByLabel('Name')` (no register screen) |
| 1 | `page.waitForEvent` timeout on `popup` ("Sign in with Google" does not exist) |
| 1 | `locator.click` timeout on `getByRole('link', { name: 'Forgot password?' })` |
| 1 | `expect(getByRole('heading', { name: 'Sign in' })).toBeVisible()` failed |

The one test that passed was `AC-018-01 and EX-018-01` (the skeleton shell already opens in Slovenian with the SL button pressed). The test was strengthened afterwards (the first visit must show the sign-in screen: one text box and at least two buttons in the main area) and re-run alone: it fails with `getByRole('main').getByRole('textbox')`: expected 1, received 0.

The harness (emulator reset, database reset by `docker compose exec db psql`, API client through emulator tokens) ran for every test; no test failed in the harness.
