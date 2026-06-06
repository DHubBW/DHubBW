import { Redirect, Stack } from 'expo-router';
import { Text, View } from 'react-native';

import { useAuth } from '@/lib/auth/AuthContext';

/**
 * Layout wrapper for the auth group `(auth)`.
 *
 * Anti-auth guard: a logged-in user lands on `/(app)/dashboard` instead of
 * the login/register screens. Implemented declaratively via expo-router
 * `<Redirect>` so it cannot race with the screen render — same pattern as
 * the protected-route guards in {@link ProtectedRoute}.
 *
 * @returns The auth group stack without a header (or a redirect when
 *   already authenticated).
 */
export default function AuthGroupLayout() {
  const { isLoading, user } = useAuth();

  if (isLoading) {
    return (
      <View className="flex-1 items-center justify-center bg-background">
        <Text className="text-foreground">Lade…</Text>
      </View>
    );
  }
  if (user !== null) {
    return <Redirect href={'/(app)/dashboard' as never} />;
  }

  return <Stack screenOptions={{ headerShown: false }} />;
}
