// https://docs.expo.dev/guides/using-eslint/
const { defineConfig } = require('eslint/config');
const expoConfig = require('eslint-config-expo/flat');
const jsxA11y = require('eslint-plugin-jsx-a11y');
const jsdoc = require('eslint-plugin-jsdoc');

module.exports = defineConfig([
  expoConfig,
  // jsx-a11y only applies to web markup (RN native renders no HTML elements).
  // On RN native the rules are no-ops; the web build benefits from them.
  {
    files: ['**/*.{ts,tsx,js,jsx}'],
    plugins: { 'jsx-a11y': jsxA11y },
    rules: jsxA11y.configs.recommended.rules,
  },
  // JSDoc enforcement for all exported functions, components and hooks.
  // Ensures TypeDoc can generate complete documentation from source comments,
  // analogous to Javadoc in the backend project.
  {
    files: ['**/*.{ts,tsx}'],
    plugins: { jsdoc },
    rules: {
      // Warn when an exported function / component has no JSDoc block.
      'jsdoc/require-jsdoc': [
        'warn',
        {
          publicOnly: true,
          require: {
            FunctionDeclaration: true,
            ArrowFunctionExpression: true,
            FunctionExpression: true,
          },
          contexts: [
            // Named export arrow functions: export const Foo = () => ...
            'ExportNamedDeclaration > VariableDeclaration > VariableDeclarator > ArrowFunctionExpression',
            // Named export function expressions: export const Foo = function() {}
            'ExportNamedDeclaration > VariableDeclaration > VariableDeclarator > FunctionExpression',
            // Default export function declarations: export default function Foo() {}
            'ExportDefaultDeclaration > FunctionDeclaration',
            // Default export arrow functions: export default () => {}
            'ExportDefaultDeclaration > ArrowFunctionExpression',
          ],
        },
      ],
      // Warn on a missing @param tag inside an existing JSDoc block.
      // checkDestructured=false: prop types are already documented via TypeScript,
      // repeating every destructured field as @param would be redundant noise.
      'jsdoc/require-param': ['warn', { enableFixer: false, checkDestructured: false }],
      // Warn on a missing @returns tag inside an existing JSDoc block.
      'jsdoc/require-returns': ['warn', { enableFixer: false }],
      // Warn when a JSDoc block has an empty description.
      'jsdoc/require-description': [
        'warn',
        { contexts: ['FunctionDeclaration', 'ArrowFunctionExpression'] },
      ],
    },
  },
  {
    ignores: ['dist/*'],
  },
]);
