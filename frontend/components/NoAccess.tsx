import { Link } from 'expo-router';
import { Text, View } from 'react-native';

type Props = {
  message: string;
  ctaLabel?: string;
  ctaTo?: string;
};

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
