/**
 * Zentraler `fetch`-Wrapper für alle DHubBW-API-Calls.
 *
 * Plattform-Verhalten:
 * - **Web** (`Platform.OS === 'web'`): `credentials: 'include'`. Das HttpOnly-
 *   `jwt`-Cookie wird vom Browser automatisch mitgeschickt; JS hat keinen
 *   Zugriff darauf. **WICHTIG für Sub-B:** der `token` im
 *   `AuthResponseDTO`-Body ist auf Web zu IGNORIEREN — die Source of Truth ist
 *   ausschließlich das Cookie. Niemals Web-Code schreiben, der `body.token`
 *   in localStorage/sessionStorage/etc. ablegt (XSS-Risiko).
 * - **Native** (iOS/Android): liest Token via `secureStorage.getToken()`,
 *   setzt `Authorization: Bearer <token>`. Der Backend-Filter prüft Cookie
 *   ZUERST, fällt aber auf Bearer zurück, wenn kein Cookie da ist.
 *
 * Andere Eigenschaften:
 * - **Timeout:** Default 15 s, override über `init.timeoutMs`. Implementiert
 *   via `AbortController`.
 * - **Network-Errors:** `TypeError` (z. B. CORS, DNS, offline) → `ApiError(0,
 *   'Network error')`.
 * - **Status-Mapping:** 401/403/409/422/507 → `ApiError(status, msg, body?)`.
 *   Andere non-2xx → generischer `ApiError(0, 'HTTP …')` (nicht-Auth-Pfad
 *   bleibt 0, weil `ApiErrorStatus` nur die Auth/Quota-relevanten Codes
 *   listet — das Issue-DoD verlangt explizit 401/403/409/507/0; 422 wurde
 *   ergänzt; alle anderen 4xx/5xx bekommen 0).
 * - **401-Listener:** `setUnauthorizedHandler(cb)` registriert einen Callback;
 *   die Rückgabe ist eine Unsubscribe-Funktion. Bei 401-Response werden alle
 *   registrierten Handler aufgerufen. **Recursion-Schutz:** wenn ein Handler
 *   selbst einen `fetchClient`-Call macht, der wieder 401 liefert, werden die
 *   Handler nicht erneut delegiert (`_inHandler`-Flag).
 *
 * Frozen Public API für Sub-B (siehe Sub-A-Handover Anhang):
 *   `fetchClient(path, init?)` und `setUnauthorizedHandler(cb)`.
 */
import { Platform } from 'react-native';

import secureStorage from '@/lib/storage/secureStorage';

import { ApiError, type ApiErrorStatus } from './errors';

const DEFAULT_TIMEOUT_MS = 15_000;

const _baseUrl = (process.env.EXPO_PUBLIC_API_BASE_URL ?? 'http://localhost:8080').replace(
  /\/+$/,
  '',
);

// --- 401-Listener-Registry --------------------------------------------------

const _unauthorizedHandlers = new Set<() => void>();
let _inHandler = false;

/**
 * Registriert einen Callback, der bei 401-Responses gefeuert wird.
 * Rückgabe ist eine Unsubscribe-Funktion.
 *
 * Sub-B nutzt das im AuthProvider-Mount, um bei abgelaufenem Token zentral
 * `secureStorage.clear()` + `setUser(null)` zu triggern.
 */
export function setUnauthorizedHandler(cb: () => void): () => void {
  _unauthorizedHandlers.add(cb);
  return () => {
    _unauthorizedHandlers.delete(cb);
  };
}

function _fireUnauthorized(): void {
  if (_inHandler) return; // Recursion-Schutz
  _inHandler = true;
  try {
    for (const cb of _unauthorizedHandlers) {
      try {
        cb();
      } catch {
        // Einzelner Handler-Throw darf andere nicht abreißen.
      }
    }
  } finally {
    _inHandler = false;
  }
}

// --- fetchClient ------------------------------------------------------------

export interface FetchClientInit extends RequestInit {
  /** Override für AbortController-Timeout in ms (Default: 15000). */
  timeoutMs?: number;
}

async function _readJsonSafe(res: Response): Promise<unknown> {
  try {
    const text = await res.text();
    if (!text) return undefined;
    return JSON.parse(text);
  } catch {
    return undefined;
  }
}

function _extractMessage(body: unknown, fallback: string): string {
  if (body && typeof body === 'object') {
    const m = (body as Record<string, unknown>).message;
    if (typeof m === 'string' && m.length > 0) return m;
  }
  return fallback;
}

const MAPPED_STATUSES: ReadonlySet<ApiErrorStatus> = new Set<ApiErrorStatus>([
  401, 403, 409, 422, 507,
]);

