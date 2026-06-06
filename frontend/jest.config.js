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
      "react-native-worklets" +
      "))",
    "node_modules/react-native-reanimated/plugin/",
    "node_modules/@react-native/babel-preset/",
  ],
};

module.exports = config;
