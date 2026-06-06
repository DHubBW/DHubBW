import { ReactNode } from 'react';
import { useWindowDimensions, View } from 'react-native';

import { BottomNav } from './BottomNav';
import { Header } from './Header';
import { Sidebar } from './Sidebar';

const BREAKPOINT = 768;

/**
 * Central app shell layout.
 *
 * Below 768 px the mobile layout is rendered (Header + Content + BottomNav).
 * At 768 px and above the desktop sidebar is shown to the left of the content area.
 *
 * @param children - Page content rendered inside the content area.
 * @returns The responsive app layout.
 */
export function AppLayout({ children }: { children: ReactNode }) {
  const { width } = useWindowDimensions();
  const isWide = width >= BREAKPOINT;

  if (isWide) {
    return (
      <View className="flex-1 flex-row bg-background">
        <Sidebar />
        <View className="flex-1">{children}</View>
      </View>
    );
  }

  return (
    <View className="flex-1 flex-col bg-background">
      <Header />
      <View className="flex-1">{children}</View>
      <BottomNav />
    </View>
  );
}
