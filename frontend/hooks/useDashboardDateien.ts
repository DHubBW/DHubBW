/**
 * Fetches the latest files visible to the authenticated user for the dashboard.
 *
 * Wraps `GET /student/dashboard/dateien/latest` — returns all files for
 * system admins, course-scoped files for students.
 * Returns a 403 when a student has no approved course enrollment.
 */
import { useQuery } from "@tanstack/react-query";

import { ApiError } from "@/lib/api/errors";
import { fetchClient } from "@/lib/api/fetchClient";
import type { DateiDTO } from "@/lib/api/types.gen";

export const DASHBOARD_DATEIEN_QUERY_KEY = [
  "dashboard",
  "dateien",
  "latest",
] as const;

/**
 * Queries `GET /student/dashboard/dateien/latest`.
 *
 * A 403 response (student has no course) resolves to an empty array instead
 * of an error so callers can show the NoAccess empty-state without special
 * error-branch logic.
 *
 * @returns TanStack Query result containing an array of {@link DateiDTO}.
 */
export function useDashboardDateien() {
  return useQuery<DateiDTO[]>({
    queryKey: DASHBOARD_DATEIEN_QUERY_KEY,
    queryFn: async () => {
      try {
        const res = await fetchClient("/student/dashboard/dateien/latest");
        return res.json() as Promise<DateiDTO[]>;
      } catch (err) {
        if (err instanceof ApiError && err.status === 403) {
          return [];
        }
        throw err;
      }
    },
    staleTime: 60_000,
  });
}
