// Integration test setup — no MSW server here.
// MSW is set up per test-file via tests/mocks/server.js to avoid loading
// pure-ESM transitive dependencies in the jest.setup context.

// MSW server – started before each test file, reset after each test, closed after all tests.
/* eslint-disable @typescript-eslint/no-require-imports */
const { server } = require("./tests/mocks/server.js");
/* eslint-enable @typescript-eslint/no-require-imports */

beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());
