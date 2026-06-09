/**
 * Fetches all approved members of the calling course admin's course.
 *
 * Wraps `GET /kursadmin/mitglieder` — requires KURSADMIN role.
 */
import { useQuery } from "@tanstack/react-query";

import { fetchClient } from "@/lib/api/fetchClient";
import type { KursmemberDTO } from "@/lib/api/types.gen";

export const KURSADMIN_MITGLIEDER_QUERY_KEY = [
  "kursadmin",
  "mitglieder",
] as const;

/**
 * Queries `GET /kursadmin/mitglieder`.
 *
 * @param enabled - when `false`, the query is paused. Default `true`.
 * @returns TanStack Query result containing {@link KursmemberDTO}[].
 */
export function useKursAdminMitglieder(enabled: boolean = true) {
  return useQuery<KursmemberDTO[]>({
    queryKey: KURSADMIN_MITGLIEDER_QUERY_KEY,
    queryFn: async () => {
      const res = await fetchClient("/kursadmin/mitglieder");
      return res.json() as Promise<KursmemberDTO[]>;
    },
    enabled,
    staleTime: 30_000,
  });
}
