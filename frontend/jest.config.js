/** @type {import('jest').Config} */
const config = {
  preset: "jest-expo",
  setupFilesAfterEnv: ["./jest.setup.js"],
  moduleNameMapper: {
    // Prevent react-native-css-interop from accessing Platform before RN is
    // fully initialised in the Jest environment — map to the identity pass-through.
    "^react-native-css-interop$":
      "<rootDir>/node_modules/react-native-css-interop/src/index.ts",
  },
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
      "react-native-css-interop|" +
      "class-variance-authority|" +
      "clsx|" +
      "tailwind-merge|" +
      "sonner-native|" +
      "react-native-worklets" +
      "))",
    "node_modules/react-native-reanimated/plugin/",
    "node_modules/@react-native/babel-preset/",
  ],
};

module.exports = config;
