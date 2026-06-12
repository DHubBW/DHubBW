/**
 * Cross-platform file-upload hook for the DHubBW student-side
 * `PUT /student/dateien/upload` endpoint.
 *
 * Strategy:
 *
 * - **Plain `multipart/form-data` via {@link fetchClient} on both Web and
 *   Native.** The backend accepts the same multipart body in both cases;
 *   `fetchClient` already drops its JSON `Content-Type` default for
 *   `FormData` bodies (see fetchClient.ts:117-119) so the platform's own
 *   boundary stays intact. Authentication is handled by `fetchClient` too:
 *   Web rides the HttpOnly `jwt` cookie, Native attaches the Bearer token
 *   from `secureStorage`. No platform-specific upload library is needed
 *   for the MVP — a future Phase 2 (progress reporting) can layer
 *   `expo-file-system.createUploadTask` in on Native without changing the
 *   public hook shape.
 *
 * - **Pre-upload size validation** against `EXPO_PUBLIC_MAX_UPLOAD_MB`
 *   (default 10 MiB). The check rejects too-large files locally before
 *   any bytes hit the wire; the backend's own 413 response from the
 *   Spring multipart resolver remains a defensive fallback.
 *
 * - **Standardised error codes** so callers (the dev spike screen today,
 *   the FE-Dateien-2 picker tomorrow) never need to inspect raw HTTP
 *   status numbers:
 *
 *   | Code             | Trigger                                              |
 *   | ---------------- | ---------------------------------------------------- |
 *   | `'too-large'`    | Pre-validation hit, or BE 413 (multipart-resolver)   |
 *   | `'forbidden'`    | BE 403 (caller is not a course member)               |
 *   | `'not-found'`    | BE 404 (folder or student missing)                   |
 *   | `'storage-full'` | BE 507 (platform storage limit exceeded)             |
 *   | `'network'`      | `ApiError(0, 'Network error')` from `fetchClient`    |
 *   | `'unknown'`      | Anything else (BE 500, unexpected throw, …)          |
 *
 *   413 is **not** in {@link ApiErrorStatus}; `fetchClient` therefore
 *   collapses it to `ApiError(0, 'HTTP 413', body)`. We disambiguate
 *   `'too-large'` from `'network'` by matching on the message prefix
 *   (`HTTP 413` vs `Network error`). Same trick for 404. This keeps the
 *   frozen `ApiErrorStatus` union untouched.
 *
 * - **Auto-invalidation** of the per-folder file list (`['dateien',
 *   'list', ueberordnerId]`) on success, so consumers see the new file
 *   without manually managing cache keys. The list query itself does
 *   not exist yet — it is owned by the FE-Dateien-2 work — but the
 *   key shape is fixed here so both ends meet.
 *
 * Public surface — keep stable for FE-Dateien-2:
 *
 * - {@link useFileUpload} — the hook.
 * - {@link UploadInput} / {@link UploadErrorCode} — the call/return types.
 * - {@link DATEIEN_LIST_QUERY_KEY} — the invalidation key factory.
 */
import { useMutation, useQueryClient } from '@tanstack/react-query';

import { ApiError } from '@/lib/api/errors';
import { fetchClient } from '@/lib/api/fetchClient';
import type { DateiUploadDTO } from '@/lib/api/types.gen';

/** Backend route the file is uploaded to. */
const UPLOAD_PATH = '/student/dateien/upload';

/** Default cap when `EXPO_PUBLIC_MAX_UPLOAD_MB` is unset / unparseable. */
const DEFAULT_MAX_UPLOAD_MB = 50;

/** Bytes per MiB. */
const MIB = 1024 * 1024;

/**
 * Upload-specific request timeout in ms. The shared `fetchClient`
 * default of 15 s is calibrated for JSON round-trips and is far too
 * short for a multipart upload of a 50 MB PDF over a slow upstream
 * (50 MB / 5 Mbit/s ≈ 80 s). Without this override the AbortController
 * tears the request down mid-upload and the user sees a misleading
 * "Netzwerkfehler"-Toast even though the server was happily receiving
 * bytes. Five minutes is the same upper bound the platform's nginx
 * reverse proxy tolerates per request.
 */
