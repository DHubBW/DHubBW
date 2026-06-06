/**
 * Route guard for routes that require admin-level role
 * ({@link StudentType} `KURSADMIN` or `SYSTEMADMIN`).
 *
 * Plain students get redirected to `/(app)/dashboard`. Implemented
 * declaratively via expo-router `<Redirect>` — see {@link ProtectedRoute}.
 */
import { Redirect } from 'expo-router';
import type { ReactNode } from 'react';
import { Text, View } from 'react-native';

import type { StudentType } from '@/lib/api/types.gen';
import { useAuth } from '@/lib/auth/AuthContext';

const ADMIN_ROLES: ReadonlySet<StudentType> = new Set<StudentType>([
  'KURSADMIN',
  'SYSTEMADMIN',
]);

function AuthSplash() {
  return (
    <View className="flex-1 items-center justify-center bg-background">
      <Text className="text-foreground">Lade…</Text>
    </View>
  );
}

/**
 * Renders `children` only when the authenticated user is `KURSADMIN` or
 * `SYSTEMADMIN`.
 */
export function AdminRoute({ children }: { children: ReactNode }) {
  const { user, isLoading } = useAuth();

  if (isLoading) return <AuthSplash />;
  if (user === null) {
    return <Redirect href={'/(auth)/login' as never} />;
  }
  if (!ADMIN_ROLES.has(user.studentType)) {
    return <Redirect href={'/(app)/dashboard' as never} />;
  }
  return <>{children}</>;
}
