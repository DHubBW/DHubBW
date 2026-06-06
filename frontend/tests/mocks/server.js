/**
 * MSW server setup for integration tests.
 *
 * Requires the pre-built CJS file directly so that Jest does not follow
 * package.json exports to the pure-ESM source tree.
 */

/* eslint-disable @typescript-eslint/no-require-imports */
const { setupServer } = require("../../node_modules/msw/lib/node/index.js");
const { handlers } = require("./handlers.js");
/* eslint-enable @typescript-eslint/no-require-imports */

const server = setupServer(...handlers);

module.exports = { server };
