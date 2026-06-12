/**
 * TanStack Query mutation that triggers a manual mensa sync via
 * `POST /systemadmin/mensa/sync`.
 *
 * <p>Restricted to SYSTEMADMINs; the backend enforces the role via
 * {@code @PreAuthorize("hasRole('SYSTEMADMIN')")} on
 * {@code MensaSyncController}. On success we invalidate every
 * mensa-related query so the dashboard picks the freshly scraped
 * meals up without a manual refetch.</p>
 *
 * <p>The dialog/button surface renders its own status-aware toast,
 * so we suppress the global error toast.</p>
 */
import { useMutation, useQueryClient } from "@tanstack/react-query";

import { fetchClient } from "@/lib/api/fetchClient";

export function useMensaSync() {
  const queryClient = useQueryClient();

  return useMutation<void, Error, void>({
    meta: { suppressGlobalErrorToast: true },
    mutationFn: async () => {
      // Scraping the upstream HTML can take a few seconds; bump the
      // per-call timeout so a slow round-trip does not surface as a
      // misleading "Netzwerkfehler".
      await fetchClient("/systemadmin/mensa/sync", {
        method: "POST",
        timeoutMs: 60_000,
      });
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ["mensa"] });
      void queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    },
  });
}
