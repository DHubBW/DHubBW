import { Stack, useRouter } from 'expo-router';
import { useEffect } from 'react';

import { AppLayout } from '@/components/layout/AppLayout';
import { useAuth } from '@/lib/auth/__stub';

export default function AppGroupLayout() {
  const { isLoading, user } = useAuth();
  const router = useRouter();

  // ProtectedRoute: nicht-eingeloggte User → Login.
  // TODO FE-S1 (#206): echter Auth-Context ersetzt __stub.
  // Hinweis: __stub liefert per Default einen Stub-User, sodass kein
  // Redirect-Loop entsteht. Mit EXPO_PUBLIC_FORCE_NO_AUTH=true wird die
  // user=null-Variante getestet.
  useEffect(() => {
    if (!isLoading && !user) {
      router.replace('/(auth)/login');
    }
  }, [isLoading, user, router]);

  return (
    <AppLayout>
      <Stack screenOptions={{ headerShown: false }} />
    </AppLayout>
  );
}