const UPLOAD_TIMEOUT_MS = 5 * 60 * 1000;

/**
 * Standardised error-code union returned in the mutation's `error.code`
 * surface. See the file-level docstring for the trigger of each value.
 */
export type UploadErrorCode =
  | 'too-large'
  | 'forbidden'
  | 'not-found'
  | 'storage-full'
  | 'duplicate-name'
  | 'invalid-type'
  | 'empty-file'
  | 'invalid-name'
  | 'network'
  | 'unknown';

/**
 * Error type rejected by the mutation. The `code` is what callers should
 * branch on; the `message` is a short human-readable hint that may end up
 * in a toast or accessibility announcement. For the {@code 'too-large'}
 * code, {@link FileUploadError.actualMb} and {@link FileUploadError.limitMb}
 * carry the size details so callers can render a precise toast without
 * having to re-derive the cap. {@link FileUploadError.serverMessage} carries
 * the raw message text the backend returned in the 4xx body, so callers can
 * surface the specific reason (e.g. which extension was blocked, which
 * duplicate file already exists) instead of a generic fallback.
 */
export class FileUploadError extends Error {
  readonly code: UploadErrorCode;

  /** Actual file size in MB (rounded to one decimal). Optional. */
  readonly actualMb?: number;

  /** Configured upload cap in MB. Optional — only set on `'too-large'`. */
  readonly limitMb?: number;

  /**
   * Raw error text from the backend body. Stripped of HTML / JSON envelope;
   * always a plain string when present so the caller can drop it straight
   * into a toast.
   */
  readonly serverMessage?: string;

  constructor(
    code: UploadErrorCode,
    message: string,
    options?: {
      actualMb?: number;
      limitMb?: number;
      serverMessage?: string;
    },
  ) {
    super(message);
    this.name = 'FileUploadError';
    this.code = code;
    this.actualMb = options?.actualMb;
    this.limitMb = options?.limitMb;
    this.serverMessage = options?.serverMessage;
    Object.setPrototypeOf(this, FileUploadError.prototype);
  }
}

/**
 * One upload call. `file` is the platform's native binary handle:
 *
 * - Web: `File` (from an `<input type="file">` change event).
 * - Native: a `Blob`-equivalent or `{ uri, name, type }` object that
 *   the JavaScriptCore `FormData` polyfill accepts. The hook does not
 *   crack the file open — it forwards verbatim.
 */
export interface UploadInput {
  /**
   * Binary payload to upload. Must expose `size` (bytes) so the
   * pre-validation works; `File` and `Blob` always do, and the React
   * Native `{ uri, name, type, size }` shape from a picker does too.
   */
  file: UploadFileLike;
  /** UUID of the target folder. Forwarded verbatim as the `ueberordnerId` form field. */
  ueberordnerId: string;
  /** Display name for the file, sent as the `name` form field. */
  name: string;
}

/**
 * Minimal structural type for the binary payload — `File`/`Blob` on Web,
 * the picker-result `{ uri, name, type, size }` shape on Native. The hook
 * only needs the `size` for pre-validation; everything else is opaque.
 */
export type UploadFileLike =
  | Blob
  | {
      uri: string;
      name: string;
      type?: string;
      size?: number;
    };

/**
 * TanStack-Query key factory for the per-folder file list. The list query
 * itself is built in FE-Dateien-2; keeping the factory here makes the
 * invalidation contract explicit and prevents key-string drift.
 *
 * @param ueberordnerId - UUID of the folder whose list should re-fetch.
 * @returns The query-key tuple, typed `as const` so TanStack narrows it.
 */
export const DATEIEN_LIST_QUERY_KEY = (ueberordnerId: string) =>
  ['dateien', 'list', ueberordnerId] as const;

