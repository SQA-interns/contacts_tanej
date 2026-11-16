import playwright from "../../frontend/node_modules/@playwright/test/index.js";

const { defineConfig, devices } = playwright;

/** Hidden browser cases: run against the local stack like the visible acceptance tests (PLAYWRIGHT_BASE_URL). */
export default defineConfig({
  testDir: ".",
  testMatch: ["*.spec.ts"],
  outputDir: "./results",
  reporter: [["list"], ["junit", { outputFile: "./results/junit-ui.xml" }]],
  workers: 1,
  fullyParallel: false,
  use: {
    baseURL: process.env.PLAYWRIGHT_BASE_URL ?? "http://localhost:5173",
    trace: "off",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
});
