/**
 * TanStack Query mutation for deleting a folder via
 * `DELETE /student/ordner/{ordnerId}`.
 *
 * The backend cascades the delete across all child folders and files —
 * on success we therefore invalidate the folder-tree query, the parent's
 * children query (when known) and the entire `['dateien']` family so any
 * file list that referenced a now-deleted folder refetches.
 *
 * Errors are surfaced as the {@link ApiError} thrown by `fetchClient`
 * (403/404/0). The Dateien screen consumes them via `onError` to drive
 * the delete-confirmation toast.
 */
import { useMutation, useQueryClient } from "@tanstack/react-query";

import { fetchClient } from "@/lib/api/fetchClient";

/** Input shape for the delete mutation. */
export interface DeleteOrdnerInput {
  /** UUID of the folder to delete. */
  ordnerId: string;
  /**
   * UUID of the parent folder, or `null` when the folder is a top-level
   * one. Used to invalidate the parent's children query.
   */
  ueberOrdnerId: string | null;
}

/**
 * Delete mutation. Caller passes `{ ordnerId, ueberOrdnerId }` to
 * `mutateAsync`; on success the entire `['ordner']` and `['dateien']`
 * cache slices are invalidated to reflect the cascade — the parent's
 * `hasChildren` flag may flip back to `false`, every file list under
 * the deleted subtree becomes stale, and any other expanded branch
 * may need to refresh.
 *
 * @returns the TanStack-Query mutation handle.
 */
export function useOrdnerDelete() {
  const queryClient = useQueryClient();

  return useMutation<void, Error, DeleteOrdnerInput>({
    // The DeleteOrdnerConfirm renders its own status-aware toast on
    // error; suppress the generic global one to avoid stacking two
    // for the same failure.
    meta: { suppressGlobalErrorToast: true },
    mutationFn: async ({ ordnerId }) => {
      await fetchClient(`/student/ordner/${ordnerId}`, {
        method: "DELETE",
      });
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["ordner"] });
      // Cascade: every file list that points at the deleted subtree
      // becomes stale. The list keys all start with ['dateien'] so a
      // single prefix invalidation covers them.
      void queryClient.invalidateQueries({ queryKey: ["dateien"] });
    },
  });
}
