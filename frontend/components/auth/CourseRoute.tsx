/**
 * Route guard for routes that require an authenticated student **enrolled in
 * a course** (i.e. {@link AuthUser.kursName} is not `null`).
 *
 * When the user is logged in but has no course assignment, navigation goes
 * to `/(app)/dashboard` — the dashboard is the place where the user can join
 * or create a course (FE-Dashboard).
 *
 * Implemented declaratively via expo-router `<Redirect>`; see
 * {@link ProtectedRoute} for the rationale.
 */
import { Redirect } from 'expo-router';
import type { ReactNode } from 'react';
import { Text, View } from 'react-native';

import { useAuth } from '@/lib/auth/AuthContext';

function AuthSplash() {
  return (
    <View className="flex-1 items-center justify-center bg-background">
      <Text className="text-foreground">Lade…</Text>
    </View>
  );
}

/**
 * Renders `children` only when the authenticated user has a course
 * assignment (`user.kursName !== null`).
 *
 * - `isLoading` → splash.
 * - `user === null` → redirect to login (with no returnTo — most callers
 *   wrap the tree with {@link ProtectedRoute} first, so this branch is a
 *   defensive fallback).
 * - `user.kursName === null` → redirect to `/(app)/dashboard`.
 * - otherwise → children.
 */
export function CourseRoute({ children }: { children: ReactNode }) {
  const { user, isLoading } = useAuth();

  if (isLoading) return <AuthSplash />;
  if (user === null) {
    return <Redirect href={'/(auth)/login' as never} />;
  }
  if (user.kursName === null) {
    return <Redirect href={'/(app)/dashboard' as never} />;
  }
  return <>{children}</>;
}
