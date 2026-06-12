/**
 * Web download hook for `GET /student/dateien/{dateiId}/download`.
 *
 * Strategy:
 *
 * - **Web:** the response body is consumed via {@link Response.blob}, an
 *   `Object-URL` is created, and a transient anchor element triggers the
 *   browser's native download dialog. {@link fetchClient} is used so
 *   cookie-based auth on Web (and bearer-based auth on Native, even though
 *   Native is out of MVP scope) flows through the standard wrapper. The
 *   binary response opens through the same wrapper because `res.blob()` is
 *   available on the standard `Response` returned by `fetchClient`.
 *
 * - **Native:** the MVP intentionally does not pull in
 *   `expo-file-system` / `expo-sharing` — those land in a follow-up phase.
 *   The hook surfaces a `'native-unsupported'` error code so the caller
 *   can render a "Demnächst auf Native"-Hinweis instead of a generic toast.
 *
 * Errors:
 *
 * - `'forbidden'`   — BE 403
 * - `'not-found'`   — BE 404 (file or student missing)
 * - `'network'`     — `ApiError(0, 'Network error')`
 * - `'unknown'`     — anything else
 * - `'native-unsupported'` — caller is on iOS/Android (out of MVP)
 *
 * On success the per-folder list query is invalidated so the
 * `downloadAnzahl` counter that BE-2 increments shows up in the table.
 */
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Platform } from "react-native";

import { ApiError } from "@/lib/api/errors";
import { fetchClient } from "@/lib/api/fetchClient";

import { DATEIEN_LISTE_QUERY_KEY } from "./useDateienListe";

/**
 * Standardised error-code union returned in the mutation's `error.code`
 * surface. See file-level docstring for the trigger of each value.
 */
export type DownloadErrorCode =
  | "forbidden"
  | "not-found"
  | "network"
  | "unknown"
  | "native-unsupported";

/**
 * Error type rejected by the download mutation. Callers branch on `code`,
 * not on the message.
 */
export class FileDownloadError extends Error {
  readonly code: DownloadErrorCode;

  constructor(code: DownloadErrorCode, message: string) {
    super(message);
    this.name = "FileDownloadError";
    this.code = code;
    Object.setPrototypeOf(this, FileDownloadError.prototype);
  }
}

/** Input shape for the download mutation. */
export interface DownloadDateiInput {
  /** UUID of the file to download. */
  dateiId: string;
  /** Display name to suggest to the browser save dialog. */
  name: string;
  /**
   * UUID of the folder the file belongs to, or `null` for the root view.
   * Used to invalidate the matching list query so the increased
   * `downloadAnzahl` is visible after the download.
   */
  ordnerId: string | null;
}

/**
 * Translates a thrown error into a {@link DownloadErrorCode}. 404 reaches
 * us as `ApiError(0, 'HTTP 404')` because 404 is not in the frozen
 * {@link ApiErrorStatus} union — same disambiguation trick the upload hook
 * uses for 413/404.
 */
function _classify(err: unknown): DownloadErrorCode {
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
 * Triggers a browser download for the given blob using a transient
 * `<a download>` element. Web-only — caller must guard on
 * `Platform.OS === 'web'`. The Object-URL is revoked on the next
 * animation frame so the browser has time to start the download.
 */
function _saveBlobInBrowser(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  // The anchor must be in the DOM for some browsers (notably Firefox) to
  // honour the `download` attribute; remove it again straight after.
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  // Defer the revoke so the download has actually started — synchronous
  // revoke after click() races the network in some browsers. Capture the
  // function eagerly so a later teardown (or rare browser quirk that
  // replaces URL) cannot break the timer.
  const revoke = URL.revokeObjectURL;
  setTimeout(() => revoke(url), 0);
}

/**
 * Download mutation. Caller passes `{ dateiId, name, ordnerId }` to
 * `mutateAsync`; on Web the browser's save dialog opens, the per-folder
 * list query is invalidated, and the mutation resolves. On Native the
 * mutation rejects with `code: 'native-unsupported'` so the caller can
 * render a "Demnächst auf Native"-Hint without surfacing a generic error
 * toast.
 *
 * @returns the TanStack-Query mutation handle.
 */
export function useDateiDownload() {
  const queryClient = useQueryClient();

  return useMutation<void, FileDownloadError, DownloadDateiInput>({
    // The Dateien screen surfaces its own download-error toast; suppress
    // the global one so users don't see two for the same failure.
    meta: { suppressGlobalErrorToast: true },
    mutationFn: async ({ dateiId, name }) => {
      if (Platform.OS !== "web") {
        throw new FileDownloadError(
          "native-unsupported",
          "Datei-Download ist auf Native im MVP noch nicht verfügbar.",
        );
      }

      let res: Response;
      try {
        res = await fetchClient(`/student/dateien/${dateiId}/download`, {
          // Big files easily exceed the shared 15 s default.
          timeoutMs: 5 * 60 * 1000,
        });
      } catch (err) {
        const code = _classify(err);
        const message =
          err instanceof Error && err.message
            ? err.message
            : "Download fehlgeschlagen.";
        throw new FileDownloadError(code, message);
      }

      try {
        const blob = await res.blob();
        _saveBlobInBrowser(blob, name);
      } catch {
        throw new FileDownloadError(
          "unknown",
          "Datei konnte nicht gespeichert werden.",
        );
      }
    },
    onSuccess: (_data, input) => {
      void queryClient.invalidateQueries({
        queryKey: DATEIEN_LISTE_QUERY_KEY(input.ordnerId),
      });
    },
  });
}
