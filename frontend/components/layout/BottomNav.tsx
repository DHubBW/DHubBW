import { Link, usePathname } from "expo-router";
import { Pressable, Text, View } from "react-native";

import { Icon } from "@/components/ui/icon";
import { ITEMS, visible } from "@/components/layout/navItems";
import { useAuth } from "@/lib/auth/AuthContext";
import { cn } from "@/lib/utils";

/**
 * Mobile navigation bar at the bottom of the screen.
 *
 * Renders icon + label for each visible {@link NavItem}. The currently active
 * route is highlighted with the primary colour. Mounted by {@link AppLayout}
 * below 768 px.
 *
 * @returns The horizontal bottom navigation bar.
 */
export function BottomNav() {
  const { user } = useAuth();
  const pathname = usePathname();
  const items = ITEMS.filter((it) => visible(it, user));

  return (
    <View className="flex-row border-t border-sidebar-border bg-sidebar">
      {items.map((it) => {
        const basePath = (it.activePath ?? it.href)
          .replace("/(app)", "")
          .split("?")[0];
        const isActive =
          pathname === basePath ||
          pathname.startsWith(basePath + "/");
        return (
          <Link key={it.href} href={it.href as never} asChild>
            <Pressable className="flex-1 items-center gap-0.5 py-2.5">
              <Icon
                as={it.icon}
                size={22}
                className={cn(
                  isActive ? "text-sidebar-primary" : "text-muted-foreground",
                )}
              />
              <Text
                className={cn(
                  "text-[10px] font-medium",
                  isActive ? "text-sidebar-primary" : "text-muted-foreground",
                )}
              >
                {it.label}
              </Text>
            </Pressable>
          </Link>
        );
      })}
    </View>
  );
}
