/**
 * Helpers that turn a thrown {@link ApiError} into the most precise
 * user-facing toast text we can. The strategy across all
 * Datei-/Ordner-CRUD dialogs is the same:
 *
 * 1. Prefer the backend's own message text — it already carries the
 *    operator-meaningful detail (which extension was blocked, which
 *    file/folder name collided, the exact size limit). The
 *    {@code GlobalExceptionHandler} writes those reasons as
 *    {@code text/plain} bodies.
 * 2. Fall back to a localised, status-aware string so the user still
 *    sees something useful when no body is available (network errors,
 *    legacy endpoints that don't surface a reason).
 *
 * The helpers in this module are intentionally generic: callers pass
 * a {@link FallbackMap} that maps every status they want to handle to
 * a localised string; everything else lands on a generic fallback.
 */
import { ApiError } from "@/lib/api/errors";
import { t } from "@/lib/i18n/t";

/**
 * Pulls the human-readable server message off an {@link ApiError}.
 * Returns {@code undefined} when the body / message is empty or just
 * a generic placeholder ({@code "HTTP 413"}, {@code "Network error"},
 * {@code "Insufficient Storage"}, etc.).
 *
 * <p>Source-of-truth: the response <em>body</em>. The
 * {@code GlobalExceptionHandler} writes the operator-meaningful reason
 * as a plain {@code text/plain} body. We only fall back to
 * {@code err.message} when the body is empty AND the message is not a
 * raw HTTP reason phrase — otherwise the user would see English
 * placeholders ("Forbidden", "Insufficient Storage") instead of the
 * localised fallback the caller registered.</p>
 *
 * @param err - the thrown error.
 * @returns the trimmed body text, or {@code undefined}.
 */
export function serverMessageOf(err: unknown): string | undefined {
  if (!(err instanceof ApiError)) return undefined;
  if (typeof err.body === "string" && err.body.trim().length > 0) {
    return err.body.trim();
  }
  // No body: only surface the message when it is clearly user-facing
  // text the backend wrote — never a raw HTTP reason phrase. Anything
  // matching the well-known status-reason set ("Forbidden", "Not
  // Found", "Insufficient Storage", …) is a fetch-layer placeholder
  // and should defer to the caller's localised fallback.
  if (
    err.message &&
    !err.message.startsWith("HTTP ") &&
    err.message !== "Network error" &&
    err.message !== "Unauthorized" &&
    !_HTTP_REASON_PHRASES.has(err.message)
  ) {
    return err.message;
  }
  return undefined;
}

/**
 * HTTP reason phrases the {@code Response.statusText} surfaces in
 * English. They look like meaningful prose to a naive
 * {@code serverMessageOf} caller but are completely useless to a
 * German-speaking end user — the localised fallback is always better.
 *
 * <p>Kept narrow on purpose: only the status codes
 * {@link fetchClient.ts} can produce as {@link ApiError} messages.
 * Adding more later is fine; the worst-case impact is one extra toast
 * showing English text once.</p>
 */
const _HTTP_REASON_PHRASES: ReadonlySet<string> = new Set([
  "Bad Request",
  "Unauthorized",
  "Forbidden",
  "Not Found",
  "Conflict",
  "Payload Too Large",
  "Unsupported Media Type",
  "Unprocessable Entity",
  "Insufficient Storage",
  "Internal Server Error",
  "Bad Gateway",
  "Service Unavailable",
  "Gateway Timeout",
]);

/**
 * Returns the effective status of the error, mapping the
 * {@code ApiError(0, 'HTTP <code>')} fallthrough back to the real
 * HTTP code. Anything else returns the underlying status.
 *
 * @param err - the thrown error.
 * @returns the HTTP status as a number, or {@code 0} when unknown.
 */
export function effectiveStatusOf(err: unknown): number {
  if (!(err instanceof ApiError)) return 0;
  if (err.status !== 0) return err.status;
  // fetchClient packs unmapped statuses as `HTTP <n>`; parse the trailing
  // number out so callers can branch on the real code.
  const match = /HTTP\s+(\d{3})/i.exec(err.message);
  if (match) return Number.parseInt(match[1] ?? "0", 10);
  return 0;
}

/**
 * Returns {@code true} when the error is a network error
 * (`ApiError(0, 'Network error')` from {@code fetchClient}).
 */
export function isNetworkError(err: unknown): boolean {
  return (
    err instanceof ApiError &&
    err.status === 0 &&
    err.message.toLowerCase().includes("network")
  );
}

/**
 * Map of HTTP status → localised fallback text. Used by
 * {@link toastForApiError} when no server-side message is available.
 */
export type FallbackMap = Record<number, string>;

/**
 * Derives the best toast text for the given API error.
 *
 * <p>Order of precedence:</p>
 * <ol>
 *   <li>Backend body text, when present (always wins — operators wrote
 *       it precisely for this purpose).</li>
 *   <li>Status-specific fallback from the supplied {@link FallbackMap}.</li>
 *   <li>Generic network / unknown copy.</li>
 * </ol>
 *
 * @param err       - the thrown error
 * @param fallbacks - localised fallback strings keyed by HTTP status
 * @returns the toast string to display
 */
export function toastForApiError(
  err: unknown,
  fallbacks: FallbackMap,
): string {
  const serverText = serverMessageOf(err);
  if (serverText) return serverText;
  if (isNetworkError(err)) {
    return t("error.network", "Netzwerkfehler – Server nicht erreichbar");
  }
  const status = effectiveStatusOf(err);
  const localised = fallbacks[status];
  if (localised) return localised;
  return t("error.unknown", "Aktion fehlgeschlagen.");
}
