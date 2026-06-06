/**
 * TEMP STUB for Sub-A (FE-S2). Will be replaced by FE-S1 (issue #206) with a
 * real AuthContext + JWT restore via /student/dashboard/profile.
 *
 * When replacing: rename this file to `lib/auth/AuthContext.tsx` and update
 * all imports from `@/lib/auth/__stub` to `@/lib/auth/AuthContext`.
 *
 * DO NOT use in production.
 */

export type StudentType = 'STUDENT' | 'KURSADMIN' | 'SYSTEMADMIN';

export type AuthUser = {
  id: string;
  name: string;
  dhbwMail: string;
  studentType: StudentType;
  kursName: string | null;
};

export type AuthState = {
  isLoading: boolean;
  user: AuthUser | null;
};

const STUB_USER: AuthUser = {
  id: 'stub',
  name: 'Stub User',
  dhbwMail: 'stub@dhbw-karlsruhe.de',
  studentType: 'STUDENT',
  kursName: null,
};

// Dev-only override: EXPO_PUBLIC_FORCE_NO_AUTH=true forces user=null so the
// anti-auth guard and login redirect can be tested manually.
const FORCE_NO_AUTH = process.env.EXPO_PUBLIC_FORCE_NO_AUTH === 'true';

/**
 * Returns the current authentication state.
 *
 * This stub always returns a hard-coded user. After issue #206 is implemented
 * this hook will be replaced by a real JWT-based context.
 *
 * @returns {@link AuthState} containing `isLoading` and `user`.
 */
export function useAuth(): AuthState {
  return {
    isLoading: false,
    user: FORCE_NO_AUTH ? null : STUB_USER,
  };
}
