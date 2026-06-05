import { Ionicons } from '@expo/vector-icons';
import { Pressable, Text, View } from 'react-native';

import { useTheme } from '@/lib/theme/ThemeProvider';

export function Header() {
  const { effectiveTheme } = useTheme();
  // Ionicons akzeptiert kein className-Prop, daher Farbe via Theme abgeleitet
  // (foreground-light = #1D2939, foreground-dark = #F2F2F5).
  const iconColor = effectiveTheme === 'dark' ? '#F2F2F5' : '#1D2939';

  return (
    <View className="flex-row items-center justify-between border-b border-sidebar-border bg-sidebar px-4 py-3">
      <Text className="text-lg font-semibold text-sidebar-foreground">DHubBW</Text>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel="Einstellungen öffnen"
        onPress={() => {
          // TODO FE-Settings: öffnet <SettingsModal> aus FE-Settings-Issue.
          console.log('TODO: open SettingsModal');
        }}
        hitSlop={8}
      >
        <Ionicons name="settings-outline" size={22} color={iconColor} />
      </Pressable>
    </View>
  );
}
