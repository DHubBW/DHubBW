import { ReactNode } from 'react';
import { useWindowDimensions, View } from 'react-native';

import { BottomNav } from './BottomNav';
import { Header } from './Header';
import { Sidebar } from './Sidebar';

const BREAKPOINT = 768;

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
