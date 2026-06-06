import '../global.css';

import { ThemeProvider as NavigationThemeProvider } from '@react-navigation/native';
import { PortalHost } from '@rn-primitives/portal';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { Text, View } from 'react-native';
import 'react-native-reanimated';
import { Toaster } from 'sonner-native';

import { useAuth } from '@/lib/auth/__stub';
import { NAV_THEME } from '@/lib/theme/nav-theme';
import { ThemeProvider, useTheme } from '@/lib/theme/ThemeProvider';

/**
 * Internal component that keeps the React Navigation theme provider in sync
 * with the active NativeWind theme.
 *
 * @returns The theme-synchronized navigation stack including StatusBar,
 *   Toaster and PortalHost.
 */
function ThemedStack() {
  const { effectiveTheme } = useTheme();
  return (
    <NavigationThemeProvider value={NAV_THEME[effectiveTheme]}>
      <Stack>
        <Stack.Screen name="(auth)" options={{ headerShown: false }} />
        <Stack.Screen name="(app)" options={{ headerShown: false }} />
        <Stack.Screen name="+not-found" options={{ title: 'Nicht gefunden' }} />
      </Stack>
      <StatusBar style={effectiveTheme === 'dark' ? 'light' : 'dark'} />
      <Toaster
        theme={effectiveTheme}
        toastOptions={{
          style: { backgroundColor: 'transparent' },
        }}
      />
      <PortalHost />
    </NavigationThemeProvider>
  );
}

/**
 * Root layout of the application.
 *
 * Wraps the entire app stack in the {@link ThemeProvider} and shows a loading
 * splash while the auth state is being hydrated.
 *
 * @returns The root layout, or a loading splash during hydration.
 */
export default function RootLayout() {
  const { isLoading } = useAuth();

  // Auth bootstrap: simple splash block. TODO FE-S1 (#206): real JWT restore
  // via /student/dashboard/profile will set isLoading to true initially.
  if (isLoading) {
    return (
      <View className="flex-1 items-center justify-center bg-background">
        <Text className="text-foreground">Lade…</Text>
      </View>
    );
  }

  return (
    <ThemeProvider>
      <ThemedStack />
    </ThemeProvider>
  );
}
