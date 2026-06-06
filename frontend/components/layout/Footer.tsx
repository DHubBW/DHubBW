import { Link } from 'expo-router';
import { Platform, Pressable, Text, View } from 'react-native';

import { useCookieConsent } from '@/lib/consent/useCookieConsent';

/**
 * Application footer shown on every screen.
 *
 * Contains:
 * - Links to the static legal pages (Impressum, Datenschutz)
 * - "Cookie-Einstellungen" button (Web only) to re-open the consent banner
 *
 * @returns The footer bar.
 */
export function Footer() {
  const { resetConsent } = useCookieConsent();

  return (
    <View className="flex-row flex-wrap items-center justify-center gap-x-4 gap-y-1 border-t border-border bg-background px-4 py-2">
      <Link href={'/(legal)/impressum' as never} asChild>
        <Text accessibilityRole="link" className="text-xs text-muted-foreground underline">
          Impressum
        </Text>
      </Link>
      <Separator />
      <Link href={'/(legal)/datenschutz' as never} asChild>
        <Text accessibilityRole="link" className="text-xs text-muted-foreground underline">
          Datenschutz
        </Text>
      </Link>
      {Platform.OS === 'web' && (
        <>
          <Separator />
          <Pressable
            accessibilityRole="button"
            onPress={resetConsent}
          >
            <Text className="text-xs text-muted-foreground underline">
              Cookie-Einstellungen
            </Text>
          </Pressable>
        </>
      )}
    </View>
  );
}

function Separator() {
  return <Text className="text-xs text-muted-foreground">·</Text>;
}
