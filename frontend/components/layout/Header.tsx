import { Ionicons } from '@expo/vector-icons';
import { Pressable, Text, View } from 'react-native';

import { useTheme } from '@/lib/theme/ThemeProvider';

/**
 * Mobile app header bar at the top of the screen.
 *
 * Shows the app name and a settings button. The button is currently a
 * placeholder — implementation is tracked in the FE-Settings issue.
 *
 * @returns The header bar with the app logo and settings icon.
 */
export function Header() {
  const { effectiveTheme } = useTheme();
  // Ionicons does not accept a className prop, so the color is derived from
  // the theme (foreground-light = #1D2939, foreground-dark = #F2F2F5).
  const iconColor = effectiveTheme === 'dark' ? '#F2F2F5' : '#1D2939';

  return (
    <View className="flex-row items-center justify-between border-b border-sidebar-border bg-sidebar px-4 py-3">
      <Text className="text-lg font-semibold text-sidebar-foreground">DHubBW</Text>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel="Einstellungen öffnen"
        onPress={() => {
          // TODO FE-Settings: opens <SettingsModal> from the FE-Settings issue.
          console.log('TODO: open SettingsModal');
        }}
        hitSlop={8}
      >
        <Ionicons name="settings-outline" size={22} color={iconColor} />
      </Pressable>
    </View>
  );
}
