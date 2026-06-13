/**
 * Hook for the "Account löschen" action.
 *
 * Calls `DELETE /student/dashboard/delete-account`. On success the caller
 * must clear the AuthContext and redirect to login — this hook does not
 * navigate itself (separation of concerns: UI decides the redirect path).
 *
 * Error handling:
 * - 409 → student is the last SYSTEMADMIN globally OR the last KURSADMIN
 *   in their own course (the controller does NOT catch
 *   {@link IllegalStateException} locally; the
 *   {@code GlobalExceptionHandler} maps it to 409 with a plain-text body
 *   that callers inspect to disambiguate the two cases — see
 *   {@code SettingsModal.handleDeleteAccount}).
 * - Network/other → re-thrown as ApiError(0).
 */
import { useCallback, useState } from "react";

import { ApiError } from "@/lib/api/errors";
import { fetchClient } from "@/lib/api/fetchClient";

/**
 * Return shape of the {@link useDeleteAccount} hook.
 *
 * @public
 */
export interface UseDeleteAccountResult {
  /** True while the delete-account request is in flight. */
  isPending: boolean;
  /**
   * Initiates the delete-account API call.
   *
   * @throws {ApiError} on 409 (last SYSTEMADMIN / last KURSADMIN) or
   *     network error.
   */
  deleteAccount: () => Promise<void>;
}

/**
 * Encapsulates the "Account löschen" API call.
 *
 * Navigation and AuthContext cleanup are the caller's responsibility.
 *
 * @returns {@link UseDeleteAccountResult}
 */
export function useDeleteAccount(): UseDeleteAccountResult {
  const [isPending, setIsPending] = useState(false);

  const deleteAccount = useCallback(async () => {
    setIsPending(true);
    try {
      await fetchClient("/student/dashboard/delete-account", {
        method: "DELETE",
      });
    } catch (err) {
      if (err instanceof ApiError) throw err;
      throw new ApiError(0, "Network error");
    } finally {
      setIsPending(false);
    }
  }, []);

  return { isPending, deleteAccount };
}
