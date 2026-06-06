/**
 * TanStack-Query-Singleton mit projektweiten Defaults.
 *
 * Defaults laut Issue-DoD:
 * - `staleTime: 30_000`
 * - `retry: 1`
 * - `refetchOnWindowFocus: false`
 *
 * Hinweis: einzelne Hooks dürfen `staleTime` per Query überschreiben — z. B.
 * `useMensaToday: 30_000`, `useMensaThisWeek: 5*60_000`, `useMensaNextWeek:
 * 30*60_000` (siehe `docs/query-keys.md`).
 */
import { QueryClient } from '@tanstack/react-query';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});