/**
 * Reads `EXPO_PUBLIC_MAX_UPLOAD_MB` and returns the cap in bytes. Falls
 * back to {@link DEFAULT_MAX_UPLOAD_MB} when the variable is missing,
 * empty, non-numeric, or `<= 0`.
 *
 * Inlined as a function (not a module-level constant) so tests that
 * temporarily mutate `process.env.EXPO_PUBLIC_MAX_UPLOAD_MB` see the new
 * value on the next `mutateAsync` call.
 */
function _maxUploadBytes(): number {
  const raw = process.env.EXPO_PUBLIC_MAX_UPLOAD_MB;
  const parsed = raw ? Number.parseFloat(raw) : Number.NaN;
  const mb = Number.isFinite(parsed) && parsed > 0 ? parsed : DEFAULT_MAX_UPLOAD_MB;
  return Math.floor(mb * MIB);
}

/**
 * Pulls the `size` field off a {@link UploadFileLike} regardless of
 * which variant it is. Returns `undefined` when the picker did not
 * include a size — pre-validation then skips and lets the backend's
 * own multipart-resolver enforce the limit.
 */
function _readSize(file: UploadFileLike): number | undefined {
  if (typeof Blob !== 'undefined' && file instanceof Blob) return file.size;
  const maybeSized = file as { size?: number };
  return typeof maybeSized.size === 'number' ? maybeSized.size : undefined;
}

/**
 * Translates a thrown error into an {@link UploadErrorCode}. See the
 * file-level docstring's table for the mapping; 413 / 404 / 415 ride
 * the `ApiError(0, 'HTTP <code>')` path because the frozen
 * {@link ApiErrorStatus} union does not list them.
 *
 * @param err - the error that escaped the upload `fetchClient` call.
 * @returns the matching {@link UploadErrorCode}.
 */
function _classify(err: unknown): UploadErrorCode {
  if (err instanceof ApiError) {
    if (err.status === 403) return 'forbidden';
    if (err.status === 507) return 'storage-full';
    if (err.status === 409) return 'duplicate-name';
    if (err.status === 0) {
      // 413 / 404 / 415 / 400 reach us here because they are not in
      // the frozen ApiErrorStatus union — fetchClient throws
      // `ApiError(0, 'HTTP …')` for them. Network errors keep their
      // message (`Network error`).
      if (err.message.includes('413')) return 'too-large';
      if (err.message.includes('415')) return 'invalid-type';
      if (err.message.includes('404')) return 'not-found';
      if (err.message.includes('400')) {
        // The validator surfaces "Datei ist leer" and "Ungültiger
        // Dateiname" both as 400. Disambiguate via the German body
        // text so callers can render the precise reason.
        const body = _serverMessageOf(err);
        if (body && /leer/i.test(body)) return 'empty-file';
        if (body && /ungültig|ungueltig/i.test(body)) return 'invalid-name';
        return 'unknown';
      }
      if (err.message.toLowerCase().includes('network')) return 'network';
      return 'unknown';
    }
    return 'unknown';
  }
  return 'unknown';
}

/**
 * Pulls the human-readable server message off an {@link ApiError}.
 * The backend's {@code GlobalExceptionHandler} writes the rejection
 * reason as a plain {@code text/plain} body, which fetchClient surfaces
 * as `err.body` (string) and as the `err.message` (when status is in
 * the mapped set). For status-0 throws the reason text lives in the
 * `body`, which is what we want to expose verbatim.
 */
function _serverMessageOf(err: ApiError): string | undefined {
  if (typeof err.body === 'string' && err.body.trim().length > 0) {
    return err.body.trim();
  }
  if (
    err.message &&
    !err.message.startsWith('HTTP ') &&
    err.message !== 'Network error'
  ) {
    return err.message;
  }
  return undefined;
}

