import { Stack } from 'expo-router';

/**
 * Layout wrapper for the legal route group `(legal)`.
 *
 * Shows a back-navigation header so the user can always return to wherever
 * they came from. Legal pages must be publicly accessible — no auth guard.
 *
 * @returns The stack navigator for the legal screens.
 */
export default function LegalGroupLayout() {
  return (
    <Stack
      screenOptions={{
        headerShown: true,
        headerBackTitle: 'Zurück',
      }}
    />
  );
}
