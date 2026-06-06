import { Link } from 'expo-router';
import { Pressable, Text, View } from 'react-native';

import { ITEMS, visible } from '@/components/layout/navItems';
import { useAuth } from '@/lib/auth/AuthContext';

/**
 * Mobile navigation bar at the bottom of the screen.
 *
 * Renders only the {@link NavItem} entries visible to the logged-in user.
 * Mounted by {@link AppLayout} when the screen width is below 768 px.
 *
 * @returns The horizontal bottom navigation bar.
 */
export function BottomNav() {
  const { user } = useAuth();
  const items = ITEMS.filter((it) => visible(it, user));

  return (
    <View className="flex-row border-t border-sidebar-border bg-sidebar py-2">
      {items.map((it) => (
        <Link key={it.href} href={it.href as never} asChild>
          <Pressable className="flex-1 items-center py-1.5">
            <Text className="text-xs text-sidebar-foreground">{it.label}</Text>
          </Pressable>
        </Link>
      ))}
    </View>
  );
}
