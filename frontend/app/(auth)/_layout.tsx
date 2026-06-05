import { Stack, useRouter } from 'expo-router';
import { useEffect } from 'react';

import { useAuth } from '@/lib/auth/__stub';

export default function AuthGroupLayout() {
  const { isLoading, user } = useAuth();
  const router = useRouter();

  // Anti-Auth-Guard: eingeloggte User landen direkt im App-Bereich.
  // TODO FE-S1 (#206): echter Auth-Context ersetzt __stub.
  useEffect(() => {
    if (!isLoading && user) {
      router.replace('/(app)/dashboard');
    }
  }, [isLoading, user, router]);

  return <Stack screenOptions={{ headerShown: false }} />;
}
