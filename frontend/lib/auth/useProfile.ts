/**
 * Profile-restore hook for the authenticated user.
 *
 * Wraps `GET /student/dashboard/profile` in a TanStack-Query call. Backed by
 * the project-wide {@link queryClient} singleton from `lib/api/queryClient.ts`.
 *
 * Behaviour:
 * - **Query-Key:** `['profile']` — single entry; `useAuth().refresh()` and
 *   `queryClient.clear()` (logout flow) operate on this key.
 * - **`refetchInterval: 5*60_000`** + **`refetchIntervalInBackground: true`** —
 *   detects role changes (KURSADMIN promotion, course-enrolment) without a
 *   full page reload. Crucial for the role-based UI guards.
 * - **`retry: 0`** — auth state must not be guessed by retrying. A 401 from
 *   the server is the unambiguous "not logged in" signal which the
 *   {@link AuthProvider} consumes via `data === undefined`.
 *
 * On 401 the global `setUnauthorizedHandler`-Listener (registered by
 * {@link AuthProvider}) takes over and triggers the auto-logout sequence,
 * see `lib/auth/AuthContext.tsx`.
 */
import { useQuery } from '@tanstack/react-query';

import { ApiError } from '@/lib/api/errors';
import { fetchClient } from '@/lib/api/fetchClient';
import type { StudentProfileDTO } from '@/lib/api/types.gen';

/** TanStack-Query key the {@link useProfile} hook is registered under. */
export const PROFILE_QUERY_KEY = ['profile'] as const;

/** Backend route the profile is fetched from. */
const PROFILE_PATH = '/student/dashboard/profile';

/** 5 minutes — see hook docstring for rationale. */
const PROFILE_REFETCH_INTERVAL_MS = 5 * 60_000;

/**
 * Reads the current user's profile via `GET /student/dashboard/profile`.
 *
 * @param enabled - when `false`, the query is paused (no fetch, no
 *   background refetch). Default `true`. The {@link AuthProvider} flips
 *   this to `false` once the bootstrap call has resolved without an
 *   authenticated session, so the 5-min polling does not fire 401s on
 *   the login screen and trigger the global 401-handler / cookie
 *   detector unnecessarily.
 * @returns TanStack-Query result. `data` is `StudentProfileDTO | undefined`;
 *   `undefined` means either still loading, the query is disabled, or
 *   401 (no active session).
 */
export function useProfile(enabled: boolean = true) {
  return useQuery<StudentProfileDTO, ApiError>({
    queryKey: PROFILE_QUERY_KEY,
    queryFn: async () => {
      const res = await fetchClient(PROFILE_PATH);
      return (await res.json()) as StudentProfileDTO;
    },
    enabled,
    refetchInterval: PROFILE_REFETCH_INTERVAL_MS,
    refetchIntervalInBackground: true,
    // 401 means "not logged in" — that is a state, not a transient error.
    // The global setUnauthorizedHandler in AuthProvider handles cleanup.
    retry: 0,
    // Re-evaluate session whenever the app regains focus on Native.
    // (Web has refetchOnWindowFocus disabled in the queryClient defaults
    // intentionally — flicker mitigation. Profile is the one place where
    // that default is too conservative, but we accept it for now to keep
    // the queryClient defaults uniform.)
  });
}
