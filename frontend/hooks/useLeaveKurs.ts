/**
 * Hook for the "Kurs verlassen" action.
 *
 * Calls `GET /student/dashboard/leave-kurs` (the backend uses GET for this
 * idempotent action — see {@link StudentDashboardController#leaveKurs}).
 * On success the profile query is invalidated so navItems / dashboard
 * reflect the new kursName=null state.
 *
 * Error handling:
 * - 400 → the student is the last KURSADMIN of the course; the controller
 *   catches {@link IllegalStateException} locally and returns 400 with a
 *   plain-text body. 400 is not in {@link ApiErrorStatus}, so it surfaces
 *   as `ApiError(0, 'HTTP 400', body)`. Callers branch on the body text
 *   ({@code "KURSADMIN"} substring) to detect this case.
 * - 404 → no course enrollment (also lands on `ApiError(0, 'HTTP 404')`).
 * - Network/other → re-thrown as ApiError(0).
 */
import { useQueryClient } from "@tanstack/react-query";
import { useCallback, useState } from "react";

import { ApiError } from "@/lib/api/errors";
import { fetchClient } from "@/lib/api/fetchClient";
import { PROFILE_QUERY_KEY } from "@/lib/auth/useProfile";

/**
 * Return shape of the {@link useLeaveKurs} hook.
 *
 * @public
 */
export interface UseLeaveKursResult {
  /** True while the leave-kurs request is in flight. */
  isPending: boolean;
  /**
   * Initiates the leave-kurs API call.
   *
   * @throws {ApiError} on 400 (last KURSADMIN — surfaces as
   *     `ApiError(0, 'HTTP 400')`), 404 (no enrollment) or network
   *     error.
   */
  leaveKurs: () => Promise<void>;
}

/**
 * Encapsulates the "Kurs verlassen" flow.
 *
 * On success the `['profile']` query is invalidated so the nav and
 * dashboard reflect the updated `kursName=null` without a full reload.
 *
 * @returns {@link UseLeaveKursResult}
 */
export function useLeaveKurs(): UseLeaveKursResult {
  const queryClient = useQueryClient();
  const [isPending, setIsPending] = useState(false);

  const leaveKurs = useCallback(async () => {
    setIsPending(true);
    try {
      await fetchClient("/student/dashboard/leave-kurs", { method: "GET" });
      await queryClient.invalidateQueries({ queryKey: PROFILE_QUERY_KEY });
    } catch (err) {
      if (err instanceof ApiError) throw err;
      throw new ApiError(0, "Network error");
    } finally {
      setIsPending(false);
    }
  }, [queryClient]);

  return { isPending, leaveKurs };
}
