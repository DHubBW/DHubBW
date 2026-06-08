import { Image } from "expo-image";
import { Moon, Settings, Sun, SunMoon } from "lucide-react-native";
import { useState } from "react";
import { Pressable, View } from "react-native";

import { SettingsModal } from "@/components/SettingsModal";
import { Icon } from "@/components/ui/icon";
import { useTheme, type ThemePreference } from "@/lib/theme/ThemeProvider";

const LOGO = require("@/assets/images/icon.png") as number;

const THEME_ICONS: Record<ThemePreference, typeof Sun> = {
  light: Sun,
  dark: Moon,
  system: SunMoon,
};

const NEXT_THEME: Record<ThemePreference, ThemePreference> = {
  light: "dark",
  dark: "system",
  system: "light",
};

/**
 * Mobile app header bar at the top of the screen.
 *
 * Shows the DHubBW logo mark + wordmark, a theme-cycle button and a settings
 * button that opens {@link SettingsModal}. Mounted by {@link AppLayout} below
 * 768 px.
 *
 * @returns The header bar.
 */
export function Header() {
  const { theme, setTheme } = useTheme();
  const [settingsOpen, setSettingsOpen] = useState(false);

  const ThemeIcon = THEME_ICONS[theme];

  return (
    <View className="w-full flex-row items-center border-b border-sidebar-border bg-sidebar px-4 py-2.5">
      {/* Logo */}
      <Image
        source={LOGO}
        style={{ width: 160, height: 50 }}
        contentFit="contain"
        contentPosition="left"
      />

      {/* Actions pushed to the right via ml-auto */}
      <View className="ml-auto flex-row items-center gap-1">
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="Design wechseln"
          onPress={() => setTheme(NEXT_THEME[theme])}
          className="rounded-md p-2 active:bg-sidebar-accent/60"
          hitSlop={8}
        >
          <Icon as={ThemeIcon} size={20} className="text-sidebar-foreground" />
        </Pressable>

        <Pressable
          testID="header-settings-button"
          accessibilityRole="button"
          accessibilityLabel="Einstellungen öffnen"
          onPress={() => setSettingsOpen(true)}
          className="rounded-md p-2 active:bg-sidebar-accent/60"
          hitSlop={8}
        >
          <Icon as={Settings} size={20} className="text-sidebar-foreground" />
        </Pressable>
      </View>

      <SettingsModal open={settingsOpen} onOpenChange={setSettingsOpen} />
    </View>
  );
}
