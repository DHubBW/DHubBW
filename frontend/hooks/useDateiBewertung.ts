/**
 * TanStack Query hook for the caller's own rating on a file.
 *
 * Wraps `GET /student/dateien/{dateiId}/bewertung`. The endpoint
 * returns both the per-file aggregate (also surfaced on
 * {@link DateiDTO}) and the caller's own rating, so this hook is the
 * single source of truth for the "I voted N stars" state when the
 * rating UI is open.
 *
 * The query is disabled when {@code dateiId} is `null` so the hook is
 * safe to call from a dialog before a row is targeted.
 */
import { useQuery } from "@tanstack/react-query";

import { ApiError } from "@/lib/api/errors";
import { fetchClient } from "@/lib/api/fetchClient";
import type { DateiBewertungDTO } from "@/lib/api/types.gen";

import { BEWERTUNG_QUERY_KEY } from "./useBewertungSet";

/**
 * Fetches aggregate + caller's own rating for the given file.
 *
 * @param dateiId - UUID of the file, or `null` to disable.
 * @returns TanStack Query result containing the rating data.
 */
export function useDateiBewertung(dateiId: string | null) {
  return useQuery<DateiBewertungDTO>({
    queryKey: BEWERTUNG_QUERY_KEY(dateiId),
    enabled: dateiId !== null,
    queryFn: async () => {
      try {
        const res = await fetchClient(`/student/dateien/${dateiId}/bewertung`);
        return (await res.json()) as DateiBewertungDTO;
      } catch (err) {
        if (err instanceof ApiError && err.status === 403) {
          // Mirror the dateien-list behaviour: a 403 here just means
          // the user can't see the aggregate yet; render the
          // "no rating" state instead of bubbling the error.
          return { durchschnitt: 0, anzahl: 0, eigeneSterne: 0 };
        }
        throw err;
      }
    },
    staleTime: 15_000,
  });
}
