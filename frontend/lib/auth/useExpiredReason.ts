/**
 * Hook that fires a callback when the current route carries `?reason=expired`.
 *
 * **Why:** when the global 401-handler logs the user out and redirects to
 * `/(auth)/login?reason=expired`, the login screen wants to surface a toast
 * "Deine Sitzung ist abgelaufen, bitte erneut anmelden". The toast itself is
 * implemented in **FE-Auth #212** — Sub-B only provides this hook so the
 * login screen can wire any callback (or, eventually, a sonner-native toast).
 *
 * Cross-platform note: `useLocalSearchParams` from expo-router yields
 * `string | string[] | undefined` per param. The hook normalises that to
 * the first string, fires the callback at most once per matching mount, and
 * de-duplicates if the same value re-renders.
 */
import { useLocalSearchParams } from 'expo-router';
import { useEffect, useRef } from 'react';

const EXPIRED_VALUE = 'expired';

/**
 * Calls `onExpired` exactly once when the current screen is mounted with
 * `?reason=expired`. Subsequent re-renders don't re-fire as long as the
 * value stays the same.
 *
 * @param onExpired - Callback to invoke; for FE-Auth #212 this will trigger
 *   a sonner-native toast. Pass `undefined` to opt out (e.g. for tests).
 */
export function useExpiredReason(onExpired: (() => void) | undefined): void {
  const params = useLocalSearchParams<{ reason?: string | string[] }>();
  const reason = Array.isArray(params.reason) ? params.reason[0] : params.reason;
  const lastFiredRef = useRef<string | null>(null);

  useEffect(() => {
    if (!onExpired) return;
    if (reason !== EXPIRED_VALUE) {
      lastFiredRef.current = null;
      return;
    }
    if (lastFiredRef.current === EXPIRED_VALUE) return;
    lastFiredRef.current = EXPIRED_VALUE;
    try {
      onExpired();
    } catch {
      // never let the toast wiring break navigation
    }
  }, [reason, onExpired]);
}
