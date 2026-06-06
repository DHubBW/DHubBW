/**
 * Route guard for routes that require an authenticated session.
 *
 * Renders the children when {@link useAuth} reports a logged-in user; while
 * the initial profile-restore is pending it shows a small splash. When the
 * user is confirmed `null`, it returns an `expo-router` `<Redirect>` to the
 * login screen carrying the current route as `?returnTo=…`. The login flow
 * (FE-Auth #212) is expected to honour that param after a successful login.
 *
 * **Why `<Redirect>` and not `useEffect`+`router.replace`:** the imperative
 * variant produces a render of the protected children while React schedules
 * the replace, plus a "second render" once the navigation actually happens.
 * In React 19 that triggers the "cannot update during render" warning and is
 * race-prone (Sub-A code-review finding). `<Redirect>` is declarative and
 * runs **before** any subtree mounts.
 */
import { Redirect, usePathname } from 'expo-router';
import type { ReactNode } from 'react';
import { Text, View } from 'react-native';

import { useAuth } from '@/lib/auth/AuthContext';

/**
 * Returns a `returnTo`-safe representation of the current route. Falls back
 * to `/(app)/dashboard` if expo-router has no pathname yet (very early
 * mount).
 */
function useReturnTo(): string {
  const pathname = usePathname();
  if (pathname && pathname.length > 0) return pathname;
  return '/(app)/dashboard';
}

/**
 * Renders a generic splash view while the auth bootstrap is pending. Same
 * look-and-feel as the splash in `app/_layout.tsx` so transitions don't
 * flicker.
 */
function AuthSplash() {
  return (
    <View className="flex-1 items-center justify-center bg-background">
      <Text className="text-foreground">Lade…</Text>
    </View>
  );
}

/**
 * Renders `children` only when an authenticated user is present.
 *
 * - `isLoading` → splash.
 * - `user === null` → `<Redirect href="/(auth)/login?returnTo=<currentRoute>" />`.
 * - otherwise → children.
 */
export function ProtectedRoute({ children }: { children: ReactNode }) {
  const { user, isLoading } = useAuth();
  const returnTo = useReturnTo();

  if (isLoading) return <AuthSplash />;
  if (user === null) {
    const href = `/(auth)/login?returnTo=${encodeURIComponent(returnTo)}`;
    return <Redirect href={href as never} />;
  }
  return <>{children}</>;
}
