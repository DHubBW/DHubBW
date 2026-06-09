/**
 * Mutation hooks for system admin actions on course-creation requests
 * and role management.
 *
 * Covers:
 * - Approve / decline a `KursErstellungsAnfrage`
 * - Add / remove SYSTEMADMIN role from a student
 * - Delete a course
 */
import { useQueryClient } from "@tanstack/react-query";
import { useCallback, useState } from "react";

import { ApiError } from "@/lib/api/errors";
import { fetchClient } from "@/lib/api/fetchClient";
import { SYSADMIN_ANFRAGEN_QUERY_KEY } from "@/hooks/useSysAdminAnfragen";
import { SYSADMIN_KURSE_QUERY_KEY } from "@/hooks/useSysAdminKurse";
import { SYSADMIN_STUDENTEN_QUERY_KEY } from "@/hooks/useSysAdminStudenten";
import { SYSADMIN_STATS_QUERY_KEY } from "@/hooks/useSysAdminStats";

// ─── approve anfrage ──────────────────────────────────────────────────────────

/**
 * Approves a pending course-creation request.
 *
 * Sends `PATCH /systemadmin/anfragen/{id}/approve`, then invalidates the
 * anfragen and stats query caches on success.
 *
 * @returns `{ isPending, approveAnfrage }`
 */
export function useApproveAnfrage() {
  const queryClient = useQueryClient();
  const [isPending, setIsPending] = useState(false);

  const approveAnfrage = useCallback(
    async (anfrageId: string) => {
      setIsPending(true);
      try {
        await fetchClient(`/systemadmin/anfragen/${anfrageId}/approve`, {
          method: "PATCH",
        });
        await Promise.all([
          queryClient.invalidateQueries({
            queryKey: SYSADMIN_ANFRAGEN_QUERY_KEY,
          }),
          queryClient.invalidateQueries({ queryKey: SYSADMIN_STATS_QUERY_KEY }),
          queryClient.invalidateQueries({ queryKey: SYSADMIN_KURSE_QUERY_KEY }),
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

  return { isPending, approveAnfrage };
}

// ─── decline anfrage ──────────────────────────────────────────────────────────

/**
 * Declines (deletes) a pending course-creation request.
 *
 * Sends `DELETE /systemadmin/anfragen/{id}`, then invalidates the
 * anfragen and stats query caches on success.
 *
 * @returns `{ isPending, declineAnfrage }`
 */
export function useDeclineAnfrage() {
  const queryClient = useQueryClient();
  const [isPending, setIsPending] = useState(false);

  const declineAnfrage = useCallback(
    async (anfrageId: string) => {
      setIsPending(true);
      try {
        await fetchClient(`/systemadmin/anfragen/${anfrageId}`, {
          method: "DELETE",
        });
        await Promise.all([
          queryClient.invalidateQueries({
            queryKey: SYSADMIN_ANFRAGEN_QUERY_KEY,
          }),
          queryClient.invalidateQueries({ queryKey: SYSADMIN_STATS_QUERY_KEY }),
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

  return { isPending, declineAnfrage };
}

// ─── delete kurs ─────────────────────────────────────────────────────────────

/**
 * Deletes a course with full cascade.
 *
 * Sends `DELETE /systemadmin/kurse/{kursId}`, then invalidates kurse,
 * studenten and stats caches on success.
 *
 * @returns `{ isPending, deleteKurs }`
 */
export function useDeleteKurs() {
  const queryClient = useQueryClient();
  const [isPending, setIsPending] = useState(false);

  const deleteKurs = useCallback(
    async (kursId: string) => {
      setIsPending(true);
      try {
        await fetchClient(`/systemadmin/kurse/${kursId}`, {
          method: "DELETE",
        });
        // Remove the mitglieder cache for the deleted course directly
        // instead of invalidating — invalidating would trigger a 404 refetch.
        queryClient.removeQueries({
          queryKey: ["systemadmin", "kurse", kursId, "mitglieder"],
        });
        await Promise.all([
          queryClient.invalidateQueries({ queryKey: SYSADMIN_KURSE_QUERY_KEY }),
          queryClient.invalidateQueries({
            queryKey: SYSADMIN_STUDENTEN_QUERY_KEY,
          }),
          queryClient.invalidateQueries({ queryKey: SYSADMIN_STATS_QUERY_KEY }),
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

  return { isPending, deleteKurs };
}

// ─── add sysadmin ─────────────────────────────────────────────────────────────

/**
 * Promotes a student to SYSTEMADMIN.
 *
 * Sends `PATCH /systemadmin/studenten/{studentId}/add-sysadmin`, then
 * invalidates the studenten cache on success.
 *
 * @returns `{ isPending, addSysAdmin }`
 */
export function useAddSysAdmin() {
  const queryClient = useQueryClient();
  const [isPending, setIsPending] = useState(false);

  const addSysAdmin = useCallback(
    async (studentId: string) => {
      setIsPending(true);
      try {
        await fetchClient(`/systemadmin/studenten/${studentId}/add-sysadmin`, {
          method: "PATCH",
        });
        await Promise.all([
          queryClient.invalidateQueries({
            queryKey: SYSADMIN_STUDENTEN_QUERY_KEY,
          }),
          queryClient.invalidateQueries({ queryKey: SYSADMIN_STATS_QUERY_KEY }),
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

  return { isPending, addSysAdmin };
}

// ─── remove sysadmin ─────────────────────────────────────────────────────────

/**
 * Revokes the SYSTEMADMIN role from a student.
 *
 * Sends `PATCH /systemadmin/studenten/{studentId}/remove-sysadmin`, then
 * invalidates the studenten cache on success.
 *
 * @returns `{ isPending, removeSysAdmin }`
 */
export function useRemoveSysAdmin() {
  const queryClient = useQueryClient();
  const [isPending, setIsPending] = useState(false);

  const removeSysAdmin = useCallback(
    async (studentId: string) => {
      setIsPending(true);
      try {
        await fetchClient(
          `/systemadmin/studenten/${studentId}/remove-sysadmin`,
          { method: "PATCH" },
        );
        await Promise.all([
          queryClient.invalidateQueries({
            queryKey: SYSADMIN_STUDENTEN_QUERY_KEY,
          }),
          queryClient.invalidateQueries({ queryKey: SYSADMIN_STATS_QUERY_KEY }),
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

  return { isPending, removeSysAdmin };
}
