/**
 * Global error handler for all unhandled TanStack-Query errors.
 *
 * Renders a sonner-native toast (bottom-right) whenever a Query or
 * Mutation settles with an error that has not been caught by the
 * caller. Catches:
 *
 * - `ApiError(401)` → silently ignored (handled by the global
 *   401-handler in AuthContext which redirects to login).
 * - `ApiError(409)` → Conflict — shown as-is (last-admin case etc.)
 * - `ApiError(0)`   → Network error
 * - All other `ApiError` → shown with status code
 * - Unknown errors  → generic fallback message
 *
 * Mutations that opt out via `meta.suppressGlobalErrorToast === true`
 * are skipped, so callers that already render a tailored toast in
 * their own `onError` (e.g. file upload with size-aware messages)
 * don't get a second generic toast on top.
 *
 * The hook installs `MutationCache` and `QueryCache` handlers on the
 * project-wide queryClient **once** on mount via useEffect. It is
 * intended to run exactly once inside `RootLayout`.
 */
import { useEffect } from "react";
import { toast } from "sonner-native";

import { ApiError } from "@/lib/api/errors";
import { queryClient } from "@/lib/api/queryClient";
import { t } from "@/lib/i18n/t";

function toastForError(err: unknown): void {
  if (err instanceof ApiError) {
    // 401 is handled by the global setUnauthorizedHandler → no toast
    if (err.status === 401) return;
    if (err.status === 0) {
      toast.error(t("error.network", "Netzwerkfehler – Server nicht erreichbar"));
      return;
    }
    // Prefer body message over generic status text
    const body = err.body;
    const bodyMsg =
      typeof body === "string" && body.length > 0
        ? body
        : typeof body === "object" &&
            body !== null &&
            typeof (body as Record<string, unknown>).message === "string"
          ? ((body as Record<string, unknown>).message as string)
          : null;
    toast.error(bodyMsg ?? err.message ?? t("error.unknown", "Unbekannter Fehler"));
    return;
  }
  if (err instanceof Error) {
    toast.error(err.message || t("error.unknown", "Unbekannter Fehler"));
    return;
  }
  toast.error(t("error.unknown", "Unbekannter Fehler"));
}

/**
 * Installs global error-toast handlers on the shared `queryClient`.
 *
 * Must be rendered inside a `QueryClientProvider` and the `Toaster`
 * must be present in the tree (guaranteed by `ThemedStack`).
 */
export function useGlobalQueryErrorToast(): void {
  useEffect(() => {
    // Patch mutationCache — fires for every failed mutation that
    // didn't opt out via meta.suppressGlobalErrorToast.
    const mutationCache = queryClient.getMutationCache();
    const unsubMutation = mutationCache.subscribe((event) => {
      if (event.type === "updated" && event.mutation.state.status === "error") {
        const meta = event.mutation.options.meta as
          | { suppressGlobalErrorToast?: boolean }
          | undefined;
        if (meta?.suppressGlobalErrorToast) return;
        toastForError(event.mutation.state.error);
      }
    });

    // Patch queryCache — fires for background refetch failures that
    // callers are not directly awaiting.
    const queryCache = queryClient.getQueryCache();
    const unsubQuery = queryCache.subscribe((event) => {
      if (event.type === "updated" && event.query.state.status === "error") {
        toastForError(event.query.state.error);
      }
    });

    return () => {
      unsubMutation();
      unsubQuery();
    };
  }, []);
}
