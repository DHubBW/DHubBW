/**
 * TanStack Query hook for the direct children of a folder.
 *
 * Wraps `GET /student/ordner/{ordnerId}/children`. The query is gated by
 * the presence of `ordnerId`, which makes it a natural fit for the
 * lazy-load pattern in the folder tree: each expanded node mounts the
 * hook with its own UUID and only that hook fires.
 */
import { useQuery } from "@tanstack/react-query";

import { ApiError } from "@/lib/api/errors";
import { fetchClient } from "@/lib/api/fetchClient";
import type { OrdnerDTO } from "@/lib/api/types.gen";

/**
 * Builds the query-key tuple for a folder's children. Exposed so callers
 * sharing the same key can invalidate the cache without re-stringifying.
 *
 * @param ordnerId - UUID of the parent folder.
 */
export const ORDNER_CHILDREN_QUERY_KEY = (ordnerId: string) =>
  ["ordner", "children", ordnerId] as const;

/**
 * Queries the direct child folders of `ordnerId`. The query is enabled
 * only when `ordnerId` is a non-empty string; passing `null` keeps the
 * hook disabled so it can sit unconditionally inside a tree-node row.
 *
 * @param ordnerId - UUID of the parent folder, or `null` to disable.
 * @returns TanStack Query result containing the child folder list. A 403
 *   response resolves to `[]`; other errors propagate.
 */
export function useOrdnerChildren(ordnerId: string | null) {
  return useQuery<OrdnerDTO[]>({
    queryKey: ORDNER_CHILDREN_QUERY_KEY(ordnerId ?? "__disabled__"),
    queryFn: async () => {
      try {
        const res = await fetchClient(
          `/student/ordner/${ordnerId}/children`,
        );
        return (await res.json()) as OrdnerDTO[];
      } catch (err) {
        if (err instanceof ApiError && err.status === 403) {
          return [];
        }
        throw err;
      }
    },
    enabled: !!ordnerId,
    staleTime: 60_000,
  });
}
