/**
 * MSW request handlers for integration tests.
 *
 * CommonJS version to allow require() from jest.setup.integration.js
 * without going through the ESM resolution path.
 *
 * @type {{ handlers: import('msw').RequestHandler[] }}
 */

/* eslint-disable @typescript-eslint/no-require-imports */
const {
  http,
  HttpResponse,
} = require("../../node_modules/msw/lib/core/index.js");
/* eslint-enable @typescript-eslint/no-require-imports */

/** Stub profile matching the backend ProfileDto schema. */
const STUB_PROFILE = {
  name: "Test Student",
  dhbwMail: "test.student@dhbw-karlsruhe.de",
  studentType: "STUDENT",
  kursName: null,
  pendingKursRequest: null,
};

/** Stub meal list matching the backend MensaTagDto schema. */
const STUB_MEALS = [
  {
    date: "2026-06-06",
    gerichte: [
      {
        name: "Testgericht",
        preis: "3.50 €",
        kategorie: "Vegan",
      },
    ],
  },
];

const handlers = [
  http.post("http://localhost:8080/public/auth/login", () =>
    HttpResponse.json({ message: "Login erfolgreich" }, { status: 200 }),
  ),

  http.post(
    "http://localhost:8080/public/auth/logout",
    () => new HttpResponse(null, { status: 204 }),
  ),

  http.post(
    "http://localhost:8080/student/dashboard/logout",
    () => new HttpResponse(null, { status: 204 }),
  ),

  http.post("http://localhost:8080/public/auth/register", () =>
    HttpResponse.json(
      { message: "Registrierung erfolgreich" },
      { status: 201 },
    ),
  ),

  http.get("http://localhost:8080/student/dashboard/profile", () =>
    HttpResponse.json(STUB_PROFILE, { status: 200 }),
  ),

  http.get("http://localhost:8080/student/meals/:date", () =>
    HttpResponse.json(STUB_MEALS, { status: 200 }),
  ),
];

module.exports = { handlers };
