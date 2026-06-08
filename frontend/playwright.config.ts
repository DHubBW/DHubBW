import { defineConfig, devices } from "@playwright/test";

const isCI = !!process.env.CI;

/**
 * Playwright configuration for web E2E tests.
 *
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
    command:
      "npm run build-web && npx --yes serve dist --single --listen 8081",
    url: "http://localhost:8081",
    reuseExistingServer: !isCI,
    timeout: 180_000,
  },
});
