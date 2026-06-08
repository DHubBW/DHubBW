/**
 * Fetches the meal plan for the current week in a single API call.
 *
 * The result is cached for 5 minutes (`staleTime: 5 * 60_000`) so that
 * switching between weekday tabs triggers no additional requests.
 */
import { useQuery } from "@tanstack/react-query";

import { fetchClient } from "@/lib/api/fetchClient";
import type { WeekMealsMap } from "@/lib/api/types.gen";

export const MENSA_THIS_WEEK_QUERY_KEY = ["mensa", "current-week"] as const;

/**
 * Queries `GET /student/meals/current-week`.
 *
 * @returns TanStack Query result containing a {@link WeekMealsMap}.
 */
export function useMensaThisWeek() {
  return useQuery<WeekMealsMap>({
    queryKey: MENSA_THIS_WEEK_QUERY_KEY,
    queryFn: async () => {
      const res = await fetchClient("/student/meals/current-week");
      return res.json() as Promise<WeekMealsMap>;
    },
    staleTime: 5 * 60_000,
  });
}
