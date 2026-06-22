/**
 * TanStack Query hook for the comment thread of a single file.
 *
 * Wraps `GET /student/dateien/{dateiId}/kommentare`. The query is
 * disabled when `dateiId` is `null` so the hook is safe to call from a
 * dialog whose target is not yet known. A 403 response resolves to
 * `[]` so the UI can render the empty state without an extra branch
 * (the file's own list query already gates visibility — a stale 403
 * here would be a transient race).
 */
import { useQuery } from "@tanstack/react-query";

import { ApiError } from "@/lib/api/errors";
import { fetchClient } from "@/lib/api/fetchClient";
import type { KommentarDTO } from "@/lib/api/types.gen";

/** Builds the query-key tuple for a file's comment thread. */
export const KOMMENTARE_QUERY_KEY = (dateiId: string | null) =>
  ["kommentare", "list", dateiId ?? "none"] as const;

/**
 * Fetches all comments attached to the given file, oldest first.
 *
 * @param dateiId - UUID of the file, or `null` to disable the query.
 * @returns TanStack Query result containing the comment list.
 */
export function useKommentareForDatei(dateiId: string | null) {
  return useQuery<KommentarDTO[]>({
    queryKey: KOMMENTARE_QUERY_KEY(dateiId),
    enabled: dateiId !== null,
    queryFn: async () => {
      try {
        const res = await fetchClient(`/student/dateien/${dateiId}/kommentare`);
        return (await res.json()) as KommentarDTO[];
      } catch (err) {
        if (err instanceof ApiError && err.status === 403) {
          return [];
        }
        throw err;
      }
    },
    staleTime: 15_000,
  });
}
