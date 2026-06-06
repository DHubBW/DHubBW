import { Stack } from 'expo-router';

import { ProtectedRoute } from '@/components/auth/ProtectedRoute';
import { AppLayout } from '@/components/layout/AppLayout';

/**
 * Layout wrapper for the authenticated app group `(app)`.
 *
 * Wraps the entire `(app)` subtree in {@link ProtectedRoute}: if the user is
 * not logged in the guard redirects to `/(auth)/login?returnTo=<route>`
 * declaratively (via expo-router `<Redirect>`), avoiding the
 * `useEffect`+`router.replace` race the FE-S2 stub had.
 *
 * @returns The app group layout with auth guard and navigation stack.
 */
export default function AppGroupLayout() {
  return (
    <ProtectedRoute>
      <AppLayout>
        <Stack screenOptions={{ headerShown: false }} />
      </AppLayout>
    </ProtectedRoute>
  );
}
