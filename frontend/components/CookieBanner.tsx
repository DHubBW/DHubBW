import { Link } from 'expo-router';
import { Platform, Pressable, Text, View } from 'react-native';

import { useCookieConsent } from '@/lib/consent/useCookieConsent';

export type CookieBannerProps = {
  /**
   * Optional override for the reject action.
   *
   * When provided it runs **instead of** the default `reject()` call from
   * the hook. Use this on the login screen to add navigation side effects.
   * When omitted, the banner simply calls `reject()`.
   */
  onRejectOverride?: () => void;
};

/**
 * DSGVO / TTDSG compliant cookie-consent banner.
 *
 * Self-contained: reads and writes consent state via {@link useCookieConsent}
 * internally. Only rendered on Web and only when consent is `'unknown'`.
 *
 * @param props - {@link CookieBannerProps}
 * @returns The banner or `null` when no action is required.
 */
export function CookieBanner({ onRejectOverride }: CookieBannerProps) {
  const { consent, accept, reject } = useCookieConsent();

  if (Platform.OS !== 'web') return null;
  if (consent !== 'unknown') return null;

  function handleReject() {
    if (onRejectOverride) {
      onRejectOverride();
    } else {
      reject();
    }
  }

  return (
    <View className="border-t border-border bg-card px-4 py-4">
      <Text className="mb-1 text-sm font-semibold text-card-foreground">
        Cookie-Hinweis
      </Text>
      <Text className="mb-3 text-xs text-muted-foreground">
        DHubBW verwendet ein technisch notwendiges Session-Cookie (`jwt`), das
        für den Login erforderlich ist. Ohne dieses Cookie ist eine Anmeldung
        nicht möglich.{' '}
        <Link href={'/(legal)/datenschutz' as never}>
          <Text className="text-xs text-primary underline">
            Datenschutzerklärung
          </Text>
        </Link>
      </Text>
      <View className="flex-row gap-2">
        <Pressable
          accessibilityRole="button"
          onPress={handleReject}
          className="rounded-md border border-border px-4 py-2"
        >
          <Text className="text-xs text-muted-foreground">Ablehnen</Text>
        </Pressable>
        <Pressable
          accessibilityRole="button"
          onPress={accept}
          className="rounded-md bg-primary px-4 py-2"
        >
          <Text className="text-xs font-medium text-primary-foreground">
            Akzeptieren
          </Text>
        </Pressable>
      </View>
    </View>
  );
}
