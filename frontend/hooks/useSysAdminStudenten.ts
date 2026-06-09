/**
 * Fetches all students platform-wide for a system admin.
 *
 * Wraps `GET /systemadmin/studenten` — requires SYSTEMADMIN role.
 */
import { useQuery } from "@tanstack/react-query";

import { fetchClient } from "@/lib/api/fetchClient";
import type { StudentDTO } from "@/lib/api/types.gen";

export const SYSADMIN_STUDENTEN_QUERY_KEY = [
  "systemadmin",
  "studenten",
] as const;

/**
 * Queries `GET /systemadmin/studenten`.
 *
 * @param enabled - when `false`, the query is paused. Default `true`.
 * @returns TanStack Query result containing an array of {@link StudentDTO}.
 */
export function useSysAdminStudenten(enabled: boolean = true) {
  return useQuery<StudentDTO[]>({
    queryKey: SYSADMIN_STUDENTEN_QUERY_KEY,
    queryFn: async () => {
      const res = await fetchClient("/systemadmin/studenten");
      return res.json() as Promise<StudentDTO[]>;
    },
    enabled,
    staleTime: 30_000,
  });
}
