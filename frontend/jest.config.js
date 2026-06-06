/** @type {import('jest').Config} */
const config = {
  preset: "jest-expo",
  setupFilesAfterEnv: ["./jest.setup.js"],
  transformIgnorePatterns: [
    "node_modules/(?!(" +
      ".pnpm|" +
      "react-native|@react-native|@react-native-community|" +
      "expo|@expo|@expo-google-fonts|" +
      "react-navigation|@react-navigation|" +
      "@sentry/react-native|native-base|" +
      "@rn-primitives|" +
      "lucide-react-native|" +
      "nativewind|" +
      "class-variance-authority|" +
      "clsx|" +
      "tailwind-merge|" +
      "sonner-native|" +
      "react-native-worklets|" +
      "msw" +
      "))",
    "node_modules/react-native-reanimated/plugin/",
    "node_modules/@react-native/babel-preset/",
  ],
  projects: [
    {
      displayName: "unit",
      preset: "jest-expo",
      testMatch: [
        "<rootDir>/tests/components/**/*.test.{ts,tsx}",
      ],
      setupFilesAfterEnv: ["<rootDir>/jest.setup.js"],
      transformIgnorePatterns: [
        "node_modules/(?!(" +
          ".pnpm|" +
          "react-native|@react-native|@react-native-community|" +
          "expo|@expo|@expo-google-fonts|" +
          "react-navigation|@react-navigation|" +
          "@sentry/react-native|native-base|" +
          "@rn-primitives|" +
          "lucide-react-native|" +
          "nativewind|" +
          "class-variance-authority|" +
          "clsx|" +
          "tailwind-merge|" +
          "sonner-native|" +
          "react-native-worklets|" +
          "msw" +
          "))",
        "node_modules/react-native-reanimated/plugin/",
        "node_modules/@react-native/babel-preset/",
      ],
    },
    {
      displayName: "integration",
      testEnvironment: "node",
      testMatch: [
        "<rootDir>/tests/integration/**/*.test.{ts,tsx}",
      ],
      setupFilesAfterEnv: ["<rootDir>/jest.setup.integration.js"],
      transform: {
        "^.+\\.[jt]sx?$": [
          "babel-jest",
          { presets: ["babel-preset-expo"] },
        ],
        "^.+\\.mjs$": [
          "babel-jest",
          { presets: ["babel-preset-expo"] },
        ],
      },
      transformIgnorePatterns: [
        "node_modules/(?!(" +
          "msw|rettime|until-async|@open-draft/deferred-promise|headers-polyfill|" +
          "expo|@expo" +
          "))",
      ],
      moduleNameMapper: {
        "^msw/node$":
          "<rootDir>/node_modules/msw/lib/node/index.js",
        "^msw$":
          "<rootDir>/node_modules/msw/lib/core/index.js",
      },
    },
  ],
};

module.exports = config;
