/**
 * TanStack Query mutation for setting / updating the caller's
 * rating on a file via `PUT /student/dateien/{dateiId}/bewertung`.
 *
 * The backend returns the updated aggregate plus the caller's own
 * rating; the file's list query is invalidated so the new average
 * appears in the table without a manual refetch.
 */
import { useMutation, useQueryClient } from "@tanstack/react-query";

import { fetchClient } from "@/lib/api/fetchClient";
import type {
  DateiBewertungDTO,
  CreateBewertungRequestDTO,
} from "@/lib/api/types.gen";

import { DATEIEN_LISTE_QUERY_KEY } from "./useDateienListe";

/** Input shape for the rating mutation. */
export interface SetBewertungInput {
  /** UUID of the file to rate. */
  dateiId: string;
  /** Star count between 1 and 5 (inclusive). */
  sterne: number;
  /**
   * UUID of the folder the file belongs to, or `null` for the root
   * view. Used to invalidate the matching list query on success.
   */
  ordnerId: string | null;
}

/**
 * Build the per-file query-key tuple for the rating aggregate.
 *
 * @param dateiId - UUID of the file the aggregate belongs to.
 */
export const BEWERTUNG_QUERY_KEY = (dateiId: string | null) =>
  ["bewertung", dateiId ?? "none"] as const;

/**
 * Set-rating mutation. On success the file's listing and the
 * per-file aggregate query are auto-invalidated.
 *
 * @returns the TanStack-Query mutation handle.
 */
export function useBewertungSet() {
  const queryClient = useQueryClient();

  return useMutation<DateiBewertungDTO, Error, SetBewertungInput>({
    meta: { suppressGlobalErrorToast: true },
    mutationFn: async ({ dateiId, sterne }) => {
      const body: CreateBewertungRequestDTO = { sterne };
      const res = await fetchClient(`/student/dateien/${dateiId}/bewertung`, {
        method: "PUT",
        body: JSON.stringify(body),
      });
      return (await res.json()) as DateiBewertungDTO;
    },
    onSuccess: (_data, input) => {
      void queryClient.invalidateQueries({
        queryKey: DATEIEN_LISTE_QUERY_KEY(input.ordnerId),
      });
      void queryClient.invalidateQueries({
        queryKey: BEWERTUNG_QUERY_KEY(input.dateiId),
      });
    },
  });
}
