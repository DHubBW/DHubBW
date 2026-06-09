import { Link, usePathname } from "expo-router";
import { Image } from "expo-image";
import {
  ChevronLeft,
  ChevronRight,
  Moon,
  Settings,
  Sun,
  SunMoon,
} from "lucide-react-native";
import { useState } from "react";
import { Pressable, Text, View } from "react-native";

import { SettingsModal } from "@/components/SettingsModal";
import { Icon } from "@/components/ui/icon";
import { ITEMS, visible } from "@/components/layout/navItems";
import { useAuth } from "@/lib/auth/AuthContext";
import { useTheme, type ThemePreference } from "@/lib/theme/ThemeProvider";
import { cn } from "@/lib/utils";

const LOGO = require("@/assets/images/icon.png") as number;

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
 * Collapsible: wide mode shows icon + label; collapsed mode shows icons only.
 * Logout button and collapse toggle live at the bottom. Mounted by
 * {@link AppLayout} at ≥768 px.
 *
 * @returns The full-height sidebar.
 */
export function Sidebar() {
  const { user } = useAuth();
  const { theme, setTheme } = useTheme();
  const pathname = usePathname();
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [collapsed, setCollapsed] = useState(false);

  const items = ITEMS.filter((it) => visible(it, user));
  const ThemeIcon = THEME_ICONS[theme];

  const roleLabel =
    user?.studentType === "KURSADMIN"
      ? "Kursadmin"
      : user?.studentType === "SYSTEMADMIN"
        ? "Systemadmin"
        : "Student";

  return (
    <View
      className={cn(
        "flex-col border-r border-sidebar-border bg-sidebar px-2 py-4",
        collapsed ? "w-14" : "w-[220px]",
      )}
    >
      {/* Logo — hidden when collapsed */}
      {!collapsed && (
        <View className="mb-6 items-center px-1">
          <Image
            source={LOGO}
            style={{ width: 160, height: 50 }}
            contentFit="contain"
          />
        </View>
      )}
      {collapsed && <View className="mb-6 h-[50px]" />}

      {/* Nav items */}
      <View className="flex-1 gap-0.5">
        {items.map((it) => {
          const basePath = (it.activePath ?? it.href)
            .replace("/(app)", "")
            .split("?")[0];
          const isActive =
            pathname === basePath || pathname.startsWith(basePath + "/");
          return (
            <Link key={it.href} href={it.href as never} asChild>
              <Pressable
                className={cn(
                  "flex-row items-center rounded-md px-2 py-2",
                  collapsed ? "justify-center" : "gap-3",
                  isActive
                    ? "bg-sidebar-accent"
                    : "active:bg-sidebar-accent/60",
                )}
              >
                <Icon
                  as={it.icon}
                  size={18}
                  className={cn(
                    isActive
                      ? "text-sidebar-primary"
                      : "text-muted-foreground",
                  )}
                />
                {!collapsed && (
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
                )}
              </Pressable>
            </Link>
          );
        })}
      </View>

      {/* Bottom area */}
      <View className="gap-0.5 border-t border-sidebar-border pt-3">
        {/* Theme cycle */}
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="Design wechseln"
          onPress={() => setTheme(NEXT_THEME[theme])}
          className={cn(
            "flex-row items-center rounded-md px-2 py-2 active:bg-sidebar-accent/60",
            collapsed ? "justify-center" : "gap-3",
          )}
        >
          <Icon as={ThemeIcon} size={18} className="text-muted-foreground" />
          {!collapsed && (
            <Text className="text-sm font-medium text-muted-foreground">
              {theme === "light"
                ? "Hell"
                : theme === "dark"
                  ? "Dunkel"
                  : "System"}
            </Text>
          )}
        </Pressable>

        {/* User block → opens SettingsModal */}
        <Pressable
          testID="sidebar-settings-button"
          accessibilityRole="button"
          accessibilityLabel="Einstellungen öffnen"
          onPress={() => setSettingsOpen(true)}
          className={cn(
            "flex-row items-center rounded-md px-2 py-2 active:bg-sidebar-accent/60",
            collapsed ? "justify-center" : "gap-3",
          )}
        >
          <View className="size-7 shrink-0 items-center justify-center rounded-full bg-primary">
            <Text className="text-[10px] font-bold text-primary-foreground">
              {user ? initials(user.name) : "?"}
            </Text>
          </View>
          {!collapsed && (
            <>
              <View className="min-w-0 flex-1">
                <Text
                  className="text-sm font-medium text-sidebar-foreground"
                  numberOfLines={1}
                >
                  {user?.name ?? ""}
                </Text>
                <Text
                  className="text-xs text-muted-foreground"
                  numberOfLines={1}
                >
                  {roleLabel}
                </Text>
              </View>
              <Icon as={Settings} size={15} className="text-muted-foreground" />
            </>
          )}
        </Pressable>

        {/* Logout — already above, remove duplicate */}

        {/* Collapse toggle */}
        <Pressable
          accessibilityRole="button"
          accessibilityLabel={collapsed ? "Sidebar ausklappen" : "Sidebar einklappen"}
          onPress={() => setCollapsed((v) => !v)}
          className="flex-row items-center justify-center rounded-md px-2 py-2 active:bg-sidebar-accent/60"
        >
          <Icon
            as={collapsed ? ChevronRight : ChevronLeft}
            size={18}
            className="text-muted-foreground"
          />
        </Pressable>
      </View>

      <SettingsModal open={settingsOpen} onOpenChange={setSettingsOpen} />
    </View>
  );
}
