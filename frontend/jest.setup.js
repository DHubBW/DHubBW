// Mock react-native-reanimated (reanimated 4.x dropped /mock export — inline stub)
jest.mock("react-native-reanimated", () => {
  const Animated = require("react-native").Animated; // eslint-disable-line @typescript-eslint/no-require-imports
  return {
    __esModule: true,
    default: {
      View: require("react-native").View, // eslint-disable-line @typescript-eslint/no-require-imports
      Text: require("react-native").Text, // eslint-disable-line @typescript-eslint/no-require-imports
      Image: require("react-native").Image, // eslint-disable-line @typescript-eslint/no-require-imports
      ScrollView: require("react-native").ScrollView, // eslint-disable-line @typescript-eslint/no-require-imports
      FlatList: require("react-native").FlatList, // eslint-disable-line @typescript-eslint/no-require-imports
      createAnimatedComponent: (c) => c,
    },
    Easing: { linear: (t) => t, ease: (t) => t, bezier: () => (t) => t, in: (e) => e, out: (e) => e, inOut: (e) => e },
    useAnimatedStyle: (fn) => fn(),
    useAnimatedProps: (fn) => fn(),
    useSharedValue: (v) => ({ value: v }),
    useDerivedValue: (fn) => ({ value: fn() }),
    useAnimatedRef: () => ({ current: null }),
    useAnimatedScrollHandler: () => {},
    useAnimatedGestureHandler: () => {},
    withTiming: (v) => v,
    withSpring: (v) => v,
    withDecay: () => 0,
    withDelay: (_d, v) => v,
    withRepeat: (v) => v,
    withSequence: (...vs) => vs[vs.length - 1],
    runOnJS: (fn) => fn,
    runOnUI: (fn) => fn,
    makeMutable: (v) => ({ value: v }),
    FadeIn: { duration: () => ({ withInitialValues: () => ({}) }) },
    FadeInDown: { withInitialValues: () => ({ duration: () => ({}) }), duration: () => ({}) },
    FadeInUp: { withInitialValues: () => ({}) },
    FadeOut: {},
    Layout: {},
    createAnimatedComponent: (c) => c,
  };
});

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
