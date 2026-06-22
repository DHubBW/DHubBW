/**
 * TanStack Query mutations for tag CRUD on a file.
 *
 * Three mutations are bundled here because they share the same set
 * of cache invalidations:
 *
 * - {@link useTagCreate} — create a new global tag (KURSADMIN /
 *   SYSTEMADMIN). The created {@link TagDTO} is returned.
 * - {@link useDateiTagAttach} — attach an existing tag to a file
 *   (uploader / KURSADMIN of folder / SYSTEMADMIN).
 * - {@link useDateiTagDetach} — detach a tag from a file (same
 *   authorisation as attach).
 *
 * Errors surface as the standard {@link ApiError} (400/403/404/409/0).
 */
import { useMutation, useQueryClient } from "@tanstack/react-query";

import { fetchClient } from "@/lib/api/fetchClient";
import type { CreateTagRequestDTO, TagDTO } from "@/lib/api/types.gen";

import { DATEIEN_LISTE_QUERY_KEY } from "./useDateienListe";
import { TAGS_QUERY_KEY } from "./useTags";

/**
 * Create-tag mutation.
 */
export function useTagCreate() {
  const queryClient = useQueryClient();
  return useMutation<TagDTO, Error, { name: string }>({
    meta: { suppressGlobalErrorToast: true },
    mutationFn: async ({ name }) => {
      const body: CreateTagRequestDTO = { name };
      const res = await fetchClient(`/student/tags`, {
        method: "POST",
        body: JSON.stringify(body),
      });
      return (await res.json()) as TagDTO;
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: TAGS_QUERY_KEY });
    },
  });
}

/** Input for attach / detach. */
export interface DateiTagInput {
  dateiId: string;
  tagId: string;
  /** Folder UUID for cache invalidation; `null` for root view. */
  ordnerId: string | null;
}

/**
 * Attach an existing tag to a file.
 */
export function useDateiTagAttach() {
  const queryClient = useQueryClient();
  return useMutation<void, Error, DateiTagInput>({
    meta: { suppressGlobalErrorToast: true },
    mutationFn: async ({ dateiId, tagId }) => {
      await fetchClient(`/student/dateien/${dateiId}/tags/${tagId}`, {
        method: "PUT",
      });
    },
    onSuccess: (_data, input) => {
      void queryClient.invalidateQueries({
        queryKey: DATEIEN_LISTE_QUERY_KEY(input.ordnerId),
      });
    },
  });
}

/**
 * Detach a tag from a file.
 */
export function useDateiTagDetach() {
  const queryClient = useQueryClient();
  return useMutation<void, Error, DateiTagInput>({
    meta: { suppressGlobalErrorToast: true },
    mutationFn: async ({ dateiId, tagId }) => {
      await fetchClient(`/student/dateien/${dateiId}/tags/${tagId}`, {
        method: "DELETE",
      });
    },
    onSuccess: (_data, input) => {
      void queryClient.invalidateQueries({
        queryKey: DATEIEN_LISTE_QUERY_KEY(input.ordnerId),
      });
    },
  });
}

/**
 * Permanently delete a tag from the platform — SYSTEMADMIN-only.
 *
 * <p>Backed by `DELETE /student/tags/{tagId}`. On success the tag
 * row is removed and every {@code datei_tag} link that pointed at
 * it is cleaned up by the backend; we therefore invalidate both
 * the global tag list and every file list (the cached
 * {@link DateiDTO}s carry stale tag chips otherwise).</p>
 *
 * @returns the TanStack-Query mutation handle.
 */
export function useTagDelete() {
  const queryClient = useQueryClient();
  return useMutation<void, Error, { tagId: string }>({
    meta: { suppressGlobalErrorToast: true },
    mutationFn: async ({ tagId }) => {
      await fetchClient(`/student/tags/${tagId}`, {
        method: "DELETE",
      });
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: TAGS_QUERY_KEY });
      // Every cached file list might carry the now-deleted tag —
      // invalidate the whole `dateien` namespace so the chips clear
      // immediately.
      void queryClient.invalidateQueries({ queryKey: ["dateien"] });
    },
  });
}
