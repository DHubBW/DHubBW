import { Link } from 'expo-router';
import { Text, View } from 'react-native';

/**
 * Login screen (placeholder).
 *
 * Will be replaced by issue FE-Auth (#206 / FE-S1) with a real form,
 * validation and a `POST /auth/login` API call.
 *
 * @returns The login screen.
 */
// TODO FE-Auth (#206 / FE-S1): real login screen with form, validation,
// API call POST /auth/login. Currently a stub placeholder only.
export default function LoginScreen() {
  return (
    <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center', padding: 24 }}>
      <Text style={{ fontSize: 18, marginBottom: 16, color: '#000' }}>
        Login (TODO: FE-Auth)
      </Text>
      <Link href="/(auth)/register">
        <Text style={{ fontSize: 14, color: '#0066cc' }}>Zur Registrierung</Text>
      </Link>
    </View>
  );
}
