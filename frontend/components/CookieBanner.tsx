import { Link } from "expo-router";
import { ShieldCheck, X } from "lucide-react-native";
import { Modal, Platform, Pressable, View } from "react-native";

import { Icon } from "@/components/ui/icon";
import { Text } from "@/components/ui/text";
import { useCookieConsent } from "@/lib/consent/useCookieConsent";

export type CookieBannerProps = {
  /**
   * Called when the user clicks "Ablehnen" instead of the default `reject()`.
   * Use this on the login screen to add navigation side-effects.
   */
  onRejectOverride?: () => void;
};

/**
 * DSGVO / TTDSG compliant cookie-consent banner.
 *
 * Renders as a full-screen blocking overlay (via RN `Modal` on Native,
 * fixed overlay on Web) so the user cannot interact with any content before
 * making a decision. Only shown on Web and only when `consent === 'unknown'`.
 *
 * @param props - {@link CookieBannerProps}
 * @returns The overlay or `null` when no action is required.
 */
export function CookieBanner({ onRejectOverride }: CookieBannerProps) {
  const { consent, accept, reject } = useCookieConsent();

  if (Platform.OS !== "web") return null;
  if (consent !== "unknown") return null;

  function handleReject() {
    if (onRejectOverride) {
      onRejectOverride();
    } else {
      reject();
    }
  }

  return (
    // Fixed overlay that covers the entire viewport and blocks all interaction.
    // `pointer-events: all` on the overlay + `inset-0` ensures nothing below
    // is reachable via click or keyboard while consent is pending.
    <View
      className="absolute bottom-0 left-0 right-0 top-0 items-end justify-end bg-black/40"
      // On RN Web `pointerEvents` must be set as a prop, not className.
      pointerEvents="box-none"
      style={{ position: "fixed" as never, zIndex: 9999 }}
    >
      {/* Card */}
      <View
        pointerEvents="auto"
        className="m-4 w-full max-w-md rounded-2xl border border-border bg-card p-5 shadow-xl shadow-black/20"
      >
        {/* Header row */}
        <View className="mb-3 flex-row items-center gap-3">
          <View className="size-9 items-center justify-center rounded-full bg-primary/10">
            <Icon as={ShieldCheck} size={18} className="text-primary" />
          </View>
          <Text className="flex-1 text-base font-semibold text-card-foreground">
            Cookie-Hinweis
          </Text>
        </View>

        {/* Body */}
        <Text className="mb-1 text-sm leading-relaxed text-muted-foreground">
          DHubBW verwendet ein technisch notwendiges Session-Cookie (
          <Text className="font-medium text-foreground">jwt</Text>
          ), das für den Login erforderlich ist. Ohne dieses Cookie ist eine
          Anmeldung nicht möglich.
        </Text>
        <Link href={"/(legal)/datenschutz" as never} asChild>
          <Text
            accessibilityRole="link"
            className="mb-4 text-xs text-primary underline"
          >
            Datenschutzerklärung lesen
          </Text>
        </Link>

        {/* Actions */}
        <View className="flex-row gap-2">
          <Pressable
            accessibilityRole="button"
            onPress={handleReject}
            className="flex-1 flex-row items-center justify-center gap-1.5 rounded-xl border border-border bg-background px-4 py-2.5 active:opacity-70"
          >
            <Icon as={X} size={14} className="text-muted-foreground" />
            <Text className="text-sm font-medium text-muted-foreground">
              Ablehnen
            </Text>
          </Pressable>
          <Pressable
            accessibilityRole="button"
            onPress={accept}
            className="flex-1 flex-row items-center justify-center gap-1.5 rounded-xl bg-primary px-4 py-2.5 active:opacity-80"
          >
            <Icon
              as={ShieldCheck}
              size={14}
              className="text-primary-foreground"
            />
            <Text className="text-sm font-medium text-primary-foreground">
              Akzeptieren
            </Text>
          </Pressable>
        </View>
      </View>
    </View>
  );
}
