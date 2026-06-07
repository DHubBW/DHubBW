/**
 * AuthContext / AuthProvider — central auth state for the app.
 *
 * Provides the {@link useAuth} hook with:
 *
 * - `user`: current {@link StudentProfileDTO} or `null` when logged out.
 * - `isLoading`: true until the initial profile-restore call resolves.
 * - `login` / `register`: wrap the public auth endpoints. Persist the bearer
 *   token to secureStorage on Native; ignore `body.token` on Web (the
 *   HttpOnly `jwt` cookie is the source of truth there). After success they
 *   invalidate the `['profile']` query so {@link useProfile} re-fetches and
 *   the user state reflects the new session.
 * - `logout`: best-effort `POST /student/dashboard/logout` (with token /
 *   cookie — the POST goes BEFORE clear because the backend endpoint
 *   itself requires auth) → `secureStorage.clear` (Native only) →
 *   `queryClient.clear` → `router.replace('/(auth)/login')`. `user`
 *   becomes `null` implicitly: `queryClient.clear` drops the
 *   `['profile']` cache, so the next `useProfile` read returns
 *   `undefined`.
 * - `refresh`: forces a profile refetch (used after explicit role changes).
 *
 * Provider responsibilities the screen-level code never has to touch:
 *
 * - Subscribes to the global 401-handler from `lib/api/fetchClient.ts`. When
 *   any request returns 401 we run the auto-logout sequence
 *   (`queryClient.clear` → `secureStorage.clear` → `setUser(null)` → redirect
 *   to `/(auth)/login?reason=expired`). The fetchClient's `_inHandler`
 *   recursion guard prevents the cleanup-triggered profile refetch from
 *   re-firing the handler.
 * - Drives the {@link CookieDisabledDetector} on Web — login-success +
 *   immediate profile-401 fires `onCookieDisabled`. UI binding (toast) is
 *   deferred to FE-Auth #212.
 *
 * **Frozen public types:**
 *
 * - {@link AuthUser} — type-alias for {@link StudentProfileDTO}, kept for
 *   backwards compatibility with the FE-S2 stub consumers in
 *   `components/layout/{BottomNav,Sidebar,navItems}`.
 * - {@link StudentType} — re-exported from `types.gen.ts` for the same
 *   reason.
 */
import { useQueryClient } from '@tanstack/react-query';
import { useRouter } from 'expo-router';
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import { Platform } from 'react-native';

import { ApiError } from '@/lib/api/errors';
import { fetchClient, setUnauthorizedHandler } from '@/lib/api/fetchClient';
import type {
  AuthResponseDTO,
  LoginRequestDTO,
  RegisterRequestDTO,
  StudentProfileDTO,
  StudentType,
} from '@/lib/api/types.gen';
import {
  createCookieDisabledDetector,
  type CookieDisabledDetector,
} from '@/lib/auth/cookieDisabledDetection';
import { PROFILE_QUERY_KEY, useProfile } from '@/lib/auth/useProfile';
import secureStorage from '@/lib/storage/secureStorage';

/**
 * Backwards-compat alias. The FE-S2 stub typed the user as `AuthUser`; the
 * real session value is the backend {@link StudentProfileDTO}. Re-exporting
 * the alias means BottomNav/Sidebar/navItems migrate by import-path-update
 * only.
 */
export type AuthUser = StudentProfileDTO;

/** Re-export for stub-consumer compatibility. */
export type { StudentType };

/**
 * Public shape of the auth context.
 */
