// https://docs.expo.dev/guides/using-eslint/
const { defineConfig } = require('eslint/config');
const expoConfig = require('eslint-config-expo/flat');
const jsxA11y = require('eslint-plugin-jsx-a11y');

module.exports = defineConfig([
  expoConfig,
  // jsx-a11y greift nur fuer Web-Markup (RN-Native rendert keine HTML-Elemente).
  // Auf RN-Native sind die Regeln no-ops, der Web-Build profitiert davon.
  {
    files: ['**/*.{ts,tsx,js,jsx}'],
    plugins: { 'jsx-a11y': jsxA11y },
    rules: jsxA11y.configs.recommended.rules,
  },
  {
    ignores: ['dist/*'],
  },
]);
