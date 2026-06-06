import { Redirect } from 'expo-router';

/**
 * Root route – redirects directly to the dashboard.
 *
 * The protected-route guard in `(app)/_layout.tsx` handles the redirect to
 * `/(auth)/login` when the user is not authenticated.
 * After FE-S1 (#206) this file will decide auth-dependently between
 * `/(app)/dashboard` and `/(auth)/login`.
 *
 * @returns A `<Redirect>` to the dashboard.
 */
export default function Index() {
  return <Redirect href="/(app)/dashboard" />;
}
