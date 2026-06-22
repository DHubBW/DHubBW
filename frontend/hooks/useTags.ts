/**
 * TanStack Query hook for the global tag list.
 *
 * Wraps `GET /student/tags`. Tags are global (not per-course in the
 * MVP) — every authenticated student can read them. The list is the
 * source for both the tag picker on the file detail dialog and the
 * tag-filter chips in the file toolbar.
 */
import { useQuery } from "@tanstack/react-query";

import { fetchClient } from "@/lib/api/fetchClient";
import type { TagDTO } from "@/lib/api/types.gen";

export const TAGS_QUERY_KEY = ["tags", "list"] as const;

/**
 * Fetches all tags, sorted alphabetically.
 *
 * @returns TanStack Query result containing the tag list.
 */
export function useTags() {
  return useQuery<TagDTO[]>({
    queryKey: TAGS_QUERY_KEY,
    queryFn: async () => {
      const res = await fetchClient(`/student/tags`);
      return (await res.json()) as TagDTO[];
    },
    staleTime: 60_000,
  });
}