/**
 * Content-Type-Guard: nur bei String-Body auf `application/json` defaulten.
 * FormData / Blob / URLSearchParams setzen ihre eigene Boundary — Browser/RN
 * laesst Content-Type leer und ergaenzt korrekt. Sub-A-Default haette die
 * Boundary zerstoert (FE-Dateien-1 Upload-Fall).
 */
function _shouldDefaultJsonContentType(body: BodyInit | null | undefined): boolean {
  return typeof body === 'string';
}

export async function fetchClient(
  path: string,
  init: FetchClientInit = {},
): Promise<Response> {
  const { timeoutMs = DEFAULT_TIMEOUT_MS, ...rest } = init;

  const headers = new Headers(rest.headers);
  if (!headers.has('Accept')) headers.set('Accept', 'application/json');
  if (
    rest.body !== undefined &&
    rest.body !== null &&
    !headers.has('Content-Type') &&
    _shouldDefaultJsonContentType(rest.body)
  ) {
    headers.set('Content-Type', 'application/json');
  }

  // Plattform-Switch: Web = Cookie, Native = Bearer. SecureStorage-Read in
  // try/catch wrappen, damit Native-Keychain-/Keystore-Errors die
  // ApiError(0)-Garantie nicht durchbrechen (iOS Keychain locked, Android
  // Keystore-Init, korrupte EXPIRES_KEY-Werte). Best-effort: ohne Token
  // fortfahren, Backend antwortet dann mit 401 → Standard-Logout-Flow.
  let credentials: RequestCredentials | undefined = rest.credentials;
  if (Platform.OS === 'web') {
    credentials = 'include';
  } else {
    let token: string | null = null;
    try {
      token = await secureStorage.getToken();
    } catch {
      token = null;
    }
    if (token && !headers.has('Authorization')) {
      headers.set('Authorization', `Bearer ${token}`);
    }
  }

  const url = path.startsWith('http') ? path : `${_baseUrl}${path}`;

  // Signal-Bridge: kombiniert Caller-Signal (TanStack-Query Cancellation
  // bei Component-Unmount) und Timeout-Signal. Ohne Bridge wuerde ein
  // mitgegebenes Caller-Signal das dokumentierte 15s-Timeout still
  // aushebeln (siehe FE-S1 Code-Review Finding #2).
  const timeoutController = new AbortController();
  const timer = setTimeout(() => timeoutController.abort(), timeoutMs);

  const callerSignal = rest.signal ?? null;
  const onCallerAbort = () => timeoutController.abort();
  if (callerSignal) {
    if (callerSignal.aborted) {
      timeoutController.abort();
    } else {
      callerSignal.addEventListener('abort', onCallerAbort, { once: true });
    }
  }

  let res: Response;
  try {
    res = await fetch(url, {
      ...rest,
      headers,
      credentials,
      signal: timeoutController.signal,
    });
  } catch (err) {
    clearTimeout(timer);
    callerSignal?.removeEventListener('abort', onCallerAbort);
    // AbortError-Differenzierung (Code-Review Finding #3):
    // - Wenn Caller selbst aborted hat (z.B. Component-Unmount in TanStack):
    //   Original-AbortError rethrowen, damit TanStack das als Cancellation
    //   erkennt und KEINE Retries / Toasts / ErrorBoundary triggert.
    // - Wenn nur unser Timeout abortet: ApiError(0, 'Network error') wie
    //   bisher dokumentiert.
    // - TypeError (CORS/DNS/offline) → ApiError(0).
    if (err instanceof DOMException && err.name === 'AbortError') {
      if (callerSignal?.aborted) {
        throw err;
      }
      throw new ApiError(0, 'Network error');
    }
    if (err instanceof TypeError) {
      throw new ApiError(0, 'Network error');
    }
    // Unbekannte fetch-Throws: trotzdem als Network-Error nach oben.
    throw new ApiError(0, 'Network error');
  }
  clearTimeout(timer);
  callerSignal?.removeEventListener('abort', onCallerAbort);

  if (res.ok) return res;

  // Non-2xx: Body lesen (best-effort), Status mappen, ApiError werfen.
  const body = await _readJsonSafe(res);

  if (res.status === 401) {
    _fireUnauthorized();
    throw new ApiError(401, _extractMessage(body, 'Unauthorized'), body);
  }

  if (MAPPED_STATUSES.has(res.status as ApiErrorStatus)) {
    const status = res.status as ApiErrorStatus;
    throw new ApiError(status, _extractMessage(body, `HTTP ${status}`), body);
  }

  // Andere non-2xx → generisch (Status-Code in Message belassen, damit
  // Caller bei Bedarf differenzieren können; Status bleibt 0 weil
  // ApiErrorStatus die Liste festschreibt).
  throw new ApiError(0, _extractMessage(body, `HTTP ${res.status}`), body);
}
