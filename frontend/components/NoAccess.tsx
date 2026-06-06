import { Link } from 'expo-router';
import { Text, View } from 'react-native';

type Props = {
  /** Error or hint message shown to the user. */
  message: string;
  /** Label of the optional call-to-action link. */
  ctaLabel?: string;
  /** Expo Router path for the optional call-to-action link. */
  ctaTo?: string;
};

/**
 * Placeholder screen for unauthorized access attempts.
 *
 * Displays an error message and an optional redirect link, e.g. back to the
 * login screen or the dashboard.
 *
 * @param message - The hint text to display.
 * @param ctaLabel - Optional label for the redirect link.
 * @param ctaTo - Optional Expo Router path for the redirect link.
 * @returns The NoAccess component.
 */
export function NoAccess({ message, ctaLabel, ctaTo }: Props) {
  return (
    <View className="flex-1 items-center justify-center bg-background p-6">
      <Text className="mb-3 text-center text-base text-foreground">{message}</Text>
      {ctaLabel && ctaTo ? (
        <Link href={ctaTo as never}>
          <Text className="text-sm text-primary">{ctaLabel}</Text>
        </Link>
      ) : null}
    </View>
  );
}
