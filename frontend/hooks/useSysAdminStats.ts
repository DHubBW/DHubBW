/**
 * Fetches platform-wide KPI stats for a system admin.
 *
 * Wraps `GET /systemadmin/stats` — requires SYSTEMADMIN role.
 */
import { useQuery } from "@tanstack/react-query";

import { fetchClient } from "@/lib/api/fetchClient";
import type { SystemAdminStatsDTO } from "@/lib/api/types.gen";

export const SYSADMIN_STATS_QUERY_KEY = ["systemadmin", "stats"] as const;

/**
 * Queries `GET /systemadmin/stats`.
 *
 * Only call this hook when the current user's `studentType` is `"SYSTEMADMIN"`.
 *
 * @param enabled - when `false`, the query is paused. Default `true`.
 * @returns TanStack Query result containing {@link SystemAdminStatsDTO}.
 */
export function useSysAdminStats(enabled: boolean = true) {
  return useQuery<SystemAdminStatsDTO>({
    queryKey: SYSADMIN_STATS_QUERY_KEY,
    queryFn: async () => {
      const res = await fetchClient("/systemadmin/stats");
      return res.json() as Promise<SystemAdminStatsDTO>;
    },
    enabled,
    staleTime: 60_000,
  });
}
