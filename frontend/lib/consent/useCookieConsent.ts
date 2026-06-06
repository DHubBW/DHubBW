import { useCallback, useEffect, useState } from 'react';
import { Platform } from 'react-native';

/**
 * Possible consent states.
 *
 * - `'unknown'`  – not yet decided (first visit or cleared storage)
 * - `'accepted'` – user accepted cookies
 * - `'rejected'` – user rejected cookies
 */
export type ConsentState = 'unknown' | 'accepted' | 'rejected';

const STORAGE_KEY = 'cookie_consent';

/**
 * Module-level listener registry.
 *
 * Lets every mounted `useCookieConsent()` instance re-sync from
 * localStorage when any other instance changes the value. This keeps
 * the Footer and the CookieBanner in sync without needing a Context.
 */
const listeners = new Set<() => void>();

/**
 * Reads the persisted consent value from localStorage (Web only).
 *
 * @returns The stored value or `'unknown'` when not set or on Native.
 */
function readConsent(): ConsentState {
  if (Platform.OS !== 'web') return 'accepted';
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw === 'accepted' || raw === 'rejected') return raw;
  } catch {
    // Private-mode or storage unavailable — treat as unknown
  }
  return 'unknown';
}

/**
 * Persists the consent decision to localStorage (Web only) and notifies
 * all registered listeners so other hook instances update their state.
 *
 * @param value - The consent decision to store, or `null` to clear.
 */
function writeConsent(value: 'accepted' | 'rejected' | null): void {
  if (Platform.OS !== 'web') return;
  try {
    if (value === null) {
      localStorage.removeItem(STORAGE_KEY);
    } else {
      localStorage.setItem(STORAGE_KEY, value);
    }
  } catch {
    // best-effort
  }
  listeners.forEach((fn) => fn());
}

/**
 * Hook that manages the cookie-consent state.
 *
 * On Native platforms cookies/localStorage do not apply, so `consent` is
 * always `'accepted'` and the banner is never shown.
 *
 * On Web the decision is persisted in `localStorage` so it survives page
 * reloads. Before any decision is made `consent` is `'unknown'`.
 *
 * All mounted instances stay in sync: calling `accept`, `reject`, or
 * `resetConsent` on one instance immediately updates every other instance
 * via the module-level listener registry.
 *
 * @returns An object with `consent`, `accept`, `reject`, and `resetConsent`.
 */
export function useCookieConsent(): {
  consent: ConsentState;
  accept: () => void;
  reject: () => void;
  resetConsent: () => void;
} {
  const [consent, setConsent] = useState<ConsentState>(readConsent);

  useEffect(() => {
    const sync = () => setConsent(readConsent());
    listeners.add(sync);
    return () => {
      listeners.delete(sync);
    };
  }, []);

  const accept = useCallback(() => {
    setConsent('accepted');
    writeConsent('accepted');
  }, []);

  const reject = useCallback(() => {
    setConsent('rejected');
    writeConsent('rejected');
  }, []);

  const resetConsent = useCallback(() => {
    setConsent('unknown');
    writeConsent(null);
  }, []);

  return { consent, accept, reject, resetConsent };
}
