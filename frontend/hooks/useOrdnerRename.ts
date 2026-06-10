/**
 * TanStack Query mutation for renaming a folder via
 * `PATCH /student/ordner/{ordnerId}`.
 *
 * Body shape is {@link RenameOrdnerRequestDTO} (`{ newName }`). On success
 * both the folder-tree query and the children query of the parent
 * (when known) are invalidated so the rename appears everywhere the
 * folder is rendered without manual refetching.
 *
 * Errors are surfaced as the {@link ApiError} thrown by `fetchClient`
 * (403/404/0). The Dateien screen consumes them via `onError` to drive
 * the rename-dialog toast.
 */
import { useMutation, useQueryClient } from "@tanstack/react-query";

import { fetchClient } from "@/lib/api/fetchClient";
import type { RenameOrdnerRequestDTO } from "@/lib/api/types.gen";

import { ORDNER_CHILDREN_QUERY_KEY } from "./useOrdnerChildren";
import { ORDNER_TREE_QUERY_KEY } from "./useOrdnerTree";

/** Input shape for the rename mutation. */
export interface RenameOrdnerInput {
  /** UUID of the folder to rename. */
  ordnerId: string;
  /** New display name. The hook trims before sending. */
  newName: string;
  /**
   * UUID of the parent folder, or `null` when the folder is a top-level
   * one. Used to invalidate the matching children query so the rename
   * appears inside an expanded parent branch.
   */
  ueberOrdnerId: string | null;
}

/**
 * Rename mutation. Caller passes `{ ordnerId, newName, ueberOrdnerId }`
 * to `mutateAsync`; on success the affected caches are invalidated.
 *
 * @returns the TanStack-Query mutation handle.
 */
export function useOrdnerRename() {
  const queryClient = useQueryClient();

  return useMutation<void, Error, RenameOrdnerInput>({
    mutationFn: async ({ ordnerId, newName }) => {
      const body: RenameOrdnerRequestDTO = { newName: newName.trim() };
      await fetchClient(`/student/ordner/${ordnerId}`, {
        method: "PATCH",
        body: JSON.stringify(body),
      });
    },
    onSuccess: (_data, input) => {
      void queryClient.invalidateQueries({ queryKey: ORDNER_TREE_QUERY_KEY });
      if (input.ueberOrdnerId) {
        void queryClient.invalidateQueries({
          queryKey: ORDNER_CHILDREN_QUERY_KEY(input.ueberOrdnerId),
        });
      }
    },
  });
}
