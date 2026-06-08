/**
 * Integration tests for `GET /student/meals/current-week` and
 * `GET /student/meals/next-week`.
 *
 * The MSW server starts and stops automatically via jest.setup.integration.js.
 * Tests verify that the stubs return the correct WeekMealsMap shape and that
 * individual dish entries contain the expected MensaDTO fields.
 */

describe("GET /student/meals/current-week", () => {
  it("returns a WeekMealsMap with weekday keys Mo–Fr", async () => {
    const response = await fetch(
      "http://localhost:8080/student/meals/current-week",
    );
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body).toHaveProperty("Mo");
    expect(body).toHaveProperty("Di");
    expect(body).toHaveProperty("Mi");
    expect(body).toHaveProperty("Do");
    expect(body).toHaveProperty("Fr");
  });

  it("returns MensaDTO-conformant entries for Monday", async () => {
    const response = await fetch(
      "http://localhost:8080/student/meals/current-week",
    );
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(Array.isArray(body.Mo)).toBe(true);
    expect(body.Mo.length).toBeGreaterThan(0);

    const first = body.Mo[0];
    expect(first).toHaveProperty("gerichtID");
    expect(first).toHaveProperty("datum");
    expect(first).toHaveProperty("essensbezeichnung");
    expect(first).toHaveProperty("allergene");
    expect(first).toHaveProperty("preis");
    expect(first).toHaveProperty("ernaehrung");
    expect(first).toHaveProperty("linie");
    expect(["VEGAN", "VEGETARISCH", "FLEISCH"]).toContain(first.ernaehrung);
    // allergene is either empty string or bracket-wrapped list e.g. "[1,Se]"
    expect(typeof first.allergene).toBe("string");
  });

  it("returns an array for each weekday (may be empty)", async () => {
    const response = await fetch(
      "http://localhost:8080/student/meals/current-week",
    );
    const body = await response.json();

    expect(response.status).toBe(200);
    for (const key of ["Mo", "Di", "Mi", "Do", "Fr"]) {
      expect(Array.isArray(body[key])).toBe(true);
    }
  });
});

describe("GET /student/meals/next-week", () => {
  it("returns a WeekMealsMap with all weekday keys Mo–Fr", async () => {
    const response = await fetch(
      "http://localhost:8080/student/meals/next-week",
    );
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body).toHaveProperty("Mo");
    expect(body).toHaveProperty("Di");
    expect(body).toHaveProperty("Mi");
    expect(body).toHaveProperty("Do");
    expect(body).toHaveProperty("Fr");
  });

  it("returns an array for each weekday (may be empty)", async () => {
    const response = await fetch(
      "http://localhost:8080/student/meals/next-week",
    );
    const body = await response.json();

    expect(response.status).toBe(200);
    for (const key of ["Mo", "Di", "Mi", "Do", "Fr"]) {
      expect(Array.isArray(body[key])).toBe(true);
    }
  });

  it("returns MensaDTO-conformant dish entries", async () => {
    const response = await fetch(
      "http://localhost:8080/student/meals/next-week",
    );
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(Array.isArray(body.Mo)).toBe(true);

    if (body.Mo.length > 0) {
      const dish = body.Mo[0];
      expect(typeof dish.preis).toBe("number");
      expect(typeof dish.essensbezeichnung).toBe("string");
      expect(typeof dish.allergene).toBe("string");
      expect(["VEGAN", "VEGETARISCH", "FLEISCH"]).toContain(dish.ernaehrung);
    }
  });
});
