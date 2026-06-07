/**
 * Client-side filter for the course list rendered in the registration
 * Step 2 screen.
 *
 * The backend exposes the full course catalogue via `GET /student/kurse`
 * with no pagination and no search parameter — every authenticated student
 * fetches the same list once. Filtering therefore happens entirely in the
 * client, where it can react to keystrokes without round-tripping.
 *
 * The matching rule is intentionally trivial: a single normalised lowercase
 * `includes` against the concatenation of `kursName` and `jahrgang`. This
 * matches the way users type queries ("tinf24", "b2", "2024") and avoids
 * Levenshtein-style fuzzy logic that would surface unrelated courses for
 * short queries.
 *
 * Whitespace at both ends of the query is trimmed; an empty query returns
 * the input list unchanged.
 *
 * @param items - All courses received from the backend.
 * @param query - User-entered search string (raw, not yet trimmed).
 * @returns The subset of `items` whose name + cohort year contains the
 *   normalised query as a substring. Order is preserved.
 */
import type { KursYearDTO } from '@/lib/api/types.gen';

export function fuzzyKursSearch(
  items: readonly KursYearDTO[],
  query: string,
): KursYearDTO[] {
  const needle = query.trim().toLowerCase();
  if (needle.length === 0) {
    // `slice()` keeps the public contract — callers receive a fresh array
    // they can safely sort/mutate without touching the source list.
    return items.slice();
  }
  return items.filter((k) => {
    const haystack = `${k.kursName} ${k.jahrgang}`.toLowerCase();
    return haystack.includes(needle);
  });
}
