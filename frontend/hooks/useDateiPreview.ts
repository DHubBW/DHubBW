/**
 * Web preview hook for `GET /student/dateien/{dateiId}/preview`.
 *
 * Strategy:
 *
 * - **Web:** the response body is consumed via {@link Response.blob}, an
 *   `Object-URL` is created and returned to the caller alongside the
 *   detected MIME type. The {@link PreviewDialog} consumes the URL and
 *   embeds it in an `<iframe>` / `<img>` / `<video>` / `<audio>` tag so
 *   the browser renders the file inline.  The caller is responsible for
 *   revoking the Object-URL once the preview closes — see
 *   {@link revokePreviewUrl}.
 *
 * - **Native:** the MVP intentionally does not support inline preview on
 *   iOS/Android.  The hook surfaces a `'native-unsupported'` error code
 *   so the caller can render a "Demnächst auf Native"-Hinweis.
 *
 * Errors mirror {@link useDateiDownload}:
 *
 * - `'forbidden'`         — BE 403
 * - `'not-found'`         — BE 404 (file or student missing)
 * - `'network'`           — `ApiError(0, 'Network error')`
 * - `'unknown'`           — anything else
 * - `'native-unsupported'` — caller is on iOS/Android (out of MVP)
 *
 * Unlike the download endpoint the preview endpoint does NOT increment
 * the per-file download counter, so we deliberately do NOT invalidate
 * the per-folder list query on success.
 */
import { useMutation } from "@tanstack/react-query";
import { Platform } from "react-native";

import { ApiError } from "@/lib/api/errors";
import { fetchClient } from "@/lib/api/fetchClient";

/**
 * Standardised error-code union returned in the mutation's `error.code`
 * surface. See file-level docstring for the trigger of each value.
 */
export type PreviewErrorCode =
  | "forbidden"
  | "not-found"
  | "network"
  | "unknown"
  | "native-unsupported";

/**
 * Error type rejected by the preview mutation. Callers branch on `code`,
 * not on the message.
 */
export class FilePreviewError extends Error {
  readonly code: PreviewErrorCode;

  constructor(code: PreviewErrorCode, message: string) {
    super(message);
    this.name = "FilePreviewError";
    this.code = code;
    Object.setPrototypeOf(this, FilePreviewError.prototype);
  }
}

/** Input shape for the preview mutation. */
export interface PreviewDateiInput {
  /** UUID of the file to preview. */
  dateiId: string;
}

/** Result returned by a successful preview mutation. */
export interface PreviewDateiResult {
  /** Browser Object-URL pointing to the fetched blob. Revoke when done. */
  url: string;
  /** MIME type as reported by the server (or `application/octet-stream`). */
  contentType: string;
}

/**
 * Translates a thrown error into a {@link PreviewErrorCode}. 404 reaches
 * us as `ApiError(0, 'HTTP 404')` because 404 is not in the frozen
 * {@link ApiErrorStatus} union — same disambiguation trick the download
 * hook uses.
 */
function _classify(err: unknown): PreviewErrorCode {
  if (err instanceof ApiError) {
    if (err.status === 403) return "forbidden";
    if (err.status === 0) {
      if (err.message.includes("404")) return "not-found";
      if (err.message.toLowerCase().includes("network")) return "network";
      return "unknown";
    }
    return "unknown";
  }
  return "unknown";
}

/**
 * Revoke a preview Object-URL created by {@link useDateiPreview}. Safe
 * to call with `null`/empty inputs so callers can keep the cleanup
 * branch-free.
 */
export function revokePreviewUrl(url: string | null | undefined): void {
  if (!url) return;
  try {
    URL.revokeObjectURL(url);
  } catch {
    // Old URL implementations / non-browser environments — best-effort.
  }
}

/**
 * Preview mutation. Caller passes `{ dateiId }` to `mutateAsync`; on Web
 * the blob is fetched and an Object-URL is returned together with the
 * MIME type so the caller can mount the right HTML element. On Native
 * the mutation rejects with `code: 'native-unsupported'`.
 *
 * @returns the TanStack-Query mutation handle.
 */
export function useDateiPreview() {
  return useMutation<
    PreviewDateiResult,
    FilePreviewError,
    PreviewDateiInput
  >({
    // PreviewDialog renders its own toast on error; suppress the global
    // one so users don't see two for the same failure.
    meta: { suppressGlobalErrorToast: true },
    mutationFn: async ({ dateiId }) => {
      if (Platform.OS !== "web") {
        throw new FilePreviewError(
          "native-unsupported",
          "Datei-Vorschau ist auf Native im MVP noch nicht verfügbar.",
        );
      }

      let res: Response;
      try {
        res = await fetchClient(`/student/dateien/${dateiId}/preview`, {
          // See useDateiDownload — bump the per-call timeout for
          // binary endpoints to five minutes.
          timeoutMs: 5 * 60 * 1000,
        });
      } catch (err) {
        const code = _classify(err);
        const message =
          err instanceof Error && err.message
            ? err.message
            : "Vorschau fehlgeschlagen.";
        throw new FilePreviewError(code, message);
      }

      try {
        const blob = await res.blob();
        const contentType =
          res.headers.get("Content-Type") ?? "application/octet-stream";
        const url = URL.createObjectURL(blob);
        return { url, contentType };
      } catch {
        throw new FilePreviewError(
          "unknown",
          "Vorschau konnte nicht geladen werden.",
        );
      }
    },
  });
}
