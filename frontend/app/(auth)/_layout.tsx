import { Stack, useRouter } from 'expo-router';
import { useEffect } from 'react';

import { useAuth } from '@/lib/auth/__stub';

/**
 * Layout wrapper for the auth group `(auth)`.
 *
 * Automatically redirects already-authenticated users to the dashboard
 * (anti-auth guard). Prevents logged-in users from seeing the login or
 * register screens.
 *
 * @returns The auth group stack without a header.
 */
export default function AuthGroupLayout() {
  const { isLoading, user } = useAuth();
  const router = useRouter();

  // Anti-auth guard: authenticated users are sent directly to the app area.
  // TODO FE-S1 (#206): real AuthContext replaces __stub.
  useEffect(() => {
    if (!isLoading && user) {
      router.replace('/(app)/dashboard');
    }
  }, [isLoading, user, router]);

  return <Stack screenOptions={{ headerShown: false }} />;
}
