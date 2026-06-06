// Mock react-native-reanimated (required for Expo 54 + reanimated 4.x)
jest.mock("react-native-reanimated", () =>
  require("react-native-reanimated/mock"),
);

// Mock react-native-worklets (peer dependency of reanimated 4.x)
jest.mock("react-native-worklets", () => ({
  useSharedValue: jest.fn(),
  useRunOnJS: jest.fn(),
}));

// Mock react-native-safe-area-context (prevents Platform.OS crash in react-native-css-interop)
jest.mock("react-native-safe-area-context", () => {
  const { View } = require("react-native"); // eslint-disable-line @typescript-eslint/no-require-imports
  return {
    SafeAreaProvider: View,
    SafeAreaView: View,
    useSafeAreaInsets: () => ({ top: 0, bottom: 0, left: 0, right: 0 }),
    useSafeAreaFrame: () => ({ x: 0, y: 0, width: 375, height: 812 }),
  };
});

// Mock expo-secure-store (not installed as a dependency, but mocked for completeness)
jest.mock(
  "expo-secure-store",
  () => ({
    getItemAsync: jest.fn(() => Promise.resolve(null)),
    setItemAsync: jest.fn(() => Promise.resolve()),
    deleteItemAsync: jest.fn(() => Promise.resolve()),
  }),
  { virtual: true },
);

// Mock expo-router
jest.mock("expo-router", () => ({
  useRouter: jest.fn(() => ({
    push: jest.fn(),
    replace: jest.fn(),
    back: jest.fn(),
  })),
  useLocalSearchParams: jest.fn(() => ({})),
  usePathname: jest.fn(() => "/"),
  Link: "Link",
  Redirect: "Redirect",
  Stack: { Screen: "Screen" },
  Tabs: { Screen: "Screen" },
}));

// Force-evaluate expo winter globals to avoid jest@30 "require outside test scope" errors.
// expo/src/winter installs lazy getters; accessing them here resolves the getters during setup.
void global.__ExpoImportMetaRegistry;
void global.TextDecoder;
void global.URL;
void global.URLSearchParams;
void global.structuredClone;
