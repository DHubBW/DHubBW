import { Link } from 'expo-router';
import { Text, View } from 'react-native';

/**
 * Fallback screen for unknown routes.
 *
 * Rendered by `app/+not-found.tsx` and shows an error message with a link
 * back to the dashboard.
 *
 * @returns The 404 error screen.
 */
export function NotFoundScreen() {
  return (
    <View className="flex-1 items-center justify-center bg-background p-6">
      <Text className="mb-3 text-lg font-semibold text-foreground">Seite nicht gefunden</Text>
      <Link href="/(app)/dashboard">
        <Text className="text-sm text-primary">Zurück zum Dashboard</Text>
      </Link>
    </View>
  );
}