/**
 * Builds the {@link FormData} body the backend's `@RequestParam` trio
 * expects: `file`, `ueberordnerId`, `name`. Web's `File`/`Blob` and
 * Native's `{ uri, name, type }` are both valid first arguments to
 * `FormData.append` — the runtime handles the upcast.
 */
function _buildBody(input: UploadInput): FormData {
  const body = new FormData();
  // FormData.append is typed `Blob | string` in lib.dom but accepts the
  // RN-style `{ uri, name, type }` literal in JavaScriptCore — cast
  // through `Blob` to satisfy both type systems.
  body.append('file', input.file as unknown as Blob);
  body.append('ueberordnerId', input.ueberordnerId);
  body.append('name', input.name);
  return body;
}

/**
 * Upload-a-file mutation, ready for `useFileUpload().mutateAsync({ file,
 * ueberordnerId, name })`. On success the per-folder list query is
 * invalidated; on failure the rejection is a {@link FileUploadError}
 * with a stable {@link UploadErrorCode}.
 *
 * Callers should branch on `error.code`, never on the raw message.
 *
 * @returns the TanStack-Query mutation handle. `mutateAsync` resolves
 *   to the {@link DateiUploadDTO} payload (`{ dateiID }`).
 */
export function useFileUpload() {
  const queryClient = useQueryClient();

  return useMutation<DateiUploadDTO, FileUploadError, UploadInput>({
    // Opt out of the global error toast — the caller renders a
    // size-aware toast in its own onError (see UploadButton.tsx).
    // Without this opt-out the user sees two toasts for the same
    // error: one from the local handler, one from the global
    // mutationCache subscriber.
    meta: { suppressGlobalErrorToast: true },
    mutationFn: async (input) => {
      // ----- Pre-validation: size ----------------------------------
      const size = _readSize(input.file);
      const limit = _maxUploadBytes();
      const limitMb = Math.round(limit / MIB);
      if (size !== undefined && size > limit) {
        const actualMb = Math.round((size / MIB) * 10) / 10;
        throw new FileUploadError(
          'too-large',
          `Datei ist ${actualMb} MB groß – das Limit liegt bei ${limitMb} MB.`,
          { actualMb, limitMb },
        );
      }

      // ----- Network call ------------------------------------------
      let res: Response;
      try {
        res = await fetchClient(UPLOAD_PATH, {
          method: 'PUT',
          body: _buildBody(input),
          timeoutMs: UPLOAD_TIMEOUT_MS,
        });
      } catch (err) {
        const code = _classify(err);
        const message =
          err instanceof Error && err.message ? err.message : 'Upload fehlgeschlagen.';
        const serverMessage =
          err instanceof ApiError ? _serverMessageOf(err) : undefined;
        if (code === 'too-large') {
          const actualMb =
            size !== undefined
              ? Math.round((size / MIB) * 10) / 10
              : undefined;
          throw new FileUploadError(
            'too-large',
            `Datei ist zu groß – das Limit liegt bei ${limitMb} MB.`,
            { actualMb, limitMb, serverMessage },
          );
        }
        throw new FileUploadError(code, message, { serverMessage });
      }

      // ----- Decode 201 body ---------------------------------------
      // fetchClient already rejects non-2xx, so reaching here means 2xx.
      // We still guard the JSON parse defensively — an unexpected empty
      // body would otherwise land in the success branch with `dateiID`
      // = undefined.
      try {
        const data = (await res.json()) as DateiUploadDTO;
        if (!data || typeof data.dateiID !== 'string') {
          throw new FileUploadError('unknown', 'Ungültige Server-Antwort.');
        }
        return data;
      } catch (err) {
        if (err instanceof FileUploadError) throw err;
        throw new FileUploadError('unknown', 'Server-Antwort konnte nicht gelesen werden.');
      }
    },
    onSuccess: (_data, input) => {
      void queryClient.invalidateQueries({
        queryKey: DATEIEN_LIST_QUERY_KEY(input.ueberordnerId),
      });
    },
  });
}
