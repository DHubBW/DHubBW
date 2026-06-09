/**
 * TanStack Query mutation for renaming a file via
 * `PATCH /student/dateien/{dateiId}`.
 *
 * Body shape is {@link RenameDateiRequestDTO} (`{ newName }`). On success
 * the per-folder list query is invalidated using the same key shape that
 * {@link DATEIEN_LISTE_QUERY_KEY} produces, so the rename appears in the
 * table without manual refetching.
 *
 * Errors are surfaced as the {@link ApiError} thrown by `fetchClient`
 * (403/404/0). The Dateien screen consumes them via `onError` to drive
 * the rename-dialog toast.
 */
import { useMutation, useQueryClient } from "@tanstack/react-query";

import { fetchClient } from "@/lib/api/fetchClient";
import type { RenameDateiRequestDTO } from "@/lib/api/types.gen";

import { DATEIEN_LISTE_QUERY_KEY } from "./useDateienListe";

/** Input shape for the rename mutation. */
export interface RenameDateiInput {
  /** UUID of the file to rename. */
  dateiId: string;
  /** New display name. The hook trims before sending. */
  newName: string;
  /**
   * UUID of the folder the file belongs to, or `null` for the root view.
   * Used to invalidate the matching list query on success.
   */
  ordnerId: string | null;
}

/**
 * Rename mutation. Caller passes `{ dateiId, newName, ordnerId }` to
 * `mutateAsync`; on success the folder's file list is auto-invalidated.
 *
 * @returns the TanStack-Query mutation handle.
 */
export function useDateiRename() {
  const queryClient = useQueryClient();

  return useMutation<void, Error, RenameDateiInput>({
    mutationFn: async ({ dateiId, newName }) => {
      const body: RenameDateiRequestDTO = { newName: newName.trim() };
      await fetchClient(`/student/dateien/${dateiId}`, {
        method: "PATCH",
        body: JSON.stringify(body),
      });
    },
    onSuccess: (_data, input) => {
      void queryClient.invalidateQueries({
        queryKey: DATEIEN_LISTE_QUERY_KEY(input.ordnerId),
      });
    },
  });
}
