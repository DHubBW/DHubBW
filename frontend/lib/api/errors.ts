/**
 * API-Error-Typen + Mapping. Wird von `lib/api/fetchClient.ts` genutzt, um
 * non-2xx-Responses und Network-Errors einheitlich nach oben durchzureichen.
 *
 * Status-Liste 422: Issue-Body listet nur 0/401/403/409/507; wir ergänzen 422,
 * weil `AuthenticationController.java` bei Mail-/Name-Regex-Mismatch garantiert
 * 422 mit `AuthResponseDTO("Invalid …", null, 0)` sendet (Z. 91–105).
 */

export type ApiErrorStatus = 0 | 401 | 403 | 409 | 422 | 507;

export class ApiError extends Error {
  readonly status: ApiErrorStatus;
  readonly body?: unknown;

  constructor(status: ApiErrorStatus, message: string, body?: unknown) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.body = body;
    // Sicherstellen, dass instanceof korrekt funktioniert (TS-Class-Extends-Built-In-Falle)
    Object.setPrototypeOf(this, ApiError.prototype);
  }
}

/**
 * Shape, die `AuthenticationController` bei Validation-Fail im Body sendet.
 * Spiegelt `AuthResponseDTO`: `{ message, token: null, expiresAt: 0 }`.
 */
export interface AuthErrorBody {
  message: string;
  token: null;
  expiresAt: 0;
}

export function isAuthErrorBody(b: unknown): b is AuthErrorBody {
  if (b === null || typeof b !== 'object') return false;
  const o = b as Record<string, unknown>;
  return (
    typeof o.message === 'string' &&
    o.token === null &&
    o.expiresAt === 0
  );
}
