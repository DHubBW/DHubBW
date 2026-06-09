/**
 * Integration tests for KursAdmin Verwaltung endpoints.
 *
 * MSW server is started/reset/stopped via jest.setup.integration.js.
 */
describe("GET /kursadmin/anfragen", () => {
  it("gibt die Beitrittsanfragen-Liste zurück", async () => {
    const response = await fetch("http://localhost:8080/kursadmin/anfragen");
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(Array.isArray(body)).toBe(true);
    expect(body[0]).toHaveProperty("anfrageID");
    expect(body[0]).toHaveProperty("studentName");
    expect(body[0]).toHaveProperty("anfrageDatum");
  });
});

describe("PATCH /kursadmin/anfragen/:id/approve", () => {
  it("nimmt eine Anfrage an (200)", async () => {
    const response = await fetch(
      "http://localhost:8080/kursadmin/anfragen/eeee0001-0000-0000-0000-000000000001/approve",
      { method: "PATCH" },
    );

    expect(response.status).toBe(200);
  });
});

describe("DELETE /kursadmin/anfragen/:id", () => {
  it("lehnt eine Anfrage ab (204)", async () => {
    const response = await fetch(
      "http://localhost:8080/kursadmin/anfragen/eeee0001-0000-0000-0000-000000000001",
      { method: "DELETE" },
    );

    expect(response.status).toBe(204);
  });
});

describe("GET /kursadmin/mitglieder", () => {
  it("gibt die Mitgliederliste zurück", async () => {
    const response = await fetch("http://localhost:8080/kursadmin/mitglieder");
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(Array.isArray(body)).toBe(true);
    expect(body[0]).toHaveProperty("studentID");
    expect(body[0]).toHaveProperty("name");
    expect(body[0]).toHaveProperty("dhbwMail");
    expect(body[0]).toHaveProperty("studentType");
  });
});

describe("PATCH /kursadmin/mitglieder/add-kursadmin/:studentId", () => {
  it("ernennt ein Mitglied zum Kursadmin (204)", async () => {
    const response = await fetch(
      "http://localhost:8080/kursadmin/mitglieder/add-kursadmin/ffff0001-0000-0000-0000-000000000003",
      { method: "PATCH" },
    );

    expect(response.status).toBe(204);
  });
});

describe("PATCH /kursadmin/mitglieder/remove-kursadmin/:studentId", () => {
  it("stuft einen Kursadmin zurück (200)", async () => {
    const response = await fetch(
      "http://localhost:8080/kursadmin/mitglieder/remove-kursadmin/ffff0001-0000-0000-0000-000000000002",
      { method: "PATCH" },
    );

    expect(response.status).toBe(200);
  });
});

describe("DELETE /kursadmin/mitglieder/:studentId", () => {
  it("entfernt ein Mitglied aus dem Kurs (204)", async () => {
    const response = await fetch(
      "http://localhost:8080/kursadmin/mitglieder/ffff0001-0000-0000-0000-000000000003",
      { method: "DELETE" },
    );

    expect(response.status).toBe(204);
  });
});

describe("PATCH /kursadmin/mitglieder/resign", () => {
  it("tritt als Kursadmin zurück (204)", async () => {
    const response = await fetch(
      "http://localhost:8080/kursadmin/mitglieder/resign",
      { method: "PATCH" },
    );

    expect(response.status).toBe(204);
  });
});
