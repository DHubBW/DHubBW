/**
 * TanStack Query mutation for deleting a file comment via
 * `DELETE /student/kommentare/{kommentarId}`.
 *
 * Backend authorisation: allowed for the comment author, KURSADMINs of
 * the file's course, and any SYSTEMADMIN. The mutation invalidates the
 * parent file's comment thread on success so the row vanishes without
 * a manual refetch.
 *
 * Errors surface as the standard {@link ApiError} (403/404/0). The
 * caller maps them to a toast in the comment thread.
 */
import { useMutation, useQueryClient } from "@tanstack/react-query";

import { fetchClient } from "@/lib/api/fetchClient";

import { KOMMENTARE_QUERY_KEY } from "./useKommentareForDatei";

/** Input shape for the delete-comment mutation. */
export interface DeleteKommentarInput {
  /** UUID of the comment to delete. */
  kommentarId: string;
  /**
   * UUID of the parent file. Used to invalidate the matching
   * comment-thread query on success.
   */
  dateiId: string;
}

/**
 * Delete-comment mutation. Caller passes `{ kommentarId, dateiId }`
 * to `mutateAsync`; on success the file's comment thread is
 * auto-invalidated.
 *
 * @returns the TanStack-Query mutation handle.
 */
export function useKommentarDelete() {
  const queryClient = useQueryClient();

  return useMutation<void, Error, DeleteKommentarInput>({
    meta: { suppressGlobalErrorToast: true },
    mutationFn: async ({ kommentarId }) => {
      await fetchClient(`/student/kommentare/${kommentarId}`, {
        method: "DELETE",
      });
    },
    onSuccess: (_data, input) => {
      void queryClient.invalidateQueries({
        queryKey: KOMMENTARE_QUERY_KEY(input.dateiId),
      });
    },
  });
}
