import { Link } from 'expo-router';
import { Text, View } from 'react-native';

import { CookieBanner } from '@/components/CookieBanner';
import { Footer } from '@/components/layout/Footer';
import { useCookieConsent } from '@/lib/consent/useCookieConsent';

/**
 * Login screen (placeholder).
 *
 * Will be replaced by issue FE-Auth (#206 / FE-S1) with a real form,
 * validation and a `POST /auth/login` API call.
 *
 * Shows the DSGVO cookie-consent banner on Web and a Footer with
 * Impressum / Datenschutz / Cookie-Einstellungen links on all platforms.
 *
 * @returns The login screen.
 */
// TODO FE-Auth (#206 / FE-S1): real login screen with form, validation,
// API call POST /auth/login. Currently a stub placeholder only.
export default function LoginScreen() {
  const { consent, reject } = useCookieConsent();

  function handleReject() {
    reject();
  }

  return (
    <View className="flex-1 bg-background">
      <View className="flex-1 items-center justify-center px-6">
        {consent === 'rejected' && (
          <View className="mb-4 rounded-lg bg-destructive/10 px-4 py-3">
            <Text className="text-sm text-destructive">
              {'Cookies sind erforderlich, um sich anzumelden. Bitte akzeptiere die Cookies über „Cookie-Einstellungen" im Footer, um fortzufahren.'}
            </Text>
          </View>
        )}
        <Text className="mb-4 text-lg font-semibold text-foreground">
          Login (TODO: FE-Auth)
        </Text>
        <Link href="/(auth)/register">
          <Text className="text-sm text-primary">Zur Registrierung</Text>
        </Link>
      </View>
      <CookieBanner onRejectOverride={handleReject} />
      <Footer />
    </View>
  );
}
