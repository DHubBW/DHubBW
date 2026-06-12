/**
 * TanStack Query mutation for moving a file via
 * `PATCH /student/dateien/{dateiId}/move`.
 *
 * <p>Body shape is {@code { targetOrdnerId }}. On success both the
 * source folder's and the target folder's list queries are
 * invalidated so the file disappears from one and re-appears in the
 * other without manual refetching.</p>
 *
 * <p>Errors surface as the {@link ApiError} thrown by
 * {@link fetchClient} (400/403/404/409/0). The Dateien screen
 * consumes them via {@code onError} to drive the move-dialog toast,
 * so we suppress the generic global error toast.</p>
 */
import { useMutation, useQueryClient } from "@tanstack/react-query";

import { fetchClient } from "@/lib/api/fetchClient";

import { DATEIEN_LISTE_QUERY_KEY } from "./useDateienListe";

/** Input shape for the move mutation. */
export interface MoveDateiInput {
  /** UUID of the file to move. */
  dateiId: string;
  /** UUID of the target folder. */
  targetOrdnerId: string;
  /**
   * UUID of the source folder, or {@code null} for the synthetic
   * root view. Used to invalidate the source list query.
   */
  sourceOrdnerId: string | null;
}

/**
 * Move mutation. On success both source and target folder list
 * queries are invalidated.
 *
 * @returns the TanStack-Query mutation handle.
 */
export function useDateiMove() {
  const queryClient = useQueryClient();

  return useMutation<void, Error, MoveDateiInput>({
    // The MoveDialog renders its own status-aware toast on error;
    // suppress the generic global one to avoid stacking two.
    meta: { suppressGlobalErrorToast: true },
    mutationFn: async ({ dateiId, targetOrdnerId }) => {
      await fetchClient(`/student/dateien/${dateiId}/move`, {
        method: "PATCH",
        body: JSON.stringify({ targetOrdnerId }),
      });
    },
    onSuccess: (_data, input) => {
      void queryClient.invalidateQueries({
        queryKey: DATEIEN_LISTE_QUERY_KEY(input.sourceOrdnerId),
      });
      void queryClient.invalidateQueries({
        queryKey: DATEIEN_LISTE_QUERY_KEY(input.targetOrdnerId),
      });
    },
  });
}
