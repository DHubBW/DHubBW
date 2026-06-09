/**
 * Mutation hook for a SYSTEMADMIN to revoke a KURSADMIN role from a student.
 *
 * Sends `PATCH /kursadmin/mitglieder/remove-kursadmin/{studentId}`.
 * SYSTEMADMIN inherits KURSADMIN authority via the role hierarchy configured
 * in Spring Security's {@code CustomUserDetailsService}.
 *
 * On success the kurse and mitglieder caches are invalidated.
 */
import { useQueryClient } from "@tanstack/react-query";
import { useCallback, useState } from "react";

import { ApiError } from "@/lib/api/errors";
import { fetchClient } from "@/lib/api/fetchClient";
import { SYSADMIN_KURSE_QUERY_KEY } from "@/hooks/useSysAdminKurse";
import { SYSADMIN_STUDENTEN_QUERY_KEY } from "@/hooks/useSysAdminStudenten";

/**
 * Revokes the KURSADMIN role from a student within a course.
 *
 * Uses `PATCH /kursadmin/mitglieder/remove-kursadmin/{studentId}`.
 *
 * @returns `{ isPending, removeKursadmin }`
 */
export function useRemoveKursadminBySysAdmin() {
  const queryClient = useQueryClient();
  const [isPending, setIsPending] = useState(false);

  const removeKursadmin = useCallback(
    async (studentId: string) => {
      setIsPending(true);
      try {
        await fetchClient(
          `/kursadmin/mitglieder/remove-kursadmin/${studentId}`,
          { method: "PATCH" },
        );
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

  return { isPending, removeKursadmin };
}
