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

export default function RootLayout() {
  const { isLoading } = useAuth();

  // AuthBootstrap: einfacher Splash-Block. TODO FE-S1 (#206): echter
  // JWT-Restore via /student/dashboard/profile setzt isLoading initial true.
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
