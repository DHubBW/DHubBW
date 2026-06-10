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

import { ORDNER_CHILDREN_QUERY_KEY } from "./useOrdnerChildren";
import { ORDNER_TREE_QUERY_KEY } from "./useOrdnerTree";

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
 * `mutateAsync`; on success the tree, the parent's children and every
 * `['dateien', …]` cache slice are invalidated to reflect the cascade.
 *
 * @returns the TanStack-Query mutation handle.
 */
export function useOrdnerDelete() {
  const queryClient = useQueryClient();

  return useMutation<void, Error, DeleteOrdnerInput>({
    mutationFn: async ({ ordnerId }) => {
      await fetchClient(`/student/ordner/${ordnerId}`, {
        method: "DELETE",
      });
    },
    onSuccess: (_data, input) => {
      void queryClient.invalidateQueries({ queryKey: ORDNER_TREE_QUERY_KEY });
      if (input.ueberOrdnerId) {
        void queryClient.invalidateQueries({
          queryKey: ORDNER_CHILDREN_QUERY_KEY(input.ueberOrdnerId),
        });
      }
      // Cascade: every file list that points at the deleted subtree
      // becomes stale. The list keys all start with ['dateien'] so a
      // single prefix invalidation covers them.
      void queryClient.invalidateQueries({ queryKey: ["dateien"] });
    },
  });
}
