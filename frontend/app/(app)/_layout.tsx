import { Stack, useRouter } from 'expo-router';
import { useEffect } from 'react';

import { AppLayout } from '@/components/layout/AppLayout';
import { useAuth } from '@/lib/auth/__stub';

/**
 * Layout wrapper for the authenticated app group `(app)`.
 *
 * Automatically redirects unauthenticated users to the login screen
 * (protected-route guard). Wraps all screens in {@link AppLayout}.
 *
 * @returns The app group layout with auth guard and navigation stack.
 */
export default function AppGroupLayout() {
  const { isLoading, user } = useAuth();
  const router = useRouter();

  // Protected route: unauthenticated users are sent to login.
  // TODO FE-S1 (#206): real AuthContext replaces __stub.
  // Note: __stub returns a stub user by default so no redirect loop occurs.
  // Set EXPO_PUBLIC_FORCE_NO_AUTH=true to test the user=null branch.
  useEffect(() => {
    if (!isLoading && !user) {
      router.replace('/(auth)/login');
    }
  }, [isLoading, user, router]);

  return (
    <AppLayout>
      <Stack screenOptions={{ headerShown: false }} />
    </AppLayout>
  );
}
