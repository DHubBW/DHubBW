import { Link } from 'expo-router';
import { Pressable, Text, View } from 'react-native';

import { ITEMS, visible } from '@/components/layout/navItems';
import { useAuth } from '@/lib/auth/__stub';

export function Sidebar() {
  const { user } = useAuth();
  const items = ITEMS.filter((it) => visible(it, user));

  return (
    <View className="w-[220px] border-r border-sidebar-border bg-sidebar px-3 py-4">
      <Text className="mb-4 text-lg font-semibold text-sidebar-foreground">DHubBW</Text>
      {items.map((it) => (
        <Link key={it.href} href={it.href as never} asChild>
          <Pressable className="px-2 py-2.5">
            <Text className="text-base text-sidebar-foreground">{it.label}</Text>
          </Pressable>
        </Link>
      ))}
    </View>
  );
}
