/**
 * TanStack Query mutation for creating a new comment on a file via
 * `POST /student/dateien/{dateiId}/kommentare`.
 *
 * The created {@link KommentarDTO} is returned and the comment-thread
 * query is invalidated using {@link KOMMENTARE_QUERY_KEY} so the new
 * row appears without a manual refetch.
 *
 * Errors surface as the standard {@link ApiError} (400/403/404/0). The
 * caller maps them to a toast in the comment composer.
 */
import { useMutation, useQueryClient } from "@tanstack/react-query";

import { fetchClient } from "@/lib/api/fetchClient";
import type {
  CreateKommentarRequestDTO,
  KommentarDTO,
} from "@/lib/api/types.gen";

import { KOMMENTARE_QUERY_KEY } from "./useKommentareForDatei";

/** Input shape for the create-comment mutation. */
export interface CreateKommentarInput {
  /** UUID of the file to comment on. */
  dateiId: string;
  /** Comment text body — backend trims and rejects empty input. */
  inhalt: string;
}

/**
 * Create-comment mutation. On success the file's comment thread is
 * auto-invalidated.
 *
 * @returns the TanStack-Query mutation handle.
 */
export function useKommentarCreate() {
  const queryClient = useQueryClient();

  return useMutation<KommentarDTO, Error, CreateKommentarInput>({
    // The CommentDialog renders its own status-aware toast on error;
    // suppress the generic global one to avoid stacking two.
    meta: { suppressGlobalErrorToast: true },
    mutationFn: async ({ dateiId, inhalt }) => {
      const body: CreateKommentarRequestDTO = { inhalt };
      const res = await fetchClient(`/student/dateien/${dateiId}/kommentare`, {
        method: "POST",
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
