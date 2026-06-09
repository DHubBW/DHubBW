/**
 * Fetches pending enrollment requests for the calling course admin.
 *
 * Wraps `GET /kursadmin/anfragen` — requires KURSADMIN role.
 */
import { useQuery } from "@tanstack/react-query";

import { fetchClient } from "@/lib/api/fetchClient";
import type { KursanfrageDTO } from "@/lib/api/types.gen";

export const KURSADMIN_ANFRAGEN_QUERY_KEY = ["kursadmin", "anfragen"] as const;

/**
 * Queries `GET /kursadmin/anfragen`.
 *
 * @param enabled - when `false`, the query is paused. Default `true`.
 * @returns TanStack Query result containing {@link KursanfrageDTO}[].
 */
export function useKursAdminAnfragen(enabled: boolean = true) {
  return useQuery<KursanfrageDTO[]>({
    queryKey: KURSADMIN_ANFRAGEN_QUERY_KEY,
    queryFn: async () => {
      const res = await fetchClient("/kursadmin/anfragen");
      return res.json() as Promise<KursanfrageDTO[]>;
    },
    enabled,
    staleTime: 30_000,
  });
}
