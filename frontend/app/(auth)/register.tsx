import { Link } from 'expo-router';
import { Text, View } from 'react-native';

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
    <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center', padding: 24 }}>
      <Text style={{ fontSize: 18, marginBottom: 16, color: '#000' }}>
        Register (TODO: FE-Auth)
      </Text>
      <Link href="/(auth)/login">
        <Text style={{ fontSize: 14, color: '#0066cc' }}>Zum Login</Text>
      </Link>
    </View>
  );
}
