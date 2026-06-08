/**
 * Fetches the meal plan for next week in a single API call.
 *
 * The result is cached for 5 minutes (`staleTime: 5 * 60_000`) so that
 * switching between weekday tabs triggers no additional requests.
 */
import { useQuery } from "@tanstack/react-query";

import { fetchClient } from "@/lib/api/fetchClient";
import type { WeekMealsMap } from "@/lib/api/types.gen";

export const MENSA_NEXT_WEEK_QUERY_KEY = ["mensa", "next-week"] as const;

/**
 * Queries `GET /student/meals/next-week`.
 *
 * @returns TanStack Query result containing a {@link WeekMealsMap}.
 */
export function useMensaNextWeek() {
  return useQuery<WeekMealsMap>({
    queryKey: MENSA_NEXT_WEEK_QUERY_KEY,
    queryFn: async () => {
      const res = await fetchClient("/student/meals/next-week");
      return res.json() as Promise<WeekMealsMap>;
    },
    staleTime: 5 * 60_000,
  });
}
