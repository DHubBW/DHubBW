import "../global.css";

import { ThemeProvider as NavigationThemeProvider } from "@react-navigation/native";
import { PortalHost } from "@rn-primitives/portal";
import { QueryClientProvider } from "@tanstack/react-query";
import { Stack } from "expo-router";
import { StatusBar } from "expo-status-bar";
import { Text, View } from "react-native";
import "react-native-reanimated";
import { Toaster } from "sonner-native";

import { queryClient } from "@/lib/api/queryClient";
import { AuthProvider, useAuth } from "@/lib/auth/AuthContext";
import { NAV_THEME } from "@/lib/theme/nav-theme";
import { ThemeProvider, useTheme } from "@/lib/theme/ThemeProvider";

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
        <Stack.Screen name="(legal)" options={{ headerShown: false }} />
        <Stack.Screen name="+not-found" options={{ title: "Nicht gefunden" }} />
      </Stack>
      <StatusBar style={effectiveTheme === "dark" ? "light" : "dark"} />
      <Toaster
        theme={effectiveTheme}
        position="bottom-right"
        toastOptions={{
          style: {
            backgroundColor: effectiveTheme === "dark" ? "#1e1e24" : "#ffffff",
            borderWidth: 1,
            borderColor: effectiveTheme === "dark" ? "#2d2d36" : "#e5e7eb",
            borderRadius: 8,
            shadowColor: "#000",
            shadowOpacity: 0.12,
            shadowRadius: 8,
            shadowOffset: { width: 0, height: 2 },
          },
        }}
      />
      <PortalHost />
    </NavigationThemeProvider>
  );
}

/**
 * Splash- vs. App-Subtree. `useAuth()` MUST live here (inside the
 * AuthProvider scope), NOT in `RootLayout` — otherwise the hook could not
 * access the AuthContext.
 *
 * Auth bootstrap: while the initial profile-restore call is in flight,
 * `useAuth().isLoading` is `true` and we render the splash. Once the call
 * resolves the themed app stack mounts.
 *
 * @returns The themed app stack, or a loading splash during auth hydration.
 */
function AuthGate() {
  const { isLoading } = useAuth();

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

/**
 * Root layout of the application.
 *
 * Provides the flat root provider pyramid in fixed order:
 * - `QueryClientProvider` outside (queryClient survives auth state changes).
 * - `AuthProvider` inside (FE-S1 Sub-B): owns user state, drives the global
 *   401-handler subscription, and exposes `useAuth()` to the rest of the app.
 * - `AuthGate` innermost: uses `useAuth()` and decides splash vs. app.
 *
 * @returns The root provider tree wrapping {@link AuthGate}.
 */
export default function RootLayout() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <AuthGate />
      </AuthProvider>
    </QueryClientProvider>
  );
}
