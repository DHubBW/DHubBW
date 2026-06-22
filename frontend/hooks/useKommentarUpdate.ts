/**
 * TanStack Query mutation for editing a file comment via
 * `PATCH /student/kommentare/{kommentarId}`.
 *
 * Backend authorisation: only the original author may edit. The
 * mutation invalidates the parent file's comment thread on success so
 * the updated row (with `bearbeitet = true` and a fresh
 * `zuletztBearbeitet` timestamp) appears immediately.
 *
 * Errors surface as the standard {@link ApiError} (400/403/404/0). The
 * caller maps them to a toast in the edit composer.
 */
import { useMutation, useQueryClient } from "@tanstack/react-query";

import { fetchClient } from "@/lib/api/fetchClient";
import type {
  KommentarDTO,
  UpdateKommentarRequestDTO,
} from "@/lib/api/types.gen";

import { KOMMENTARE_QUERY_KEY } from "./useKommentareForDatei";

/** Input shape for the update-comment mutation. */
export interface UpdateKommentarInput {
  /** UUID of the comment to edit. */
  kommentarId: string;
  /** New comment text body — backend trims and rejects empty input. */
  inhalt: string;
  /**
   * UUID of the parent file. Used to invalidate the matching
   * comment-thread query on success.
   */
  dateiId: string;
}

/**
 * Update-comment mutation. Caller passes
 * `{ kommentarId, inhalt, dateiId }` to `mutateAsync`; on success the
 * file's comment thread is auto-invalidated.
 *
 * @returns the TanStack-Query mutation handle.
 */
export function useKommentarUpdate() {
  const queryClient = useQueryClient();

  return useMutation<KommentarDTO, Error, UpdateKommentarInput>({
    meta: { suppressGlobalErrorToast: true },
    mutationFn: async ({ kommentarId, inhalt }) => {
      const body: UpdateKommentarRequestDTO = { inhalt };
      const res = await fetchClient(`/student/kommentare/${kommentarId}`, {
        method: "PATCH",
        body: JSON.stringify(body),
      });
      return (await res.json()) as KommentarDTO;
    },
    onSuccess: (_data, input) => {
      void queryClient.invalidateQueries({
        queryKey: KOMMENTARE_QUERY_KEY(input.dateiId),
      });
    },
  });
}
