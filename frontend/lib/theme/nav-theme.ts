import { DarkTheme, DefaultTheme, type Theme } from '@react-navigation/native';

// Mirror der CSS-Variablen aus global.css fuer @react-navigation/native.
// Nur die Felder, die NavigationThemeProvider erwartet (background/border/
// card/notification/primary/text). RN-Komponenten ausserhalb von Tailwind
// (Stack-Header, Tab-Bar) ziehen ihre Farben hierher.
const LIGHT = {
  background: 'hsl(0 0% 98%)',
  foreground: 'hsl(214.3 32.6% 16.9%)',
  card: 'hsl(0 0% 100%)',
  primary: 'hsl(350.2 85.2% 42.4%)',
  border: 'hsl(214.3 31.8% 91.4%)',
  destructive: 'hsl(0 72.2% 50.6%)',
};

const DARK = {
  background: 'hsl(220 9.1% 6.5%)',
  foreground: 'hsl(240 13% 95.5%)',
  card: 'hsl(216 8.8% 11.2%)',
  primary: 'hsl(350.2 85.2% 42.4%)',
  border: 'hsl(214.3 7.7% 17.8%)',
  destructive: 'hsl(354.9 73.8% 53.5%)',
};

export const NAV_THEME: Record<'light' | 'dark', Theme> = {
  light: {
    ...DefaultTheme,
    colors: {
      background: LIGHT.background,
      border: LIGHT.border,
      card: LIGHT.card,
      notification: LIGHT.destructive,
      primary: LIGHT.primary,
      text: LIGHT.foreground,
    },
  },
  dark: {
    ...DarkTheme,
    colors: {
      background: DARK.background,
      border: DARK.border,
      card: DARK.card,
      notification: DARK.destructive,
      primary: DARK.primary,
      text: DARK.foreground,
    },
  },
};
