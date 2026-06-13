import { Redirect } from 'expo-router';

/**
 * Root route — redirects to the dashboard.
 *
 * The {@link ProtectedRoute} guard in `(app)/_layout.tsx` decides
 * auth-dependently between rendering the dashboard and redirecting to
 * `/(auth)/login?returnTo=/(app)/dashboard`, so this entry point only has
 * to forward into the `(app)` group.
 *
 * @returns A `<Redirect>` to the dashboard.
 */
export default function Index() {
  return <Redirect href="/(app)/dashboard" />;
}
