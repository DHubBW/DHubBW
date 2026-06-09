/**
 * TanStack Query hook for the top-level folder list.
 *
 * Wraps `GET /student/ordner` (no trailing slash — Spring's routing rejects
 * `/student/ordner/` with 500 on this endpoint). A 403 response is mapped
 * to an empty array so the UI can show the no-folder empty state without
 * a special error branch; other errors propagate.
 */
import { useQuery } from "@tanstack/react-query";

import { ApiError } from "@/lib/api/errors";
import { fetchClient } from "@/lib/api/fetchClient";
import type { OrdnerDTO } from "@/lib/api/types.gen";

export const ORDNER_TREE_QUERY_KEY = ["ordner", "tree"] as const;

/**
 * Queries the top-level folders of the caller's course.
 *
 * @returns TanStack Query result containing the root folder list. Resolves
 *   to `[]` on 403 (caller has no course); other errors propagate.
 */
export function useOrdnerTree() {
  return useQuery<OrdnerDTO[]>({
    queryKey: ORDNER_TREE_QUERY_KEY,
    queryFn: async () => {
      try {
        const res = await fetchClient("/student/ordner");
        return (await res.json()) as OrdnerDTO[];
      } catch (err) {
        if (err instanceof ApiError && err.status === 403) {
          return [];
        }
        throw err;
      }
    },
    staleTime: 60_000,
  });
}
