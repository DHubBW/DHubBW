/**
 * Integration test for the backend REST API stubs.
 *
 * Uses MSW to intercept HTTP requests and return schema-conformant stub
 * responses. The MSW server is started/stopped globally in jest.setup.integration.js.
 */

describe("GET /student/dashboard/profile", () => {
  it("returns the profile stub with correct fields", async () => {
    const response = await fetch(
      "http://localhost:8080/student/dashboard/profile",
    );
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body).toMatchObject({
      name: expect.any(String),
      dhbwMail: expect.stringContaining("@dhbw-karlsruhe.de"),
      studentType: expect.stringMatching(/^(STUDENT|KURSADMIN|SYSTEMADMIN)$/),
    });
  });

  it("returns the current-week meals stub", async () => {
    const response = await fetch(
      "http://localhost:8080/student/meals/current-week",
    );
    const body = await response.json();

    expect(response.status).toBe(200);
    expect(body).toHaveProperty("Mo");
    expect(Array.isArray(body.Mo)).toBe(true);
  });
});
