import { defineConfig, devices } from '@playwright/test';

/**
 * Browser tests (NFR-07: Chromium, Firefox, WebKit). Screenshots at the three widths of UI-22 are
 * written by the tests to ../logs/screenshots. The skeleton runs against the built app (vite preview);
 * the end-to-end scenarios of e2e-scenarios.md run against the Compose stack (PLAYWRIGHT_BASE_URL).
 */
const baseURL = process.env.PLAYWRIGHT_BASE_URL ?? 'http://localhost:4173';

export default defineConfig({
  testDir: './e2e',
  outputDir: './test-results',
  reporter: [['list']],
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
