/**
 * Mutation hook for a SYSTEMADMIN to delete another student's
 * account with email confirmation.
 *
 * Sends `DELETE /systemadmin/studenten/{studentId}` with the body
 * `{ confirmedMail: <typed> }`. The backend trim+lower-case
 * compares the typed value against the target's `dhbwMail`; on
 * mismatch the request is rejected with HTTP 400. SYSTEMADMINs
 * cannot delete their own account through this endpoint and must
 * use the self-service in the settings instead.
 *
 * The mutation opts out of the global error toast
 * (`meta.suppressGlobalErrorToast === true`) because the calling
 * dialog renders a tailored, status-aware toast itself via
 * `toastForApiError({ 400: …, 409: … })` — see the StudentRow in
 * `app/(app)/verwaltung.tsx`.
 */
import { useMutation, useQueryClient } from "@tanstack/react-query";

import { ApiError } from "@/lib/api/errors";
import { fetchClient } from "@/lib/api/fetchClient";
import { SYSADMIN_STATS_QUERY_KEY } from "@/hooks/useSysAdminStats";
import { SYSADMIN_STUDENTEN_QUERY_KEY } from "@/hooks/useSysAdminStudenten";

/**
 * Input shape for the delete-student-by-sysadmin mutation.
 *
 * @public
 */
export interface DeleteStudentBySysAdminInput {
  /** UUID of the student account to delete. */
  studentId: string;
  /**
   * Email address that the SYSTEMADMIN typed into the confirmation
   * field. Must equal the target's `dhbwMail` after trim +
   * lower-case normalization on the backend; mismatch yields a
   * 400 response.
   */
  confirmedMail: string;
}

/**
 * Deletes a student account on behalf of a SYSTEMADMIN.
 *
 * On success, invalidates both the studenten list and the
 * platform stats cache so the row disappears and the user count
 * KPI refreshes.
 *
 * @returns the TanStack-Query mutation handle
 */
export function useDeleteStudentBySysAdmin() {
  const queryClient = useQueryClient();

  return useMutation<void, Error, DeleteStudentBySysAdminInput>({
    // The dialog renders its own status-aware toast on error
    // (400 / 409); suppress the generic global one to avoid
    // stacking two toasts for the same failure.
    meta: { suppressGlobalErrorToast: true },
    mutationFn: async ({ studentId, confirmedMail }) => {
      try {
        await fetchClient(`/systemadmin/studenten/${studentId}`, {
          method: "DELETE",
          body: JSON.stringify({ confirmedMail }),
        });
      } catch (err) {
        if (err instanceof ApiError) throw err;
        throw new ApiError(0, "Network error");
      }
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: SYSADMIN_STUDENTEN_QUERY_KEY,
      });
      void queryClient.invalidateQueries({
        queryKey: SYSADMIN_STATS_QUERY_KEY,
      });
    },
  });
}
