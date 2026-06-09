/**
 * Integration tests for systemadmin REST endpoints via MSW.
 *
 * Verifies that all six /systemadmin/* endpoints respond with
 * schema-conformant data as defined in the MSW handlers.
 */

describe("GET /systemadmin/stats", () => {
  it("returns platform-wide KPI stats", async () => {
    const res = await fetch("http://localhost:8080/systemadmin/stats");
    const body = await res.json();

    expect(res.status).toBe(200);
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

describe("GET /systemadmin/kurse", () => {
  it("returns an array of KursDTO", async () => {
    const res = await fetch("http://localhost:8080/systemadmin/kurse");
    const body = await res.json();

    expect(res.status).toBe(200);
    expect(Array.isArray(body)).toBe(true);
    expect(body.length).toBeGreaterThan(0);
    expect(body[0]).toMatchObject({
      kursID: expect.any(String),
      kursName: expect.any(String),
      jahrgang: expect.any(String),
    });
  });
});

describe("GET /systemadmin/kurse/:kursId/mitglieder", () => {
  it("returns member-role list for a course", async () => {
    const res = await fetch(
      "http://localhost:8080/systemadmin/kurse/aaaa0001-0000-0000-0000-000000000001/mitglieder",
    );
    const body = await res.json();

    expect(res.status).toBe(200);
    expect(Array.isArray(body)).toBe(true);
    expect(body[0]).toMatchObject({
      studentName: expect.any(String),
      rolle: expect.any(String),
    });
  });
});

describe("GET /systemadmin/anfragen", () => {
  it("returns an array of KursErstellungsAnfrageDTO", async () => {
    const res = await fetch("http://localhost:8080/systemadmin/anfragen");
    const body = await res.json();

    expect(res.status).toBe(200);
    expect(Array.isArray(body)).toBe(true);
    expect(body[0]).toMatchObject({
      anfrageID: expect.any(String),
      studentName: expect.any(String),
      kursName: expect.any(String),
      jahrgang: expect.any(String),
      anfrageDatum: expect.any(String),
    });
  });
});

describe("PATCH /systemadmin/anfragen/:id/approve", () => {
  it("returns 200 on approve", async () => {
    const res = await fetch(
      "http://localhost:8080/systemadmin/anfragen/bbbb0001-0000-0000-0000-000000000001/approve",
      { method: "PATCH" },
    );
    expect(res.status).toBe(200);
  });
});

describe("DELETE /systemadmin/anfragen/:id", () => {
  it("returns 204 on decline", async () => {
    const res = await fetch(
      "http://localhost:8080/systemadmin/anfragen/bbbb0001-0000-0000-0000-000000000001",
      { method: "DELETE" },
    );
    expect(res.status).toBe(204);
  });
});

describe("GET /systemadmin/studenten", () => {
  it("returns an array of StudentDTO with studentType", async () => {
    const res = await fetch("http://localhost:8080/systemadmin/studenten");
    const body = await res.json();

    expect(res.status).toBe(200);
    expect(Array.isArray(body)).toBe(true);
    const sa = body.find(
      (s: { studentType: string }) => s.studentType === "SYSTEMADMIN",
    );
    expect(sa).toBeDefined();
    expect(sa).toMatchObject({
      studentID: expect.any(String),
      name: expect.any(String),
      dhbwMail: expect.any(String),
      studentType: "SYSTEMADMIN",
    });
  });
});

describe("PATCH /systemadmin/studenten/:studentId/add-sysadmin", () => {
  it("returns 200 on add", async () => {
    const res = await fetch(
      "http://localhost:8080/systemadmin/studenten/dddd0001-0000-0000-0000-000000000002/add-sysadmin",
      { method: "PATCH" },
    );
    expect(res.status).toBe(200);
  });
});

describe("PATCH /systemadmin/studenten/:studentId/remove-sysadmin", () => {
  it("returns 200 on remove", async () => {
    const res = await fetch(
      "http://localhost:8080/systemadmin/studenten/dddd0001-0000-0000-0000-000000000001/remove-sysadmin",
      { method: "PATCH" },
    );
    expect(res.status).toBe(200);
  });
});

describe("DELETE /systemadmin/kurse/:kursId", () => {
  it("returns 204 on delete", async () => {
    const res = await fetch(
      "http://localhost:8080/systemadmin/kurse/aaaa0001-0000-0000-0000-000000000001",
      { method: "DELETE" },
    );
    expect(res.status).toBe(204);
  });
});
