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

function applyWebClass(effective: EffectiveTheme) {
  if (Platform.OS !== 'web' || typeof document === 'undefined') return;
  document.documentElement.classList.toggle('dark', effective === 'dark');
}

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

export function useTheme(): ThemeContextValue {
  const ctx = useContext(ThemeContext);
  if (!ctx) {
    throw new Error('useTheme must be used inside <ThemeProvider>');
  }
  return ctx;
}
