/**
 * @deprecated Dead code — kept only as a placeholder until
 * the next cleanup. Use {@link useAddKursadminBySysAdmin} from
 * `@/hooks/useKursadminBySysAdmin` instead.
 *
 * This file shadows the same export name from
 * `useKursadminBySysAdmin.ts`. Nothing imports this module
 * (`grep -rn "from .@/hooks/useAddKursadminBySysAdmin." src/` is
 * empty), and the path it calls
 * (`PATCH /kursadmin/mitglieder/add-kursadmin/{studentId}`) is
 * the KURSADMIN-scoped endpoint that requires the caller to be
 * an approved member of a course — i.e. it would 404 for a
 * SYSTEMADMIN without a course assignment, which is exactly the
 * bug the dedicated SA endpoint
 * `PATCH /systemadmin/kurse/{kursId}/kursadmin/{studentId}/add`
 * was introduced to avoid (see {@link useKursadminBySysAdmin}).
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
