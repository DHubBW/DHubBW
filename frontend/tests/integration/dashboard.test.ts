/**
 * Integration tests for the dashboard API stubs.
 *
 * Verifies that MSW handlers for the four new dashboard endpoints
 * return schema-conformant responses.
 */

describe("Dashboard API stubs", () => {
  it("GET /student/dashboard/meals/today returns a meal array", async () => {
    const response = await fetch(
      "http://localhost:8080/student/dashboard/meals/today",
    );
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(Array.isArray(body)).toBe(true);
    expect(body.length).toBeGreaterThan(0);
    expect(body[0]).toMatchObject({
      gerichtID: expect.any(String),
      essensbezeichnung: expect.any(String),
      preis: expect.any(Number),
      ernaehrung: expect.stringMatching(/^(FLEISCH|VEGETARISCH|VEGAN)$/),
      linie: expect.any(String),
    });
  });

  it("GET /student/dashboard/dateien/latest returns a file array", async () => {
    const response = await fetch(
      "http://localhost:8080/student/dashboard/dateien/latest",
    );
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(Array.isArray(body)).toBe(true);
    expect(body.length).toBeGreaterThan(0);
    expect(body[0]).toMatchObject({
      dateiID: expect.any(String),
      name: expect.any(String),
      zuletztBearbeitet: expect.stringMatching(/^\d{4}-\d{2}-\d{2}$/),
      downloadAnzahl: expect.any(Number),
    });
  });

  it("GET /kursadmin/mitglieder/stats returns KursAdminStatsDTO shape", async () => {
    const response = await fetch(
      "http://localhost:8080/kursadmin/mitglieder/stats",
    );
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body).toMatchObject({
      anzahlMitglieder: expect.any(Number),
      anzahlDateien: expect.any(Number),
      anzahlAnfragen: expect.any(Number),
    });
  });

  it("GET /systemadmin/stats returns SystemAdminStatsDTO shape", async () => {
    const response = await fetch("http://localhost:8080/systemadmin/stats");
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body).toMatchObject({
      anzahlKurse: expect.any(Number),
      anzahlNutzer: expect.any(Number),
      anzahlDateien: expect.any(Number),
      megabyteBelegt: expect.any(Number),
      megabyteVerfuegbar: expect.any(Number),
      anzahlKurserstellungsanfragen: expect.any(Number),
    });
  });
});
