import { test, expect } from "@playwright/test";

/**
 * E2E stub for the login flow.
 *
 * Phase 1 (FE-S3): verifies that the login page (or its redirect) loads
 * without a JS error. Full form + API interaction will be added once the
 * real auth implementation lands (FE-S1 #206).
 */
test.describe("Login", () => {
  test("app root loads without a page crash", async ({ page }) => {
    await page.goto("/");

    // The stub auth always returns a user, so we expect a redirect to the
    // dashboard rather than the login screen. Either way the page must load.
    await expect(page).not.toHaveURL(/error/);
    await expect(page.locator("body")).toBeVisible();
  });
});
