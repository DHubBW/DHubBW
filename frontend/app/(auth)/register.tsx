import { Link } from 'expo-router';
import { Text, View } from 'react-native';

import { Footer } from '@/components/layout/Footer';

/**
 * Registration screen (placeholder).
 *
 * Will be replaced by issue FE-Auth (#206 / FE-S1) with a real form,
 * validation and a `POST /auth/register` API call.
 *
 * @returns The registration screen.
 */
// TODO FE-Auth (#206 / FE-S1): real register screen with form, validation,
// API call POST /auth/register. Currently a stub placeholder only.
export default function RegisterScreen() {
  return (
    <View className="flex-1 bg-background">
      <View className="flex-1 items-center justify-center px-6">
        <Text className="mb-4 text-lg font-semibold text-foreground">
          Register (TODO: FE-Auth)
        </Text>
        <Link href="/(auth)/login">
          <Text className="text-sm text-primary">Zum Login</Text>
        </Link>
      </View>
      <Footer />
    </View>
  );
}
