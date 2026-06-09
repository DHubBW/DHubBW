/**
 * Fetches course members with roles for a system admin.
 *
 * Wraps `GET /systemadmin/kurse/{kursId}/mitglieder` — requires SYSTEMADMIN.
 */
import { useQuery } from "@tanstack/react-query";

import { fetchClient } from "@/lib/api/fetchClient";
import type { KursMitgliedRolleDTO } from "@/lib/api/types.gen";

/**
 * Returns the TanStack query key for a given course's member list.
 *
 * @param kursId - UUID string of the course.
 */
export function sysAdminKursMitgliederQueryKey(kursId: string) {
  return ["systemadmin", "kurse", kursId, "mitglieder"] as const;
}

/**
 * Queries `GET /systemadmin/kurse/{kursId}/mitglieder`.
 *
 * @param kursId  - UUID of the course to query. Pass `null` to pause.
 * @returns TanStack Query result containing an array of
 *   {@link KursMitgliedRolleDTO}.
 */
export function useSysAdminKursMitglieder(kursId: string | null) {
  return useQuery<KursMitgliedRolleDTO[]>({
    queryKey: sysAdminKursMitgliederQueryKey(kursId ?? ""),
    queryFn: async () => {
      const res = await fetchClient(`/systemadmin/kurse/${kursId}/mitglieder`);
      return res.json() as Promise<KursMitgliedRolleDTO[]>;
    },
    enabled: kursId !== null,
    staleTime: 30_000,
  });
}
