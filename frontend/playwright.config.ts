import { defineConfig, devices } from "@playwright/test";

const isCI = !!process.env.CI;

/**
 * Playwright configuration for web E2E tests.
 *
 * Local: all three browser engines, no worker cap, 60 s timeout.
 * CI (vm553): Chromium only, 1 worker, 30 s timeout, 1 retry — keeps RAM
 * usage low on the shared CI runner.
 *
 * @see https://playwright.dev/docs/test-configuration
 */
export default defineConfig({
  testDir: "./e2e",
  fullyParallel: true,
  forbidOnly: isCI,
  retries: isCI ? 1 : 0,
  workers: isCI ? 1 : undefined,
  reporter: "html",
  timeout: isCI ? 30_000 : 60_000,

  use: {
    baseURL: "http://localhost:8081",
    trace: "on-first-retry",
  },

  projects: isCI
    ? [
        {
          name: "chromium",
          use: { ...devices["Desktop Chrome"] },
        },
      ]
    : [
        {
          name: "chromium",
          use: { ...devices["Desktop Chrome"] },
        },
        {
          name: "firefox",
          use: { ...devices["Desktop Firefox"] },
        },
        {
          name: "webkit",
          use: { ...devices["Desktop Safari"] },
        },
      ],

  webServer: {
    command: "npx expo start --web --port 8081",
    url: "http://localhost:8081",
    reuseExistingServer: !isCI,
    timeout: 120_000,
  },
});
