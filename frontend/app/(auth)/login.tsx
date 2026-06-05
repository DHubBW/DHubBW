import { Link } from 'expo-router';
import { Text, View } from 'react-native';

// TODO FE-Auth (#206 / FE-S1): echter Login-Screen mit Form, Validierung,
// API-Call POST /auth/login. Aktuell nur Stub-Platzhalter.
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
