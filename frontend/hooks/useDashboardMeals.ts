/**
 * Fetches today's mensa meals for the dashboard preview.
 *
 * Wraps `GET /student/dashboard/meals/today` with a 1-minute stale window.
 */
import { useQuery } from "@tanstack/react-query";

import { fetchClient } from "@/lib/api/fetchClient";
import type { MensaDTO } from "@/lib/api/types.gen";

export const DASHBOARD_MEALS_QUERY_KEY = [
  "dashboard",
  "meals",
  "today",
] as const;

/**
 * Queries `GET /student/dashboard/meals/today`.
 *
 * @returns TanStack Query result containing an array of today's {@link MensaDTO}.
 */
export function useDashboardMeals() {
  return useQuery<MensaDTO[]>({
    queryKey: DASHBOARD_MEALS_QUERY_KEY,
    queryFn: async () => {
      const res = await fetchClient("/student/dashboard/meals/today");
      return res.json() as Promise<MensaDTO[]>;
    },
    staleTime: 60_000,
  });
}
