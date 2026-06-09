import { test, expect, setupAuthenticatedPage } from "./fixtures/auth";

/**
 * E2E tests for the Kursadmin Verwaltung flow (Issue #217).
 *
 * All API endpoints are stubbed via page.route() so no running backend
 * is required — only the Expo dev server on port 8081.
 */

const STUB_KURSADMIN_PROFILE = {
  name: "Max Müller",
  dhbwMail: "max.mueller@dhbw-karlsruhe.de",
  studentType: "KURSADMIN",
  kursName: "TINF24B2",
  pendingKursRequest: null,
};

/** Stubs all KursAdmin API endpoints needed for the Verwaltung screen. */
async function setupKursadminRoutes(page: Parameters<typeof setupAuthenticatedPage>[0]) {
  await page.route("**/kursadmin/mitglieder/stats", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ anzahlMitglieder: 2, anzahlDateien: 5, anzahlAnfragen: 1 }),
    }),
  );

  await page.route("**/kursadmin/anfragen", (route) =>
    route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify([
        {
          anfrageID: "eeee0001-0000-0000-0000-000000000001",
          kursID: "aaaa0001-0000-0000-0000-000000000001",
          kursName: "TINF24B2",
          studentID: "ffff0001-0000-0000-0000-000000000001",
          studentName: "Lisa Bauer",
          anfrageDatum: "2026-06-01",
        },
      ]),
    }),
  );

  await page.route("**/kursadmin/mitglieder", (route) => {
    if (route.request().method() === "GET") {
      return route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify([
          {
            studentID: "ffff0001-0000-0000-0000-000000000002",
            name: "Max Müller",
            dhbwMail: "max.mueller@dhbw-karlsruhe.de",
            studentType: "KURSADMIN",
          },
          {
            studentID: "ffff0001-0000-0000-0000-000000000003",
            name: "Anna Schmidt",
            dhbwMail: "anna.schmidt@dhbw-karlsruhe.de",
            studentType: "STUDENT",
          },
        ]),
      });
    }
    return route.continue();
  });
}

test.describe("Kursadmin Verwaltung — Anfrage annehmen", () => {
  test("navigiert zu /verwaltung und zeigt Beitrittsanfragen", async ({
    page,
  }) => {
    await setupAuthenticatedPage(page, STUB_KURSADMIN_PROFILE);
    await setupKursadminRoutes(page);
    await page.goto("/verwaltung");

    await expect(page.getByText("Kursverwaltung")).toBeVisible();
    await expect(page.getByText("Beitrittsanfragen")).toBeVisible();
  });

  test("Annehmen-Button öffnet ConfirmDialog", async ({ page }) => {
    await setupAuthenticatedPage(page, STUB_KURSADMIN_PROFILE);
    await setupKursadminRoutes(page);
    await page.goto("/verwaltung");

    await expect(page.getByText("Lisa Bauer")).toBeVisible();
    await page.getByText("Annehmen").first().click();

    await expect(page.getByText("Anfrage annehmen")).toBeVisible();
  });

  test("Anfrage bestätigen: Dialog bestätigen zeigt Toast", async ({
    page,
  }) => {
    await setupAuthenticatedPage(page, STUB_KURSADMIN_PROFILE);
    await setupKursadminRoutes(page);

    await page.route("**/kursadmin/anfragen/**/approve", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ response: "approved", persistenceOperation: "UPDATE" }),
      }),
    );

    await page.goto("/verwaltung");

    await expect(page.getByText("Lisa Bauer")).toBeVisible();
    await page.getByText("Annehmen").first().click();

    await expect(page.getByText("Anfrage annehmen")).toBeVisible();
    const confirmBtn = page.getByRole("button", { name: "Annehmen" }).last();
    await confirmBtn.click();

    await expect(page.getByText(/aufgenommen/)).toBeVisible();
  });
});

test.describe("Kursadmin Verwaltung — Mitglied entfernen", () => {
  test("navigiert zu /verwaltung und zeigt Mitgliederliste", async ({
    page,
  }) => {
    await setupAuthenticatedPage(page, STUB_KURSADMIN_PROFILE);
    await setupKursadminRoutes(page);
    await page.goto("/verwaltung");

    await expect(page.getByText("Mitglieder")).toBeVisible();
    await expect(page.getByText("Anna Schmidt")).toBeVisible();
  });

  test("Trash-Button für Studenten öffnet Entfernen-Dialog", async ({
    page,
  }) => {
    await setupAuthenticatedPage(page, STUB_KURSADMIN_PROFILE);
    await setupKursadminRoutes(page);
    await page.goto("/verwaltung");

    await expect(page.getByText("Anna Schmidt")).toBeVisible();

    const buttons = page.getByRole("button");
    const count = await buttons.count();

    let entfernenDialogVisible = false;
    for (let i = 0; i < count; i++) {
      await buttons.nth(i).click();
      if (await page.getByText("Aus Kurs entfernen").isVisible()) {
        entfernenDialogVisible = true;
        break;
      }
      const cancelBtn = page.getByText("Abbrechen");
      if (await cancelBtn.isVisible()) {
        await cancelBtn.click();
      }
    }

    expect(entfernenDialogVisible).toBe(true);
  });

  test("Dialog bestätigen nach Mitglied entfernen zeigt Toast", async ({
    page,
  }) => {
    await setupAuthenticatedPage(page, STUB_KURSADMIN_PROFILE);
    await setupKursadminRoutes(page);

    await page.route("**/kursadmin/mitglieder/**", (route) => {
      if (route.request().method() === "DELETE") {
        return route.fulfill({ status: 204 });
      }
      return route.continue();
    });

    await page.goto("/verwaltung");

    await expect(page.getByText("Anna Schmidt")).toBeVisible();

    const buttons = page.getByRole("button");
    const count = await buttons.count();

    for (let i = 0; i < count; i++) {
      await buttons.nth(i).click();
      if (await page.getByText("Aus Kurs entfernen").isVisible()) {
        await page.getByRole("button", { name: "Entfernen" }).last().click();
        break;
      }
      const cancelBtn = page.getByText("Abbrechen");
      if (await cancelBtn.isVisible()) {
        await cancelBtn.click();
      }
    }

    await expect(page.getByText(/entfernt/)).toBeVisible();
  });
});
