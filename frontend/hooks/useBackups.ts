/**
 * TanStack Query hooks for the SYSTEMADMIN backup feature.
 *
 * <p>Backed by:</p>
 * <ul>
 *   <li>{@code GET    /systemadmin/backups}            — list</li>
 *   <li>{@code GET    /systemadmin/backups/latest}     — last run</li>
 *   <li>{@code POST   /systemadmin/backups}            — manual trigger</li>
 *   <li>{@code POST   /systemadmin/backups/{id}/restore} — restore</li>
 *   <li>{@code DELETE /systemadmin/backups/{id}}       — manual delete</li>
 * </ul>
 *
 * <p>Both restore and manual delete enforce a two-fold confirmation
 * count on the backend (mirrored by the admin UI's two-step
 * arm-then-confirm button). The retention sweep that auto-removes
 * 14-day-old backups runs server-side and bypasses this gate.</p>
 */
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { ApiError } from "@/lib/api/errors";
import { fetchClient } from "@/lib/api/fetchClient";
import type { BackupDTO, RestoreBackupRequestDTO } from "@/lib/api/types.gen";

export const BACKUPS_QUERY_KEY = ["backups", "list"] as const;
export const BACKUP_LATEST_QUERY_KEY = ["backups", "latest"] as const;

/**
 * Fetches the list of backups, newest first.
 *
 * @returns TanStack Query result.
 */
export function useBackups() {
  return useQuery<BackupDTO[]>({
    queryKey: BACKUPS_QUERY_KEY,
    queryFn: async () => {
      const res = await fetchClient(`/systemadmin/backups`);
      return (await res.json()) as BackupDTO[];
    },
    staleTime: 30_000,
  });
}

/**
 * Fetches the most recent backup. Resolves to `null` on a 204
 * "no content" response so the UI can render the "no backup yet"
 * placeholder without an extra branch.
 *
 * @returns TanStack Query result.
 */
export function useLatestBackup() {
  return useQuery<BackupDTO | null>({
    queryKey: BACKUP_LATEST_QUERY_KEY,
    queryFn: async () => {
      try {
        const res = await fetchClient(`/systemadmin/backups/latest`);
        if (res.status === 204) return null;
        return (await res.json()) as BackupDTO;
      } catch (err) {
        if (err instanceof ApiError && err.status === 0) {
          return null;
        }
        throw err;
      }
    },
    staleTime: 30_000,
  });
}

/**
 * Manual-backup trigger.
 *
 * @returns TanStack-Query mutation handle.
 */
export function useBackupCreate() {
  const queryClient = useQueryClient();
  return useMutation<BackupDTO, Error, void>({
    meta: { suppressGlobalErrorToast: true },
    mutationFn: async () => {
      const res = await fetchClient(`/systemadmin/backups`, {
        method: "POST",
      });
      return (await res.json()) as BackupDTO;
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: BACKUPS_QUERY_KEY });
      void queryClient.invalidateQueries({
        queryKey: BACKUP_LATEST_QUERY_KEY,
      });
    },
  });
}

/** Input shape for the restore mutation. */
export interface RestoreBackupInput {
  /** UUID of the backup to restore. */
  backupId: string;
  /**
   * Caller's confirmation count. Must be at least 2 — the admin UI
   * forces a double-confirm dialog so the value is always >= 2 when
   * this hook is invoked, but the value is checked by the backend
   * regardless.
   */
  confirmationCount: number;
}

/**
 * Restore-backup mutation.
 *
 * @returns TanStack-Query mutation handle.
 */
export function useBackupRestore() {
  const queryClient = useQueryClient();
  return useMutation<void, Error, RestoreBackupInput>({
    meta: { suppressGlobalErrorToast: true },
    mutationFn: async ({ backupId, confirmationCount }) => {
      const body: RestoreBackupRequestDTO = { confirmationCount };
      await fetchClient(`/systemadmin/backups/${backupId}/restore`, {
        method: "POST",
        body: JSON.stringify(body),
      });
    },
    onSuccess: () => {
      // Invalidate everything that the restore could have touched —
      // the file list, ratings, comments, tags.
      void queryClient.invalidateQueries({ queryKey: ["dateien"] });
      void queryClient.invalidateQueries({ queryKey: ["bewertung"] });
      void queryClient.invalidateQueries({ queryKey: ["kommentare"] });
      void queryClient.invalidateQueries({ queryKey: ["tags"] });
    },
  });
}

/** Input shape for the manual delete-backup mutation. */
export interface DeleteBackupInput {
  /** UUID of the backup to delete. */
  backupId: string;
  /**
   * Caller's confirmation count. Must be at least 2 — the admin UI
   * forces a two-step button so the value is always >= 2 here, but
   * the backend re-checks regardless.
   */
  confirmationCount: number;
}

/**
 * Manual-delete-backup mutation. The 14-day retention sweep runs
 * server-side and bypasses this hook.
 *
 * @returns TanStack-Query mutation handle.
 */
export function useBackupDelete() {
  const queryClient = useQueryClient();
  return useMutation<void, Error, DeleteBackupInput>({
    meta: { suppressGlobalErrorToast: true },
    mutationFn: async ({ backupId, confirmationCount }) => {
      const body: RestoreBackupRequestDTO = { confirmationCount };
      await fetchClient(`/systemadmin/backups/${backupId}`, {
        method: "DELETE",
        body: JSON.stringify(body),
      });
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: BACKUPS_QUERY_KEY });
      void queryClient.invalidateQueries({
        queryKey: BACKUP_LATEST_QUERY_KEY,
      });
    },
  });
}
