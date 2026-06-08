import { Link, usePathname } from "expo-router";
import { Image } from "expo-image";
import { Moon, Settings, Sun, SunMoon } from "lucide-react-native";
import { useState } from "react";
import { Pressable, Text, View } from "react-native";

import { SettingsModal } from "@/components/SettingsModal";
import { Icon } from "@/components/ui/icon";
import { ITEMS, visible } from "@/components/layout/navItems";
import { useAuth } from "@/lib/auth/AuthContext";
import { useTheme, type ThemePreference } from "@/lib/theme/ThemeProvider";
import { cn } from "@/lib/utils";

const LOGO = require("@/assets/images/icon.png") as number;

/**
 * Returns up to two uppercase initials for a display name.
 *
 * @param name - Full display name (e.g. "Anna Schmidt").
 * @returns Two-letter initials string (e.g. "AS").
 */
function initials(name: string): string {
  const parts = name.trim().split(/\s+/);
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

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
 * Desktop sidebar navigation.
 *
 * Shows the DHBW logo, icon + label nav entries with an active highlight, a
 * theme-cycle button and a user block at the bottom that opens
 * {@link SettingsModal}. Mounted by {@link AppLayout} at ≥768 px.
 *
 * @returns The full-height sidebar.
 */
export function Sidebar() {
  const { user } = useAuth();
  const { theme, setTheme } = useTheme();
  const pathname = usePathname();
  const [settingsOpen, setSettingsOpen] = useState(false);

  const items = ITEMS.filter((it) => visible(it, user));
  const ThemeIcon = THEME_ICONS[theme];

  return (
    <View className="w-[220px] flex-col border-r border-sidebar-border bg-sidebar px-3 py-4">
      {/* Logo */}
      <View className="mb-6 items-center px-2">
        <Image
          source={LOGO}
          style={{ width: 160, height: 50 }}
          contentFit="contain"
        />
      </View>

      {/* Nav items */}
      <View className="flex-1 gap-0.5">
        {items.map((it) => {
          const isActive =
            pathname === it.href.replace("/(app)", "") ||
            pathname.startsWith(it.href.replace("/(app)", "") + "/");
          return (
            <Link key={it.href} href={it.href as never} asChild>
              <Pressable
                className={cn(
                  "flex-row items-center gap-3 rounded-md px-2 py-2",
                  isActive
                    ? "bg-sidebar-accent"
                    : "active:bg-sidebar-accent/60",
                )}
              >
                <Icon
                  as={it.icon}
                  size={18}
                  className={cn(
                    isActive ? "text-sidebar-primary" : "text-muted-foreground",
                  )}
                />
                <Text
                  className={cn(
                    "text-sm font-medium",
                    isActive
                      ? "text-sidebar-foreground"
                      : "text-muted-foreground",
                  )}
                >
                  {it.label}
                </Text>
              </Pressable>
            </Link>
          );
        })}
      </View>

      {/* Bottom area: theme toggle + user block */}
      <View className="gap-1 border-t border-sidebar-border pt-3">
        {/* Theme cycle button */}
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="Design wechseln"
          onPress={() => setTheme(NEXT_THEME[theme])}
          className="flex-row items-center gap-3 rounded-md px-2 py-2 active:bg-sidebar-accent/60"
        >
          <Icon as={ThemeIcon} size={18} className="text-muted-foreground" />
          <Text className="text-sm font-medium text-muted-foreground">
            {theme === "light"
              ? "Hell"
              : theme === "dark"
                ? "Dunkel"
                : "System"}
          </Text>
        </Pressable>

        {/* User block → opens SettingsModal */}
        <Pressable
          testID="sidebar-settings-button"
          accessibilityRole="button"
          accessibilityLabel="Einstellungen öffnen"
          onPress={() => setSettingsOpen(true)}
          className="flex-row items-center gap-3 rounded-md px-2 py-2 active:bg-sidebar-accent/60"
        >
          {/* Avatar initials circle */}
          <View className="size-7 shrink-0 items-center justify-center rounded-full bg-primary">
            <Text className="text-[10px] font-bold text-primary-foreground">
              {user ? initials(user.name) : "?"}
            </Text>
          </View>
          <View className="min-w-0 flex-1">
            <Text
              className="text-sm font-medium text-sidebar-foreground"
              numberOfLines={1}
            >
              {user?.name ?? ""}
            </Text>
            <Text className="text-xs text-muted-foreground" numberOfLines={1}>
              {user?.studentType === "KURSADMIN"
                ? "Kursadmin"
                : user?.studentType === "SYSTEMADMIN"
                  ? "Systemadmin"
                  : "Student"}
            </Text>
          </View>
          <Icon as={Settings} size={15} className="text-muted-foreground" />
        </Pressable>
      </View>

      <SettingsModal open={settingsOpen} onOpenChange={setSettingsOpen} />
    </View>
  );
}
