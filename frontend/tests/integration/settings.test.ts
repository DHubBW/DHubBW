/**
 * Integration tests for settings-related backend stubs.
 *
 * Verifies the MSW handlers for logout, leave-kurs and delete-account
 * return the expected status codes as consumed by the SettingsModal flows.
 */

describe("POST /student/dashboard/logout", () => {
  it("returns 204 No Content on logout", async () => {
    const response = await fetch(
      "http://localhost:8080/student/dashboard/logout",
      { method: "POST" },
    );

    expect(response.status).toBe(204);
  });
});

describe("GET /student/dashboard/leave-kurs", () => {
  it("returns 200 with response body on success", async () => {
    const response = await fetch(
      "http://localhost:8080/student/dashboard/leave-kurs",
    );

    expect(response.status).toBe(200);
    const body = await response.json();
    expect(body).toHaveProperty("response");
  });
});

describe("DELETE /student/dashboard/delete-account", () => {
  it("returns 200 with RequestStatusResponseDTO on success", async () => {
    const response = await fetch(
      "http://localhost:8080/student/dashboard/delete-account",
      { method: "DELETE" },
    );

    expect(response.status).toBe(200);
    const body = await response.json();
    expect(body).toHaveProperty("response");
    expect(body).toHaveProperty("persistenceOperation");
  });
});
