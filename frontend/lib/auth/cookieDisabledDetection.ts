/**
 * Web-only detection: did the browser silently drop the JWT cookie?
 *
 * **Symptom:** `POST /public/auth/login` succeeds (200 with body containing
 * the auth token) but the immediately following `GET /student/dashboard/profile`
 * returns 401 — because the browser refused to store/send the `Set-Cookie:
 * jwt=…` from the login response. Typical causes: cookies blocked, third-party
 * cookies disabled in cross-origin dev setups, privacy extensions.
 *
 * Native is unaffected: there the bearer token is read from secureStorage on
 * every request, so a missing cookie cannot break the flow.
 *
 * **Scope of this module:** detection only. It exposes a tiny callback
 * registry that {@link AuthProvider} feeds with login-success / profile-401
 * events. UI binding (e.g. a sonner-native toast "Cookies sind blockiert")
 * lives in **FE-Auth #212**, not here.
 */
import { Platform } from 'react-native';

/**
 * Possible signals the AuthProvider hands to the detector.
 *
 * - `'login-success'`: a login or register call returned 200.
 * - `'profile-401'`: a profile call returned 401.
 * - `'profile-success'`: a profile call returned 200 — clears any pending
 *   "login just succeeded" flag so a much-later 401 (token expiry) is not
 *   mis-attributed to disabled cookies.
 */
export type CookieDetectionSignal =
  | 'login-success'
  | 'profile-401'
  | 'profile-success';

/**
 * Stateful detector. Holds a single boolean: "did a login just succeed?".
 *
 * After `'login-success'` the next signal decides:
 * - `'profile-success'` → flag cleared, all good.
 * - `'profile-401'` → cookies were dropped, fire `onCookieDisabled`.
 *
 * `onCookieDisabled` is intentionally optional — FE-Auth #212 will wire a
 * toast, but Sub-B does **not** import sonner-native here.
 */
export interface CookieDisabledDetector {
  /** Feeds a signal into the state machine. */
  signal(s: CookieDetectionSignal): void;
  /**
   * Sets/replaces the callback fired when disabled cookies are detected.
   * Pass `null` to unregister.
   */
  setOnCookieDisabled(cb: (() => void) | null): void;
  /** Resets the internal "login just succeeded" flag. */
  reset(): void;
}

/**
 * Creates a fresh detector. **Web only** — on Native every method is a no-op
 * (Native auth uses the bearer token from secureStorage, not cookies).
 */
export function createCookieDisabledDetector(): CookieDisabledDetector {
  if (Platform.OS !== 'web') {
    // No-op on Native — `signal()` becomes a free noop, no allocations.
    return {
      signal: () => {},
      setOnCookieDisabled: () => {},
      reset: () => {},
    };
  }

  let loginJustSucceeded = false;
  let onCookieDisabled: (() => void) | null = null;

  return {
    signal(s: CookieDetectionSignal): void {
      if (s === 'login-success') {
        loginJustSucceeded = true;
        return;
      }
      if (s === 'profile-success') {
        loginJustSucceeded = false;
        return;
      }
      if (s === 'profile-401' && loginJustSucceeded) {
        loginJustSucceeded = false;
        try {
          onCookieDisabled?.();
        } catch {
          // never let UI hooks break the auth flow
        }
      }
    },
    setOnCookieDisabled(cb: (() => void) | null): void {
      onCookieDisabled = cb;
    },
    reset(): void {
      loginJustSucceeded = false;
    },
  };
}
