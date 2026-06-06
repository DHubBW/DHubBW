/** @type {import('lint-staged').Config} */
const config = {
  "*.{ts,tsx}": ["eslint --fix", "prettier --write"],
  "*.{js,jsx,json,md,css}": ["prettier --write"],
};

module.exports = config;
