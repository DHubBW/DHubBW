/**
 * Mutation hook for a SYSTEMADMIN to assign a KURSADMIN role to a student.
 *
 * Sends `PATCH /kursadmin/mitglieder/add-kursadmin/{studentId}`.
 * Requires the caller to be authenticated with SYSTEMADMIN (which holds
 * KURSADMIN authority via the role hierarchy in Spring Security).
 *
 * On success the kurse and studenten caches are invalidated so the
 * member drawer and student list refresh automatically.
 */
import { useQueryClient } from "@tanstack/react-query";
import { useCallback, useState } from "react";

import { ApiError } from "@/lib/api/errors";
import { fetchClient } from "@/lib/api/fetchClient";
import { SYSADMIN_KURSE_QUERY_KEY } from "@/hooks/useSysAdminKurse";
import { SYSADMIN_STUDENTEN_QUERY_KEY } from "@/hooks/useSysAdminStudenten";

/**
 * Assigns the KURSADMIN role to a student within a course.
 *
 * Uses the `PATCH /kursadmin/mitglieder/add-kursadmin/{studentId}` endpoint.
 * The SYSTEMADMIN inherits the KURSADMIN role via Spring Security role
 * hierarchy, so the same endpoint works.
 *
 * @returns `{ isPending, addKursadmin }`
 */
export function useAddKursadminBySysAdmin() {
  const queryClient = useQueryClient();
  const [isPending, setIsPending] = useState(false);

  const addKursadmin = useCallback(
    async (studentId: string) => {
      setIsPending(true);
      try {
        await fetchClient(`/kursadmin/mitglieder/add-kursadmin/${studentId}`, {
          method: "PATCH",
        });
        await Promise.all([
          queryClient.invalidateQueries({ queryKey: SYSADMIN_KURSE_QUERY_KEY }),
          queryClient.invalidateQueries({
            queryKey: SYSADMIN_STUDENTEN_QUERY_KEY,
          }),
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
