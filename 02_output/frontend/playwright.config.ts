import { defineConfig, devices } from '@playwright/test';

/**
 * Browser tests (NFR-07: Chromium, Firefox, WebKit). Three kinds, by folder:
 * - checks/      the skeleton and screen checks (three widths of UI-22, accessibility); screenshots are
 *                written to ../logs/screenshots. They run against the built app (vite preview).
 * - acceptance/  the browser acceptance tests of the stories (phase 3, frozen).
 * - e2e/         the end-to-end tests of e2e-scenarios.md, one per scenario (phase 3, frozen).
 * acceptance/ and e2e/ run against the real local stack (docker compose up, see acceptance/support.ts):
 * set PLAYWRIGHT_BASE_URL to the web app of the stack (http://localhost:5173). They share one database, so
 * they run one at a time.
 */
const baseURL = process.env.PLAYWRIGHT_BASE_URL ?? 'http://localhost:4173';

export default defineConfig({
  testDir: '.',
  testMatch: ['checks/**/*.spec.ts', 'acceptance/**/*.spec.ts', 'e2e/**/*.spec.ts'],
  outputDir: './test-results',
  reporter: [['list']],
  // Post-run, approved by the developer: on CI (slow shared runners) a failed test is tried once more.
  // A test that passes only on the retry is still reported as "flaky" in the summary; locally no retry.
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  fullyParallel: false,
  use: { baseURL, trace: 'retain-on-failure' },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox', use: { ...devices['Desktop Firefox'] } },
    { name: 'webkit', use: { ...devices['Desktop Safari'] } },
  ],
  webServer: process.env.PLAYWRIGHT_BASE_URL
    ? undefined
    : {
        command: 'npm run build && npm run preview',
        url: baseURL,
        reuseExistingServer: true,
        timeout: 120_000,
      },
});
