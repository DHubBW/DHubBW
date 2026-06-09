/**
 * Mutation hooks for SYSTEMADMIN to manage KURSADMIN roles per course.
 *
 * Uses the dedicated SA endpoints:
 * - `PATCH /systemadmin/kurse/{kursId}/kursadmin/{studentId}/add`
 * - `PATCH /systemadmin/kurse/{kursId}/kursadmin/{studentId}/remove`
 *
 * These bypass the KURSADMIN-scoped endpoints that require the caller
 * to have an approved course enrollment — a SYSTEMADMIN without a
 * course membership would get 404 from those.
 */
import { useQueryClient } from "@tanstack/react-query";
import { useCallback, useState } from "react";

import { ApiError } from "@/lib/api/errors";
import { fetchClient } from "@/lib/api/fetchClient";
import { SYSADMIN_KURSE_QUERY_KEY } from "@/hooks/useSysAdminKurse";
import { SYSADMIN_STUDENTEN_QUERY_KEY } from "@/hooks/useSysAdminStudenten";
import { sysAdminKursMitgliederQueryKey } from "@/hooks/useSysAdminKursMitglieder";
import { PROFILE_QUERY_KEY } from "@/lib/auth/useProfile";

/**
 * Promotes a course member to KURSADMIN.
 *
 * @returns `{ isPending, addKursadmin(kursId, studentId) }`
 */
export function useAddKursadminBySysAdmin() {
  const queryClient = useQueryClient();
  const [isPending, setIsPending] = useState(false);

  const addKursadmin = useCallback(
    async (kursId: string, studentId: string) => {
      setIsPending(true);
      try {
        await fetchClient(
          `/systemadmin/kurse/${kursId}/kursadmin/${studentId}/add`,
          { method: "PATCH" },
        );
        await Promise.all([
          queryClient.invalidateQueries({
            queryKey: sysAdminKursMitgliederQueryKey(kursId),
          }),
          queryClient.invalidateQueries({ queryKey: SYSADMIN_KURSE_QUERY_KEY }),
          queryClient.invalidateQueries({
            queryKey: SYSADMIN_STUDENTEN_QUERY_KEY,
          }),
          // Refresh the SA's own profile in case role display is stale.
          queryClient.invalidateQueries({ queryKey: PROFILE_QUERY_KEY }),
        ]);
      } catch (err) {
        if (err instanceof ApiError) throw err;
        throw new ApiError(0, "Network error");
      } finally {
        setIsPending(false);
      }
    },
    [queryClient],
  );

  return { isPending, addKursadmin };
}

/**
 * Revokes KURSADMIN role from a course member.
 *
 * @returns `{ isPending, removeKursadmin(kursId, studentId) }`
 */
export function useRemoveKursadminBySysAdmin() {
  const queryClient = useQueryClient();
  const [isPending, setIsPending] = useState(false);

  const removeKursadmin = useCallback(
    async (kursId: string, studentId: string) => {
      setIsPending(true);
      try {
        await fetchClient(
          `/systemadmin/kurse/${kursId}/kursadmin/${studentId}/remove`,
          { method: "PATCH" },
        );
        await Promise.all([
          queryClient.invalidateQueries({
            queryKey: sysAdminKursMitgliederQueryKey(kursId),
          }),
          queryClient.invalidateQueries({ queryKey: SYSADMIN_KURSE_QUERY_KEY }),
          queryClient.invalidateQueries({
            queryKey: SYSADMIN_STUDENTEN_QUERY_KEY,
          }),
          // Refresh the SA's own profile in case role display is stale.
          queryClient.invalidateQueries({ queryKey: PROFILE_QUERY_KEY }),
        ]);
      } catch (err) {
        if (err instanceof ApiError) throw err;
        throw new ApiError(0, "Network error");
      } finally {
        setIsPending(false);
      }
    },
    [queryClient],
  );

  return { isPending, removeKursadmin };
}
