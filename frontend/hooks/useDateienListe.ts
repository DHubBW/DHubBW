/**
 * TanStack Query hook for the file list of a folder, plus the "Alle Dateien"
 * root listing.
 *
 * The query-key shape matches the contract that
 * {@link DATEIEN_LIST_QUERY_KEY} from `useFileUpload` uses for
 * auto-invalidation after a successful upload. When `ordnerId` is `null`,
 * the hook fetches `GET /student/dateien` (top-level files of the caller's
 * course); otherwise it fetches `GET /student/dateien/ordner/{ordnerId}`.
 * The root cache uses the `'all'` sentinel so the keys remain a `(string,
 * string, string)` tuple in line with the upload-side factory.
 */
import { useQuery } from "@tanstack/react-query";

import { ApiError } from "@/lib/api/errors";
import { fetchClient } from "@/lib/api/fetchClient";
import type { DateiDTO } from "@/lib/api/types.gen";

/** Sentinel used as the third tuple slot for the "Alle Dateien" root view. */
export const DATEIEN_LIST_ALL_KEY = "all" as const;

/**
 * Builds the query-key tuple for a folder's file list.
 *
 * @param ordnerId - UUID of the folder, or `null` for the root listing.
 */
export const DATEIEN_LISTE_QUERY_KEY = (ordnerId: string | null) =>
  ["dateien", "list", ordnerId ?? DATEIEN_LIST_ALL_KEY] as const;

/**
 * Queries the files of a folder, or all top-level files of the course
 * when `ordnerId` is `null`. A 403 response resolves to `[]` so the UI
 * can render the empty state without an extra branch.
 *
 * @param ordnerId - UUID of the target folder, or `null` for the root view.
 * @returns TanStack Query result containing the file list.
 */
export function useDateienListe(ordnerId: string | null) {
  return useQuery<DateiDTO[]>({
    queryKey: DATEIEN_LISTE_QUERY_KEY(ordnerId),
    queryFn: async () => {
      const path =
        ordnerId === null
          ? "/student/dateien"
          : `/student/dateien/ordner/${ordnerId}`;
      try {
        const res = await fetchClient(path);
        return (await res.json()) as DateiDTO[];
      } catch (err) {
        if (err instanceof ApiError && err.status === 403) {
          return [];
        }
        throw err;
      }
    },
    staleTime: 30_000,
  });
}
