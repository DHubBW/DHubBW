/**
 * MSW server setup for integration tests.
 *
 * Used in jest.setup.integration.js to intercept HTTP requests during tests.
 */
import { setupServer } from "msw/node";
import { handlers } from "./handlers";

export const server = setupServer(...handlers);
