/**
 * Fetches aggregated stats for a course admin from the dashboard.
 *
 * Wraps `GET /kursadmin/mitglieder/stats` — requires KURSADMIN role.
 * Returns `null` (not an error) when the caller holds SYSTEMADMIN but has
 * no course assignment (404), so the dashboard can decide whether to show
 * the section or skip it gracefully.
 */
import { useQuery } from "@tanstack/react-query";

import { ApiError } from "@/lib/api/errors";
import { fetchClient } from "@/lib/api/fetchClient";
import type { KursAdminStatsDTO } from "@/lib/api/types.gen";

export const KURSADMIN_STATS_QUERY_KEY = ["kursadmin", "stats"] as const;

/**
 * Queries `GET /kursadmin/mitglieder/stats`.
 *
 * A 404 response (admin has no course assignment) resolves to `null` instead
 * of an error so callers can hide the section without error-branch handling.
 *
 * @param enabled - when `false`, the query is paused. Default `true`.
 * @returns TanStack Query result containing {@link KursAdminStatsDTO} or `null`.
 */
export function useKursAdminStats(enabled: boolean = true) {
  return useQuery<KursAdminStatsDTO | null>({
    queryKey: KURSADMIN_STATS_QUERY_KEY,
    queryFn: async () => {
      try {
        const res = await fetchClient("/kursadmin/mitglieder/stats");
        return res.json() as Promise<KursAdminStatsDTO>;
      } catch (err) {
        if (err instanceof ApiError && err.status === 403) {
          return null;
        }
        throw err;
      }
    },
    enabled,
    staleTime: 60_000,
  });
}
