/**
 * TanStack Query mutation for deleting a file via
 * `DELETE /student/dateien/{dateiId}`.
 *
 * The backend returns 204 with a small status DTO; we discard the body
 * and surface a `void` success. On success the per-folder list query is
 * invalidated using {@link DATEIEN_LISTE_QUERY_KEY} so the row vanishes
 * without a manual refetch.
 *
 * Errors are the standard {@link ApiError} surface (403/404/0) — the
 * caller maps them to a toast.
 */
import { useMutation, useQueryClient } from "@tanstack/react-query";

import { fetchClient } from "@/lib/api/fetchClient";

import { DATEIEN_LISTE_QUERY_KEY } from "./useDateienListe";

/** Input shape for the delete mutation. */
export interface DeleteDateiInput {
  /** UUID of the file to delete. */
  dateiId: string;
  /**
   * UUID of the folder the file belongs to, or `null` for the root view.
   * Used to invalidate the matching list query on success.
   */
  ordnerId: string | null;
}

/**
 * Delete mutation. Caller passes `{ dateiId, ordnerId }` to
 * `mutateAsync`; on success the folder's file list is auto-invalidated.
 *
 * @returns the TanStack-Query mutation handle.
 */
export function useDateiDelete() {
  const queryClient = useQueryClient();

  return useMutation<void, Error, DeleteDateiInput>({
    // The Dateien screen renders its own status-aware toast on error;
    // suppress the generic global one to avoid stacking two.
    meta: { suppressGlobalErrorToast: true },
    mutationFn: async ({ dateiId }) => {
      await fetchClient(`/student/dateien/${dateiId}`, {
        method: "DELETE",
      });
    },
    onSuccess: (_data, input) => {
      void queryClient.invalidateQueries({
        queryKey: DATEIEN_LISTE_QUERY_KEY(input.ordnerId),
      });
    },
  });
}
