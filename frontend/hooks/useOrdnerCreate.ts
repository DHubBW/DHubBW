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
    // The CreateOrdnerDialog renders a precise, status-aware toast in
    // its own onError. Without this opt-out the global mutationCache
    // subscriber would fire a second generic toast on top.
    meta: { suppressGlobalErrorToast: true },
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
      // Invalidate the entire ordner-cache slice. A new folder may flip
      // its parent's `hasChildren` from false → true (so the chevron
      // appears) and the grand-parent's children list also changes —
      // it's cheaper and less error-prone to refetch every active
      // `["ordner", ...]` query than to thread the dependency through
      // the call sites.
      void queryClient.invalidateQueries({ queryKey: ["ordner"] });
    },
  });
}
