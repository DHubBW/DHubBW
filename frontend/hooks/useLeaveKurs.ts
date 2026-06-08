/**
 * Hook for the "Kurs verlassen" action.
 *
 * Calls `GET /student/dashboard/leave-kurs` (intentional GET per backend
 * design — see issue Errinnerung.md note). On success the profile query is
 * invalidated so navItems / dashboard reflect the new kursName=null state.
 *
 * Error handling:
 * - 400 → the student is Kursadmin; caller should surface a toast.
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
   * @throws {ApiError} on 400 (student is Kursadmin) or network error.
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
