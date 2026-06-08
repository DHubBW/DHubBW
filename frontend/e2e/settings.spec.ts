import { type Page } from "@playwright/test";
import { test, expect, STUB_PROFILE_WITH_KURS } from "./fixtures/auth";
import { setupAuthenticatedPage } from "./fixtures/auth";

/**
 * E2E tests for the SettingsModal "Kurs verlassen" and "Account löschen" flows.
 *
 * Phase: FE-Settings-full (#220).
 *
 * These tests use page.route() to stub GET /student/dashboard/profile so the
 * AuthContext boots with user !== null without needing a running backend.
 * localStorage is pre-seeded with cookie_consent=accepted via addInitScript
 * so the CookieBanner overlay does not block interactions.
 */

/** Opens the settings modal regardless of viewport (sidebar or header). */
async function openSettingsModal(page: Page): Promise<void> {
  await page.waitForURL("**/dashboard");

  const sidebarBtn = page.getByTestId("sidebar-settings-button");
  const headerBtn = page.getByTestId("header-settings-button");

  if (await sidebarBtn.isVisible()) {
    await sidebarBtn.click();
  } else {
    await headerBtn.click();
  }

  await expect(page.getByText("Einstellungen")).toBeVisible();
}

test.describe("SettingsModal – Kurs verlassen", () => {
  test("opens leave-kurs confirm dialog when 'Kurs verlassen' is pressed", async ({
    page,
  }) => {
    // Use kurs-profile so the "Kurs verlassen" button is visible
    await setupAuthenticatedPage(page, STUB_PROFILE_WITH_KURS);
    await page.goto("/dashboard");
    await openSettingsModal(page);

    const leaveKursButton = page.getByTestId("settings-leave-kurs-button");
    await expect(leaveKursButton).toBeVisible();
    await leaveKursButton.click();

    await expect(page.getByText("Kurs verlassen?")).toBeVisible();
    await expect(
      page.getByText(/Du verlierst Zugriff auf alle Kursdateien/),
    ).toBeVisible();
  });

  test("cancel button closes the confirm dialog without action", async ({
    page,
  }) => {
    await setupAuthenticatedPage(page, STUB_PROFILE_WITH_KURS);
    await page.goto("/dashboard");
    await openSettingsModal(page);

    await page.getByTestId("settings-leave-kurs-button").click();
    await page.getByTestId("confirm-dialog-cancel").click();

    await expect(page.getByText("Kurs verlassen?")).not.toBeVisible();
    await expect(page.getByText("Einstellungen")).toBeVisible();
  });

  test("'Kurs verlassen' button is hidden when user has no kursName", async ({
    authenticatedPage,
  }) => {
    // authenticatedPage uses STUB_PROFILE with kursName: null
    await authenticatedPage.goto("/dashboard");
    await openSettingsModal(authenticatedPage);

    await expect(
      authenticatedPage.getByTestId("settings-leave-kurs-button"),
    ).not.toBeVisible();
  });
});

test.describe("SettingsModal – Account löschen", () => {
  test("opens delete-account strict confirm dialog when 'Account löschen' is pressed", async ({
    authenticatedPage,
  }) => {
    await authenticatedPage.goto("/dashboard");
    await openSettingsModal(authenticatedPage);
    await authenticatedPage.getByTestId("settings-delete-account-button").click();

    await expect(authenticatedPage.getByText("Account löschen?")).toBeVisible();
    await expect(
      authenticatedPage.getByText(/unwiderruflich verloren/),
    ).toBeVisible();
    await expect(
      authenticatedPage.getByTestId("confirm-dialog-strict-input"),
    ).toBeVisible();
  });

  test("confirm button is disabled until LÖSCHEN is typed", async ({
    authenticatedPage,
  }) => {
    await authenticatedPage.goto("/dashboard");
    await openSettingsModal(authenticatedPage);
    await authenticatedPage.getByTestId("settings-delete-account-button").click();

    const confirmButton = authenticatedPage.getByTestId(
      "confirm-dialog-strict-confirm",
    );
    await expect(confirmButton).toBeDisabled();

    await authenticatedPage
      .getByTestId("confirm-dialog-strict-input")
      .fill("LÖSCHEN");
    await expect(confirmButton).toBeEnabled();
  });

  test("confirm button stays disabled when input does not exactly match (case-sensitive)", async ({
    authenticatedPage,
  }) => {
    await authenticatedPage.goto("/dashboard");
    await openSettingsModal(authenticatedPage);
    await authenticatedPage.getByTestId("settings-delete-account-button").click();

    await authenticatedPage
      .getByTestId("confirm-dialog-strict-input")
      .fill("löschen");
    await expect(
      authenticatedPage.getByTestId("confirm-dialog-strict-confirm"),
    ).toBeDisabled();
  });

  test("cancel button closes the confirm dialog without action", async ({
    authenticatedPage,
  }) => {
    await authenticatedPage.goto("/dashboard");
    await openSettingsModal(authenticatedPage);
    await authenticatedPage.getByTestId("settings-delete-account-button").click();
    await authenticatedPage.getByTestId("confirm-dialog-strict-cancel").click();

    await expect(
      authenticatedPage.getByText("Account löschen?"),
    ).not.toBeVisible();
    await expect(authenticatedPage.getByText("Einstellungen")).toBeVisible();
  });
});
