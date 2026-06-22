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
 * <p>The unfiltered key (no tags) intentionally returns a
 * 3-element tuple so it stays interchangeable with
 * {@link DATEIEN_LIST_QUERY_KEY} from {@code useFileUpload} — that
 * factory is the auto-invalidation contract for upload-success
 * cache evictions, and TanStack Query matches sub-keys by prefix
 * so any tag-filtered variant ({@code [..., tagFingerprint]}) is
 * invalidated by the same root call.</p>
 *
 * @param ordnerId - UUID of the folder, or `null` for the root listing.
 * @param tagIds   - Optional list of tag UUIDs used for filtering.
 *   Order is normalised by the helper so different orderings hit
 *   the same cache slot. Only when {@code tagIds} is non-empty does
 *   the key gain a fourth element — keeping the unfiltered key at
 *   exactly 3 elements preserves backward compatibility.
 */
export const DATEIEN_LISTE_QUERY_KEY = (
  ordnerId: string | null,
  tagIds: string[] = [],
) => {
  const base = ["dateien", "list", ordnerId ?? DATEIEN_LIST_ALL_KEY] as const;
  if (tagIds.length === 0) {
    return base;
  }
  return [...base, [...tagIds].sort().join(",")] as const;
};

/**
 * Queries the files of a folder, or all top-level files of the course
 * when `ordnerId` is `null`. A 403 response resolves to `[]` so the UI
 * can render the empty state without an extra branch.
 *
 * @param ordnerId - UUID of the target folder, or `null` for the root view.
 * @param tagIds   - Optional tag UUIDs. When non-empty the backend
 *   only returns files that carry every given tag (AND-semantics).
 * @returns TanStack Query result containing the file list.
 */
export function useDateienListe(
  ordnerId: string | null,
  tagIds: string[] = [],
) {
  return useQuery<DateiDTO[]>({
    queryKey: DATEIEN_LISTE_QUERY_KEY(ordnerId, tagIds),
    queryFn: async () => {
      const tagQuery =
        tagIds.length === 0
          ? ""
          : `?${tagIds.map((id) => `tagIds=${encodeURIComponent(id)}`).join("&")}`;
      const path =
        ordnerId === null
          ? `/student/dateien${tagQuery}`
          : `/student/dateien/ordner/${ordnerId}${tagQuery}`;
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
