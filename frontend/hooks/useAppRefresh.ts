/**
 * Cross-platform refresh hook.
 *
 * - **Web**: `window.location.reload()` — full hard reload, guarantees
 *   all server-side state is re-fetched from scratch.
 * - **Native**: `queryClient.invalidateQueries()` — marks every cached
 *   query as stale so the next render triggers a background re-fetch.
 *
 * Use after mutating actions that affect global UI state (leave-kurs,
 * logout, role change, etc.) to ensure the nav, dashboard, and other
 * query-driven screens stay in sync.
 */
import { useCallback } from "react";
import { Platform } from "react-native";

import { queryClient } from "@/lib/api/queryClient";

/**
 * Returns a `refresh()` function appropriate for the current platform.
 *
 * @returns `() => void` — call it after a successful mutation to
 *   update the whole app state.
 */
export function useAppRefresh(): () => void {
  return useCallback(() => {
    if (Platform.OS === "web" && typeof window !== "undefined") {
      window.location.reload();
      return;
    }
    void queryClient.invalidateQueries();
  }, []);
}
