/**
 * TEMP-STUB für Sub-A (FE-S2). Wird durch FE-S1 (Issue #206) ersetzt mit echtem
 * AuthContext + JWT-Restore via /student/dashboard/profile.
 *
 * Bei Ersetzung: Datei umbenennen nach `lib/auth/AuthContext.tsx` und Imports
 * (`@/lib/auth/__stub`) auf `@/lib/auth/AuthContext` ändern.
 *
 * NICHT in Production verwenden.
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

// Dev-Only Override: EXPO_PUBLIC_FORCE_NO_AUTH=true erzwingt user=null,
// damit Anti-Auth-Guard und Login-Redirect manuell testbar sind.
const FORCE_NO_AUTH = process.env.EXPO_PUBLIC_FORCE_NO_AUTH === 'true';

export function useAuth(): AuthState {
  return {
    isLoading: false,
    user: FORCE_NO_AUTH ? null : STUB_USER,
  };
}
