/**
 * Normaliser for the `/student/dashboard/kurs` backend quirk.
 *
 * `StudentDashboardController.java:232` returns a {@link SmallResponseDTO}
 * whose `response` is the literal string `'Kein Kurs'` when the student is not
 * enrolled in any course (instead of a clean `null`). Every consumer of this
 * endpoint must funnel the response through {@link normalizeKurs} so the
 * literal is converted to `null`.
 *
 * **Important:** this quirk is **only** present on the `/dashboard/kurs`
 * endpoint. {@link StudentProfileDTO.kursName} (returned by
 * `/dashboard/profile`) is a clean `string | null` and must NOT be passed
 * through this normaliser.
 */
import type { SmallResponseDTO } from '@/lib/api/types.gen';

/**
 * The literal value the backend uses to express "no course" on the
 * `/student/dashboard/kurs` endpoint.
 */
export const KEIN_KURS_LITERAL = 'Kein Kurs';

/**
 * Returns the actual course name, or `null` when the backend signalled
 * "Kein Kurs".
 *
 * @param body - Raw {@link SmallResponseDTO} from `GET /student/dashboard/kurs`.
 */
export function normalizeKurs(body: SmallResponseDTO): string | null {
  if (body.response === KEIN_KURS_LITERAL) return null;
  return body.response;
}
