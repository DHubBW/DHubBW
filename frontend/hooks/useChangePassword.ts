/**
 * Hook for the "Passwort ändern" action.
 *
 * Calls `PATCH /student/dashboard/change-password`. Errors flow up as
 * {@link ApiError} so the calling dialog can surface a status-aware
 * toast (e.g. 401 → "Aktuelles Passwort ist falsch", 400 → use the
 * backend body verbatim).
 */
import { useCallback, useState } from "react";

import { ApiError } from "@/lib/api/errors";
import { fetchClient } from "@/lib/api/fetchClient";

/**
 * Return shape of the {@link useChangePassword} hook.
 *
 * @public
 */
export interface UseChangePasswordResult {
  /** True while the change-password request is in flight. */
  isPending: boolean;
  /**
   * Initiates the change-password API call.
   *
   * @throws {ApiError} on 400 (validation), 401 (wrong current
   *     password), or network error.
   */
  changePassword: (
    currentPassword: string,
    newPassword: string,
    repeatPassword: string,
  ) => Promise<void>;
}

/**
 * Encapsulates the "Passwort ändern" API call. Toast/UI feedback is
 * the caller's responsibility — the hook only reports `isPending`
 * and re-throws errors as {@link ApiError}.
 *
 * @returns {@link UseChangePasswordResult}
 */
export function useChangePassword(): UseChangePasswordResult {
  const [isPending, setIsPending] = useState(false);

  const changePassword = useCallback(
    async (
      currentPassword: string,
      newPassword: string,
      repeatPassword: string,
    ) => {
      setIsPending(true);
      try {
        await fetchClient("/student/dashboard/change-password", {
          method: "PATCH",
          body: JSON.stringify({
            currentPassword,
            newPassword,
            repeatPassword,
          }),
        });
      } catch (err) {
        if (err instanceof ApiError) throw err;
        throw new ApiError(0, "Network error");
      } finally {
        setIsPending(false);
      }
    },
    [],
  );

  return { isPending, changePassword };
}
