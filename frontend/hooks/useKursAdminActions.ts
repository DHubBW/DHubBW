/**
 * Mutation hooks for course admin actions:
 * approve / decline enrollment requests, resign as KURSADMIN,
 * add / remove KURSADMIN role, remove a course member.
 */
import { useQueryClient } from "@tanstack/react-query";
import { useCallback, useState } from "react";

import { KURSADMIN_ANFRAGEN_QUERY_KEY } from "@/hooks/useKursAdminAnfragen";
import { KURSADMIN_MITGLIEDER_QUERY_KEY } from "@/hooks/useKursAdminMitglieder";
import { KURSADMIN_STATS_QUERY_KEY } from "@/hooks/useKursAdminStats";
import { ApiError } from "@/lib/api/errors";
import { fetchClient } from "@/lib/api/fetchClient";

// ─── approve enrollment request ───────────────────────────────────────────────

/**
 * Approves a pending enrollment request.
 *
 * Sends `PATCH /kursadmin/anfragen/{id}/approve`, then invalidates
 * anfragen and stats caches on success.
 *
 * @returns `{ isPending, approveAnfrage }`
 */
export function useKursAdminApproveAnfrage() {
  const queryClient = useQueryClient();
  const [isPending, setIsPending] = useState(false);

  const approveAnfrage = useCallback(
    async (anfrageId: string) => {
      setIsPending(true);
      try {
        await fetchClient(`/kursadmin/anfragen/${anfrageId}/approve`, {
          method: "PATCH",
        });
        await Promise.all([
          queryClient.invalidateQueries({
            queryKey: KURSADMIN_ANFRAGEN_QUERY_KEY,
          }),
          queryClient.invalidateQueries({
            queryKey: KURSADMIN_STATS_QUERY_KEY,
          }),
          queryClient.invalidateQueries({
            queryKey: KURSADMIN_MITGLIEDER_QUERY_KEY,
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

  return { isPending, approveAnfrage };
}

// ─── decline enrollment request ───────────────────────────────────────────────

/**
 * Declines (deletes) a pending enrollment request.
 *
 * Sends `DELETE /kursadmin/anfragen/{id}`, then invalidates
 * anfragen and stats caches on success.
 *
 * @returns `{ isPending, declineAnfrage }`
 */
export function useKursAdminDeclineAnfrage() {
  const queryClient = useQueryClient();
  const [isPending, setIsPending] = useState(false);

  const declineAnfrage = useCallback(
    async (anfrageId: string) => {
      setIsPending(true);
      try {
        await fetchClient(`/kursadmin/anfragen/${anfrageId}`, {
          method: "DELETE",
        });
        await Promise.all([
          queryClient.invalidateQueries({
            queryKey: KURSADMIN_ANFRAGEN_QUERY_KEY,
          }),
          queryClient.invalidateQueries({
            queryKey: KURSADMIN_STATS_QUERY_KEY,
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

  return { isPending, declineAnfrage };
}

// ─── resign as KURSADMIN ──────────────────────────────────────────────────────

/**
 * Gives up the calling admin's KURSADMIN role.
 *
 * Sends `PATCH /kursadmin/mitglieder/resign`.
 * Throws `ApiError(409)` when the admin is the only KURSADMIN.
 *
 * @returns `{ isPending, resign }`
 */
export function useKursAdminResign() {
  const [isPending, setIsPending] = useState(false);

  const resign = useCallback(async () => {
    setIsPending(true);
    try {
      await fetchClient("/kursadmin/mitglieder/resign", { method: "PATCH" });
    } catch (err) {
      if (err instanceof ApiError) throw err;
      throw new ApiError(0, "Network error");
    } finally {
      setIsPending(false);
    }
  }, []);

  return { isPending, resign };
}

// ─── add KURSADMIN role ───────────────────────────────────────────────────────

/**
 * Promotes a STUDENT course member to KURSADMIN.
 *
 * Sends `PATCH /kursadmin/mitglieder/add-kursadmin/{studentId}`,
 * then invalidates the mitglieder cache on success.
 *
 * @returns `{ isPending, addKursadmin }`
 */
export function useKursAdminAddKursadmin() {
  const queryClient = useQueryClient();
  const [isPending, setIsPending] = useState(false);

  const addKursadmin = useCallback(
    async (studentId: string) => {
      setIsPending(true);
      try {
        await fetchClient(
          `/kursadmin/mitglieder/add-kursadmin/${studentId}`,
          { method: "PATCH" },
        );
        await queryClient.invalidateQueries({
          queryKey: KURSADMIN_MITGLIEDER_QUERY_KEY,
        });
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

// ─── remove KURSADMIN role ────────────────────────────────────────────────────

/**
 * Revokes the KURSADMIN role from a course member.
 *
 * Sends `PATCH /kursadmin/mitglieder/remove-kursadmin/{studentId}`,
 * then invalidates the mitglieder cache on success.
 * Throws `ApiError(409)` when the target is the only KURSADMIN.
 *
 * @returns `{ isPending, removeKursadmin }`
 */
export function useKursAdminRemoveKursadmin() {
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
        await queryClient.invalidateQueries({
          queryKey: KURSADMIN_MITGLIEDER_QUERY_KEY,
        });
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

// ─── remove course member ──────────────────────────────────────────────────────

/**
 * Removes a STUDENT from the course.
 *
 * Sends `DELETE /kursadmin/mitglieder/{studentId}`, then invalidates
 * mitglieder and stats caches on success.
 * Throws `ApiError(409)` when the target holds an elevated role.
 *
 * @returns `{ isPending, removeMember }`
 */
export function useKursAdminRemoveMember() {
  const queryClient = useQueryClient();
  const [isPending, setIsPending] = useState(false);

  const removeMember = useCallback(
    async (studentId: string) => {
      setIsPending(true);
      try {
        await fetchClient(`/kursadmin/mitglieder/${studentId}`, {
          method: "DELETE",
        });
        await Promise.all([
          queryClient.invalidateQueries({
            queryKey: KURSADMIN_MITGLIEDER_QUERY_KEY,
          }),
          queryClient.invalidateQueries({
            queryKey: KURSADMIN_STATS_QUERY_KEY,
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

  return { isPending, removeMember };
}
