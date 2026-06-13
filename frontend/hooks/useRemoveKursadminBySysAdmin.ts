/**
 * @deprecated Dead code — kept only as a placeholder until the
 * next cleanup. Use {@link useRemoveKursadminBySysAdmin} from
 * `@/hooks/useKursadminBySysAdmin` instead. The path this module
 * calls (`PATCH /kursadmin/mitglieder/remove-kursadmin/{studentId}`)
 * is KURSADMIN-scoped — a SYSTEMADMIN without a course assignment
 * would 404 there. The dedicated SA endpoint
 * `PATCH /systemadmin/kurse/{kursId}/kursadmin/{studentId}/remove`
 * lives in {@link useKursadminBySysAdmin} and is the one the
 * Verwaltung screen actually imports.
 */
import { useQueryClient } from "@tanstack/react-query";
import { useCallback, useState } from "react";

import { ApiError } from "@/lib/api/errors";
import { fetchClient } from "@/lib/api/fetchClient";
import { SYSADMIN_KURSE_QUERY_KEY } from "@/hooks/useSysAdminKurse";
import { SYSADMIN_STUDENTEN_QUERY_KEY } from "@/hooks/useSysAdminStudenten";

/**
 * @deprecated Use the version exported from
 *     `@/hooks/useKursadminBySysAdmin` (signature
 *     `(kursId, studentId)`) instead. This implementation calls
 *     the wrong endpoint for the SYSTEMADMIN-without-own-course
 *     case.
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
