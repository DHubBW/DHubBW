/**
 * Fetches all courses for a system admin.
 *
 * Wraps `GET /systemadmin/kurse` — requires SYSTEMADMIN role.
 */
import { useQuery } from "@tanstack/react-query";

import { fetchClient } from "@/lib/api/fetchClient";
import type { KursDTO } from "@/lib/api/types.gen";

export const SYSADMIN_KURSE_QUERY_KEY = ["systemadmin", "kurse"] as const;

/**
 * Queries `GET /systemadmin/kurse`.
 *
 * @param enabled - when `false`, the query is paused. Default `true`.
 * @returns TanStack Query result containing an array of {@link KursDTO}.
 */
export function useSysAdminKurse(enabled: boolean = true) {
  return useQuery<KursDTO[]>({
    queryKey: SYSADMIN_KURSE_QUERY_KEY,
    queryFn: async () => {
      const res = await fetchClient("/systemadmin/kurse");
      return res.json() as Promise<KursDTO[]>;
    },
    enabled,
    staleTime: 30_000,
  });
}
