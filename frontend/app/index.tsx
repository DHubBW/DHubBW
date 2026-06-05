import { Redirect } from 'expo-router';

// Root-Route → Auth-Stub liefert Default-User, also ist der natürliche Einstieg
// das Dashboard. ProtectedRoute in (app)/_layout.tsx redirected user==null
// weiter zu /(auth)/login.
// TODO FE-S1 (#206): mit echtem AuthContext entscheidet diese Datei
// auth-abhängig zwischen /(app)/dashboard und /(auth)/login.
export default function Index() {
  return <Redirect href="/(app)/dashboard" />;
}