export interface AuthContextValue {
  /** Current user profile, or `null` when logged out. */
  user: AuthUser | null;
  /** True until the initial profile-restore call resolves. */
  isLoading: boolean;
  /**
   * Logs the user in via `POST /public/auth/login`.
   *
   * @throws {ApiError} on 401 (invalid credentials), 422 (regex mismatch)
   *   or network error (status `0`).
   */
  login: (dhbwMail: string, password: string) => Promise<void>;
  /**
   * Registers a new student via `POST /public/auth/register`.
   *
   * @throws {ApiError} on 409 (mail already taken), 422 (regex mismatch),
   *   or network error.
   */
  register: (dhbwMail: string, name: string, password: string) => Promise<void>;
  /**
   * Logs the user out: best-effort backend call, then local cleanup, then
   * redirect to `/(auth)/login`.
   */
  logout: () => Promise<void>;
  /** Forces a profile refetch. */
  refresh: () => Promise<void>;
  /**
   * Sets/clears the callback that fires on Web when cookies are blocked
   * (login succeeds but the very next profile call returns 401). FE-Auth
   * #212 will wire a sonner-native toast here.
   */
  setOnCookieDisabled: (cb: (() => void) | null) => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

/** Path the global 401-handler redirects to. */
const LOGIN_REDIRECT_PATH = '/(auth)/login';
const LOGIN_EXPIRED_PATH = '/(auth)/login?reason=expired';

/**
 * Wraps the app subtree and provides the {@link useAuth} hook.
 *
 * Must live **inside** the project's {@link queryClient} provider — Sub-A
 * marks the slot in `app/_layout.tsx` with `AUTH_PROVIDER_HIER_EINSETZEN`.
 *
 * @param children - the rest of the app tree.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const router = useRouter();

  // ---------------------------------------------------------------------
  // Bootstrap-gate for useProfile.
  //
  // The very first profile call must run unconditionally — otherwise we
  // could not restore an existing session on app start. After that first
  // result is in (success OR 401), `bootstrapDone` flips to `true` and
  // the `enabled` flag is governed by "do we currently have a user?" —
  // i.e. did the latest profile call return data. This stops the 5-min
  // background polling from firing 401s on the login screen and from
  // re-triggering the global 401-handler / cookie detector for nothing.
  //
  // After a successful login `bootstrapDone` is reset to `false` so the
  // post-login refetch is guaranteed to run regardless of the previous
  // (logged-out) gate state; once that fetch resolves, `profile.data` is
  // populated and the gate stays open for the rest of the session.
  // ---------------------------------------------------------------------
  const [bootstrapDone, setBootstrapDone] = useState(false);
  const cachedProfile = queryClient.getQueryData<StudentProfileDTO>(
    PROFILE_QUERY_KEY,
  );
  const profile = useProfile(!bootstrapDone || cachedProfile !== undefined);
  useEffect(() => {
    if (!bootstrapDone && (profile.isFetched || profile.isError)) {
      setBootstrapDone(true);
    }
  }, [bootstrapDone, profile.isFetched, profile.isError]);

  // Detector lives for the lifetime of the provider — recreated on remount.
  const detectorRef = useRef<CookieDisabledDetector | null>(null);
  if (detectorRef.current === null) {
    detectorRef.current = createCookieDisabledDetector();
  }

  // ---------------------------------------------------------------------
  // Cookie-disabled detection: feed the detector with profile-call results.
  // Web-only behaviour; the detector is a no-op on Native.
  // ---------------------------------------------------------------------
  useEffect(() => {
    if (Platform.OS !== 'web') return;
    if (profile.status === 'success') {
      detectorRef.current?.signal('profile-success');
      return;
    }
    if (
      profile.status === 'error' &&
      profile.error instanceof ApiError &&
      profile.error.status === 401
    ) {
      detectorRef.current?.signal('profile-401');
    }
  }, [profile.status, profile.error]);

  // ---------------------------------------------------------------------
  // Local-cleanup helper — used by both the manual logout flow and the
  // global 401-handler. **No router.replace** here; callers add navigation
  // in the order their flow dictates.
  // ---------------------------------------------------------------------
  const localCleanup = useCallback(async () => {
    try {
      await secureStorage.clear();
    } catch {
      // best-effort — Keychain/Keystore errors must not abort logout
    }
    queryClient.clear();
    detectorRef.current?.reset();
  }, [queryClient]);

  // ---------------------------------------------------------------------
  // Tracks whether the user has ever been authenticated in this provider
  // instance. Set to true the moment `user` becomes non-null; never reset
  // back to false except on logout. The global 401-handler consults this
  // ref to distinguish a real session expiry (was logged in, now is not)
  // from a bootstrap 401 on `/login` / `/register` (never was logged in).
  // Without this guard the bootstrap 401 would call `queryClient.clear()`
  // + `router.replace('/login?reason=expired')`, which would in turn
  // re-trigger the profile fetch (still mounted with enabled=true) and
  // produce a request storm.
  // ---------------------------------------------------------------------
  const wasLoggedInRef = useRef(false);
  useEffect(() => {
    if (profile.data !== undefined) {
      wasLoggedInRef.current = true;
    }
  }, [profile.data]);

  // ---------------------------------------------------------------------
  // Global 401-handler — registered as a Subscribe (Set-based registry from
  // Sub-A's fetchClient). The handler runs the auto-logout sequence in the
  // documented order: queryClient.clear → secureStorage.clear → setUser(null)
  // → redirect. fetchClient's `_inHandler` recursion guard prevents the
  // profile refetch (triggered indirectly by queryClient.clear) from
  // re-firing the handler.
  // ---------------------------------------------------------------------
  useEffect(() => {
    const unsubscribe = setUnauthorizedHandler(() => {
      // Skip the auto-logout flow for the bootstrap 401 — `useProfile`
      // is the unambiguous "are you logged in?" probe; a 401 here on
      // `/login` / `/register` is expected, not a session expiry. The
      // `bootstrapDone` useEffect will see `profile.isError`, flip the
      // gate, and the query stops on its own.
      if (!wasLoggedInRef.current) return;

      // Auto-logout cleanup ordering per Handover Z. 66:
      // queryClient.clear → secureStorage.clear → setUser(null) → redirect.
      // setUser(null) happens implicitly: queryClient.clear drops the
      // ['profile'] cache, useProfile re-runs, returns 401 → user=null.
      wasLoggedInRef.current = false;
      queryClient.clear();
      void secureStorage.clear().catch(() => {
        // best-effort — handler must be idempotent
      });
      detectorRef.current?.reset();
      try {
        router.replace(LOGIN_EXPIRED_PATH);
      } catch {
        // router unavailable during early render — accept and move on
      }
    });
    return unsubscribe;
  }, [queryClient, router]);

  // ---------------------------------------------------------------------
  // login
  //
  // Posts credentials, parses {@link AuthResponseDTO}, persists token on
  // Native (Web ignores body.token — see fetchClient JSDoc), then waits for
  // a fresh profile fetch so the returned promise only resolves once
  // `user` is populated.
  // ---------------------------------------------------------------------
  const login = useCallback(
    async (dhbwMail: string, password: string) => {
      const body: LoginRequestDTO = { dhbwMail, password };
      const res = await fetchClient('/public/auth/login', {
        method: 'POST',
        body: JSON.stringify(body),
      });
      const data = (await res.json()) as AuthResponseDTO;

      if (Platform.OS !== 'web') {
        if (!data.token || !data.expiresAt) {
          throw new ApiError(0, 'Auth response missing token/expiresAt');
        }
        await secureStorage.setToken({
          token: data.token,
          expiresAt: data.expiresAt,
        });
      }
      // On Web body.token is intentionally ignored — the HttpOnly jwt cookie
      // set by the same response is the only place the token lives.

      detectorRef.current?.signal('login-success');
      // Re-open the bootstrap gate so the post-login refetch runs even if
      // we were previously in the "logged-out, polling disabled" state.
      setBootstrapDone(false);
      await queryClient.invalidateQueries({ queryKey: PROFILE_QUERY_KEY });
    },
    [queryClient],
  );

  // ---------------------------------------------------------------------
  // register — same flow as login against the register endpoint.
  // ---------------------------------------------------------------------
  const register = useCallback(
    async (dhbwMail: string, name: string, password: string) => {
      const body: RegisterRequestDTO = { dhbwMail, name, password };
      const res = await fetchClient('/public/auth/register', {
        method: 'POST',
        body: JSON.stringify(body),
      });
      const data = (await res.json()) as AuthResponseDTO;

      if (Platform.OS !== 'web') {
        if (!data.token || !data.expiresAt) {
          throw new ApiError(0, 'Auth response missing token/expiresAt');
        }
        await secureStorage.setToken({
          token: data.token,
          expiresAt: data.expiresAt,
        });
      }

      detectorRef.current?.signal('login-success');
      // Re-open the bootstrap gate — see login() for rationale.
      setBootstrapDone(false);
      await queryClient.invalidateQueries({ queryKey: PROFILE_QUERY_KEY });
    },
    [queryClient],
  );

  // ---------------------------------------------------------------------
  // logout — manual flow, ordering per Handover Z. 67:
  // best-effort POST /student/dashboard/logout (with token / cookie) →
  // secureStorage.clear (Native) → queryClient.clear → router.replace
  // ('/(auth)/login').
  //
  // The POST goes BEFORE clear because the backend endpoint itself needs
  // the auth credential — clearing first would turn every logout into a
  // 401. `user = null` happens implicitly: queryClient.clear drops the
  // ['profile'] cache, and useProfile is gated to disabled once
  // bootstrapDone+no-cached-data, so we never refetch.
  // ---------------------------------------------------------------------
  const logout = useCallback(async () => {
    try {
      await fetchClient('/student/dashboard/logout', { method: 'POST' });
    } catch {
      // 401/network — accept, the user wants to log out anyway
    }
    await localCleanup();
    try {
      router.replace(LOGIN_REDIRECT_PATH);
    } catch {
      // ignore — see 401-handler note
    }
  }, [localCleanup, router]);

  // ---------------------------------------------------------------------
  // refresh — forces a profile refetch (e.g. after the user accepts a role
  // change).
  // ---------------------------------------------------------------------
  const refresh = useCallback(async () => {
    await queryClient.invalidateQueries({ queryKey: PROFILE_QUERY_KEY });
  }, [queryClient]);

  // ---------------------------------------------------------------------
  // setOnCookieDisabled — exposed on the context so FE-Auth #212 can wire
  // a sonner-native toast at the screen level without re-implementing the
  // detector.
  // ---------------------------------------------------------------------
  const setOnCookieDisabled = useCallback((cb: (() => void) | null) => {
    detectorRef.current?.setOnCookieDisabled(cb);
  }, []);

  // ---------------------------------------------------------------------
  // Derive the public state.
  // ---------------------------------------------------------------------
  const user: AuthUser | null = profile.data ?? null;
  // isLoading is true ONLY during the initial bootstrap. After the first
  // profile call resolves we never block the UI again — refetches happen
  // in the background.
  const isLoading = profile.isPending && !profile.isFetched;

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isLoading,
      login,
      register,
      logout,
      refresh,
      setOnCookieDisabled,
    }),
    [user, isLoading, login, register, logout, refresh, setOnCookieDisabled],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

/**
 * Reads the current auth context. Must be called inside an
 * {@link AuthProvider} subtree.
 */
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (ctx === null) {
    throw new Error('useAuth() must be called within <AuthProvider>');
  }
  return ctx;
}
