import "../global.css";

import { ThemeProvider as NavigationThemeProvider } from "@react-navigation/native";
import { PortalHost } from "@rn-primitives/portal";
import { QueryClientProvider } from "@tanstack/react-query";
import { Stack } from "expo-router";
import { StatusBar } from "expo-status-bar";
import * as React from "react";
import { Text, View, Platform, useWindowDimensions } from "react-native";
import "react-native-reanimated";
import { GestureHandlerRootView } from "react-native-gesture-handler";
import { SafeAreaProvider } from "react-native-safe-area-context";
import { Toaster } from "sonner-native";

import { queryClient } from "@/lib/api/queryClient";
import { AuthProvider, useAuth } from "@/lib/auth/AuthContext";
import { NAV_THEME } from "@/lib/theme/nav-theme";
import { ThemeProvider, useTheme } from "@/lib/theme/ThemeProvider";
import { useGlobalQueryErrorToast } from "@/hooks/useGlobalQueryErrorToast";

/**
 * Internal component that keeps the React Navigation theme provider in sync
 * with the active NativeWind theme.
 *
 * @returns The theme-synchronized navigation stack including StatusBar,
 *   Toaster and PortalHost.
 */
function ThemedStack({ isLoading }: { isLoading: boolean }) {
  const { effectiveTheme } = useTheme();
  const { width } = useWindowDimensions();
  const isMobile = width < 768;

  React.useEffect(() => {
    if (Platform.OS === "web" && typeof document !== "undefined") {
      document.title = "DHubBW";
    }
  }, []);

  if (isLoading) {
    return (
      <View className="flex-1 items-center justify-center bg-background">
        <Text className="text-foreground">Lade…</Text>
      </View>
    );
  }

  return (
    <NavigationThemeProvider value={NAV_THEME[effectiveTheme]}>
      <Stack>
        <Stack.Screen name="(auth)" options={{ headerShown: false }} />
        <Stack.Screen name="(app)" options={{ headerShown: false }} />
        <Stack.Screen name="(legal)" options={{ headerShown: false }} />
        <Stack.Screen name="+not-found" options={{ title: "Nicht gefunden" }} />
      </Stack>
      <StatusBar style={effectiveTheme === "dark" ? "light" : "dark"} />
      {/*
       * PortalHost MUST mount BEFORE the Toaster so the Toaster's View
       * is the very last sibling in the tree. RN-Web layers Views in
       * paint order — the toast's `position: fixed` overlay paints
       * above any radix-portal dialog overlay only when its DOM node
       * comes after the dialog's portal target. The explicit
       * z-index: 9999 below is belt-and-braces for the rare case where
       * a Stack.Screen mounts another portal that re-orders things.
       */}
      <PortalHost />
      <Toaster
        theme={effectiveTheme}
        position="bottom-center"
        offset={24}
        positionerStyle={
          isMobile
            ? {
                width: "100%",
                paddingHorizontal: 0,
                alignItems: "stretch",
                // On web `sonner-native` defaults to `position: absolute`,
                // which paints the toast relative to its React parent and
                // therefore BENEATH any radix-portal dialog overlay
                // (mounted at document.body end with `position: fixed`).
                // Override to `fixed` + a very high z-index so toasts
                // always layer above the dialog backdrop.
                ...(Platform.OS === "web"
                  ? ({ position: "fixed" } as object)
                  : {}),
                zIndex: 2147483647,
                elevation: 9999,
              }
            : {
                alignItems: "flex-end",
                right: 0,
                left: undefined,
                width: "auto",
                paddingRight: 20,
                ...(Platform.OS === "web"
                  ? ({ position: "fixed" } as object)
                  : {}),
                zIndex: 2147483647,
                elevation: 9999,
              }
        }
        toastOptions={{
          style: {
            backgroundColor: effectiveTheme === "dark" ? "#1e1e24" : "#ffffff",
            borderWidth: 1,
            borderColor: effectiveTheme === "dark" ? "#2d2d36" : "#e5e7eb",
            borderRadius: isMobile ? 0 : 8,
            shadowColor: "#000",
            shadowOpacity: 0.12,
            shadowRadius: 8,
            shadowOffset: { width: 0, height: 2 },
            ...(isMobile
              ? { width: "100%", minWidth: undefined, maxWidth: undefined }
              : { minWidth: 220, maxWidth: 320 }),
          },
        }}
      />
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
  useGlobalQueryErrorToast();

  return (
    <ThemeProvider>
      <ThemedStack isLoading={isLoading} />
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
    <GestureHandlerRootView style={{ flex: 1 }}>
      <SafeAreaProvider>
        <QueryClientProvider client={queryClient}>
          <AuthProvider>
            <AuthGate />
          </AuthProvider>
        </QueryClientProvider>
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}
