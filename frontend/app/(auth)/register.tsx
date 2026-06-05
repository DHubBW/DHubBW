import { Link } from 'expo-router';
import { Text, View } from 'react-native';

// TODO FE-Auth (#206 / FE-S1): echter Register-Screen mit Form, Validierung,
// API-Call POST /auth/register. Aktuell nur Stub-Platzhalter.
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
