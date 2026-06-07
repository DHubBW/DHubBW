/**
 * Resolves the post-login redirect target from a `?returnTo=` query param.
 *
 * **Why a dedicated helper:** the login screen is a popular Open-Redirect
 * target ("look here, just click this URL to log in" → after auth the user
 * is bounced to `evil.com`). The Frozen-API contract for FE-Auth #212 is:
 *
 * 1. `decodeURIComponent` the raw value (so encoded paths like
 *    `%2F%28app%29%2Fmensa` are accepted as legitimate in-app routes).
 * 2. Whitelist: the decoded path **must** start with `/` AND **must not**
 *    start with `//` (which the URL parser reads as a protocol-relative URL
 *    pointing to another host).
 * 3. Anything that fails the whitelist — including `undefined`, malformed
 *    encodings, absolute URLs, and protocol-relative URLs — falls back to
 *    the hardcoded safe target {@link FALLBACK_PATH}.
 *
 * Cross-platform: on Native there is no URL bar, but `useLocalSearchParams`
 * still resolves to the deep-link query parameters, so the same logic applies.
 *
 * The hook signature mirrors {@link useExpiredReason}: callers pass the
 * `useLocalSearchParams` result directly. `string | string[] | undefined` is
 * normalised to the first string.
 */
const FALLBACK_PATH = '/(app)/dashboard';

/**
 * Shape that callers pass in. Matches `useLocalSearchParams<{returnTo?: ...}>()`
 * and is also convenient to construct in unit tests.
 */
export interface ResolveReturnToParams {
  returnTo?: string | string[] | undefined;
}

/**
 * Returns the safe redirect target for the post-login navigation.
 *
 * @param params - Parameters object from `useLocalSearchParams`. Only the
 *   `returnTo` field is read; everything else is ignored.
 * @returns A whitelisted in-app path starting with `/` (never `//`), or the
 *   hardcoded fallback `/(app)/dashboard` when the input is missing,
 *   malformed, or fails the whitelist.
 */
export function resolveReturnTo(params: ResolveReturnToParams): string {
  const raw = Array.isArray(params.returnTo) ? params.returnTo[0] : params.returnTo;
  if (typeof raw !== 'string' || raw.length === 0) {
    return FALLBACK_PATH;
  }

  let decoded: string;
  try {
    decoded = decodeURIComponent(raw);
  } catch {
    // Malformed escape sequence (e.g. lone `%` without two hex digits) — refuse.
    return FALLBACK_PATH;
  }

  // Whitelist: must be a same-origin in-app path.
  // - `startsWith('/')` rejects absolute URLs (`https://evil.com`).
  // - `!startsWith('//')` rejects protocol-relative URLs (`//evil.com`),
  //   which a browser would resolve against the current scheme.
  if (!decoded.startsWith('/') || decoded.startsWith('//')) {
    return FALLBACK_PATH;
  }

  // Defense-in-depth: a backslash is treated as `/` by some URL parsers
  // (notably WHATWG-URL on Web), so `/\evil.com` could be read as
  // `//evil.com`. NUL bytes truncate strings in some downstream consumers
  // (router parsers, native bridges) and have no legitimate path use.
  if (decoded.includes('\\') || decoded.includes('\0')) {
    return FALLBACK_PATH;
  }

  return decoded;
}
