import { Redirect, Stack, useGlobalSearchParams, usePathname } from 'expo-router';
import { Text, View } from 'react-native';

import { useAuth } from '@/lib/auth/AuthContext';
import { resolveReturnTo } from '@/lib/auth/resolveReturnTo';

/**
 * Layout wrapper for the auth group `(auth)`.
 *
 * Anti-auth guard: a logged-in user lands on the post-login target instead
 * of the login/register screens. Implemented declaratively via expo-router
 * `<Redirect>` so it cannot race with the screen render — same pattern as
 * the protected-route guards in {@link ProtectedRoute}.
 *
 * Post-login target is resolved from `?returnTo=` directly inside the
 * layout via {@link resolveReturnTo}: this keeps the login screen free of
 * its own `useEffect`-driven redirect (which would race the layout's
 * declarative redirect on slow renders) and ensures the open-redirect
 * whitelist runs on every authenticated visit to the auth group.
 *
 * The hook is `useGlobalSearchParams` rather than `useLocalSearchParams`
 * because the latter, in a layout context, returns the layout route's own
 * params and is empty for the focused child screen on Web — leaving the
 * fallback path the only reachable target. Global params reflect the
 * currently visible URL, which is exactly what the whitelist needs.
 *
 * Exception: the registration screen owns a multi-step flow whose Step 2
 * (course selection) requires the user to already be authenticated — the
 * `register()` call in Step 1 establishes the session and the screen then
 * advances to Step 2 locally. Redirecting away the moment `user !== null`
 * would tear Step 2 down before it ever renders, so the guard skips the
 * redirect on `/register`. The screen's own logic still routes to the
 * dashboard on Step-2 success or skip.
 *
 * @returns The auth group stack without a header (or a redirect when
 *   already authenticated, except on `/register`).
 */
export default function AuthGroupLayout() {
  const { isLoading, user } = useAuth();
  const pathname = usePathname();
  const params = useGlobalSearchParams<{ returnTo?: string | string[] }>();

  if (isLoading) {
    return (
      <View className="flex-1 items-center justify-center bg-background">
        <Text className="text-foreground">Lade…</Text>
      </View>
    );
  }
  // `pathname` is normalised by expo-router and does not include the group
  // segment, so `/register` matches the route file `app/(auth)/register.tsx`.
  const isRegisterRoute = pathname === '/register';
  if (user !== null && !isRegisterRoute) {
    return <Redirect href={resolveReturnTo(params) as never} />;
  }

  return <Stack screenOptions={{ headerShown: false }} />;
}
