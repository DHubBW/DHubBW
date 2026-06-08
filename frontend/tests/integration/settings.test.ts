/**
 * Integration test for the settings-related backend stubs.
 *
 * Verifies the MSW handler for `POST /student/dashboard/logout` returns 204
 * as expected by the `useAuth().logout()` flow in `SettingsModal`.
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
