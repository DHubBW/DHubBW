/**
 * TanStack Query mutation for creating a folder via
 * `PUT /student/ordner`.
 *
 * Body shape is {@link CreateOrdnerRequestDTO} (`{ ueberOrdnerId, name,
 * isPublic, farbe }`). On success the folder-tree query is invalidated so
 * the new folder appears in the sidebar without manual refetching; when
 * the new folder is a child of an already-expanded parent, that parent's
 * children query is invalidated too.
 *
 * Errors are surfaced as the {@link ApiError} thrown by `fetchClient`
 * (403/404/507/0). The Dateien screen consumes them via `onError` to
 * drive the create-dialog toast.
 */
import { useMutation, useQueryClient } from "@tanstack/react-query";

import { fetchClient } from "@/lib/api/fetchClient";
import type { CreateOrdnerRequestDTO } from "@/lib/api/types.gen";

import { ORDNER_CHILDREN_QUERY_KEY } from "./useOrdnerChildren";
import { ORDNER_TREE_QUERY_KEY } from "./useOrdnerTree";

/** Input shape for the create mutation. Matches the backend DTO 1:1. */
export type CreateOrdnerInput = CreateOrdnerRequestDTO;

/**
 * Create-folder mutation. Resolves to the new folder's UUID returned by
 * the backend (201 Created body); on success the matching cache slices
 * are invalidated.
 *
 * @returns the TanStack-Query mutation handle.
 */
export function useOrdnerCreate() {
  const queryClient = useQueryClient();

  return useMutation<string, Error, CreateOrdnerInput>({
    mutationFn: async (input) => {
      const body: CreateOrdnerRequestDTO = {
        ueberOrdnerId: input.ueberOrdnerId,
        name: input.name.trim(),
        isPublic: input.isPublic,
        farbe: input.farbe,
      };
      const res = await fetchClient("/student/ordner", {
        method: "PUT",
        body: JSON.stringify(body),
      });
      // The backend returns the new UUID as a JSON-encoded string.
      return (await res.json()) as string;
    },
    onSuccess: (_data, input) => {
      void queryClient.invalidateQueries({ queryKey: ORDNER_TREE_QUERY_KEY });
      // Sub-folder: also refresh the parent's lazy children list so the
      // new entry is visible inside an already-expanded branch.
      if (input.ueberOrdnerId) {
        void queryClient.invalidateQueries({
          queryKey: ORDNER_CHILDREN_QUERY_KEY(input.ueberOrdnerId),
        });
      }
    },
  });
}
