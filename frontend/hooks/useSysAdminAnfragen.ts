/**
 * Fetches all pending course-creation requests for a system admin.
 *
 * Wraps `GET /systemadmin/anfragen` — requires SYSTEMADMIN role.
 */
import { useQuery } from "@tanstack/react-query";

import { fetchClient } from "@/lib/api/fetchClient";
import type { KursErstellungsAnfrageDTO } from "@/lib/api/types.gen";

export const SYSADMIN_ANFRAGEN_QUERY_KEY = ["systemadmin", "anfragen"] as const;

/**
 * Queries `GET /systemadmin/anfragen`.
 *
 * @param enabled - when `false`, the query is paused. Default `true`.
 * @returns TanStack Query result containing an array of
 *   {@link KursErstellungsAnfrageDTO}.
 */
export function useSysAdminAnfragen(enabled: boolean = true) {
  return useQuery<KursErstellungsAnfrageDTO[]>({
    queryKey: SYSADMIN_ANFRAGEN_QUERY_KEY,
    queryFn: async () => {
      const res = await fetchClient("/systemadmin/anfragen");
      return res.json() as Promise<KursErstellungsAnfrageDTO[]>;
    },
    enabled,
    staleTime: 30_000,
  });
}
