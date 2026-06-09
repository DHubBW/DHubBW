import { test as base, expect, type Page } from "@playwright/test";
import type { StudentProfileDTO } from "../../lib/api/types.gen";

/**
 * Stub profile that mirrors the MSW handler in tests/mocks/handlers.ts.
 * Kept in sync manually — field names must match StudentProfileDTO exactly.
 */
const STUB_PROFILE: StudentProfileDTO = {
  name: "Test Student",
  dhbwMail: "test.student@dhbw-karlsruhe.de",
  studentType: "STUDENT",
  kursName: null,
  pendingKursRequest: null,
};

/**
 * Stub profile for a user that is enrolled in a course (kursName set).
 * Used for tests that require the "Kurs verlassen" button to be visible.
 */
export const STUB_PROFILE_WITH_KURS = {
  ...STUB_PROFILE,
  kursName: "TINF24B2",
};

/**
 * Configures a Playwright page with two preconditions required for
 * authenticated-screen E2E tests:
 *
 * 1. **CookieBanner suppressed** — sets `cookie_consent=accepted` in
 *    localStorage before the first page load so the `fixed; z-index: 9999`
 *    overlay never mounts and does not intercept pointer events.
 *
 * 2. **Auth stub via `page.route()`** — intercepts
 *    `GET *\/student/dashboard/profile` and returns the stub profile so
 *    `AuthContext.useProfile` resolves with `user !== null`, preventing
 *    `ProtectedRoute` from redirecting to `/login`.
 *
 * Both must be registered BEFORE `page.goto()` so they are active for
 * the very first document load.
 *
 * @param page - Playwright page to configure.
 * @param profile - Profile stub to return (default: STUB_PROFILE).
 */
export async function setupAuthenticatedPage(
  page: Page,
  profile = STUB_PROFILE,
): Promise<void> {
  // CookieBanner suppression: addInitScript runs before React hydration,
  // before useCookieConsent's localStorage.getItem call.
  await page.addInitScript(() => {
    localStorage.setItem("cookie_consent", "accepted");
  });

  // Auth stub: intercepts the profile endpoint so AuthContext boots with a
  // real user without needing a running backend on :8080.
  await page.route("**/student/dashboard/profile", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(profile),
    }),
  );
}

/**
 * Extended Playwright test fixture that provides an `authenticatedPage`
 * pre-configured with cookie consent and auth stub (STUB_PROFILE, no kurs).
 *
 * Usage:
 * ```ts
 * import { test, expect } from './fixtures/auth';
 * test('example', async ({ authenticatedPage }) => { ... });
 * ```
 */
export const test = base.extend<{
  authenticatedPage: Page;
  authenticatedPageWithKurs: Page;
}>({
  authenticatedPage: async ({ page }, use) => {
    await setupAuthenticatedPage(page);
    await use(page);
  },
  authenticatedPageWithKurs: async ({ page }, use) => {
    await setupAuthenticatedPage(page, STUB_PROFILE_WITH_KURS);
    await use(page);
  },
});

export { expect };
