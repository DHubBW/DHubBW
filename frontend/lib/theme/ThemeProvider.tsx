import AsyncStorage from '@react-native-async-storage/async-storage';
import * as SystemUI from 'expo-system-ui';
import { useColorScheme as useNativewindColorScheme } from 'nativewind';
import {
  createContext,
  type ReactNode,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';
import { Platform, useColorScheme as useRNColorScheme } from 'react-native';

export type ThemePreference = 'system' | 'light' | 'dark';
export type EffectiveTheme = 'light' | 'dark';

type ThemeContextValue = {
  theme: ThemePreference;
  effectiveTheme: EffectiveTheme;
  setTheme: (next: ThemePreference) => void;
};

const STORAGE_KEY = 'dhubbw.theme';
const DEFAULT_THEME: ThemePreference = 'system';

const ThemeContext = createContext<ThemeContextValue | null>(null);

/**
 * Adds or removes the CSS class `dark` on the `<html>` element (web only).
 * Required by NativeWind to activate dark-mode styles on the web platform.
 *
 * @param effective - The currently active theme (`'light'` or `'dark'`).
 */
function applyWebClass(effective: EffectiveTheme) {
  if (Platform.OS !== 'web' || typeof document === 'undefined') return;
  document.documentElement.classList.toggle('dark', effective === 'dark');
}

/**
 * Provides the app-wide theme and persists the user preference via AsyncStorage.
 * Keeps NativeWind and the native system UI in sync with the effective theme.
 *
 * @param children - Child components that need access to the theme context.
 * @returns The ThemeContext provider, or `null` while hydrating from storage.
 */
export function ThemeProvider({ children }: { children: ReactNode }) {
  const systemScheme = useRNColorScheme();
  const { setColorScheme } = useNativewindColorScheme();
  const [theme, setThemeState] = useState<ThemePreference>(DEFAULT_THEME);
  const [hydrated, setHydrated] = useState(false);

  useEffect(() => {
    AsyncStorage.getItem(STORAGE_KEY)
      .then((value) => {
        if (value === 'light' || value === 'dark' || value === 'system') {
          setThemeState(value);
        }
      })
      .finally(() => setHydrated(true));
  }, []);

  const effectiveTheme: EffectiveTheme = useMemo(() => {
    if (theme === 'system') return systemScheme === 'dark' ? 'dark' : 'light';
    return theme;
  }, [theme, systemScheme]);

  useEffect(() => {
    setColorScheme(effectiveTheme);
    applyWebClass(effectiveTheme);
    SystemUI.setBackgroundColorAsync(
      effectiveTheme === 'dark' ? '#0F1012' : '#FAFAFA'
    ).catch(() => {});
  }, [effectiveTheme, setColorScheme]);

  const setTheme = useCallback((next: ThemePreference) => {
    setThemeState(next);
    AsyncStorage.setItem(STORAGE_KEY, next).catch(() => {});
  }, []);

  const value = useMemo<ThemeContextValue>(
    () => ({ theme, effectiveTheme, setTheme }),
    [theme, effectiveTheme, setTheme]
  );

  if (!hydrated) return null;

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

/**
 * Hook that returns the current theme context.
 *
 * Must be called inside a {@link ThemeProvider}.
 *
 * @returns `theme` (stored preference), `effectiveTheme` (active theme),
 *   and `setTheme` (setter for the stored preference).
 * @throws {Error} When called outside a `ThemeProvider`.
 */
export function useTheme(): ThemeContextValue {
  const ctx = useContext(ThemeContext);
  if (!ctx) {
    throw new Error('useTheme must be used inside <ThemeProvider>');
  }
  return ctx;
}
