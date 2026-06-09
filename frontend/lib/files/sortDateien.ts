/**
 * Sort helpers used by the file table and the mobile card list.
 *
 * Sorting is client-side because the backend does not support a `sort=`
 * query parameter on the file endpoints. The five sortable columns map
 * to {@link DateiDTO} fields with intuitive primary types (string for
 * name / uploader, ISO-date string for the modified column, number for
 * downloads / size).
 */
import type { DateiDTO } from "@/lib/api/types.gen";

export type SortColumn = "name" | "downloads" | "uploader" | "modified" | "size";
export type SortDirection = "asc" | "desc";

export interface SortState {
  column: SortColumn;
  direction: SortDirection;
}

/**
 * Returns the comparator value for `datei` under the given sort column.
 * Strings are lower-cased for stable case-insensitive ordering. The
 * modified column uses the raw ISO-`YYYY-MM-DD` string, which sorts
 * lexicographically the same way as a `Date` comparison without the
 * allocation.
 */
function sortValue(
  datei: DateiDTO,
  column: SortColumn,
): string | number {
  switch (column) {
    case "name":
      return datei.name.toLocaleLowerCase("de");
    case "downloads":
      return datei.downloadAnzahl;
    case "uploader":
      return (datei.uploaderName ?? "").toLocaleLowerCase("de");
    case "modified":
      return datei.zuletztBearbeitet;
    case "size":
      return datei.fileSize;
  }
}

/**
 * Returns a new array with `dateien` sorted according to `sort`. The
 * input is not mutated; ties keep their original order so repeated
 * column toggles stay stable.
 *
 * @param dateien - Files to sort.
 * @param sort - Active column + direction.
 * @returns Sorted copy of the input list.
 */
export function sortDateien(
  dateien: DateiDTO[],
  sort: SortState,
): DateiDTO[] {
  const sorted = dateien.slice();
  sorted.sort((a, b) => {
    const av = sortValue(a, sort.column);
    const bv = sortValue(b, sort.column);
    if (av < bv) return sort.direction === "asc" ? -1 : 1;
    if (av > bv) return sort.direction === "asc" ? 1 : -1;
    return 0;
  });
  return sorted;
}

/**
 * Returns the next {@link SortState} after a header click. Clicking the
 * already-active column toggles direction; clicking another column
 * resets to ascending.
 */
export function toggleSort(current: SortState, column: SortColumn): SortState {
  if (current.column === column) {
    return {
      column,
      direction: current.direction === "asc" ? "desc" : "asc",
    };
  }
  return { column, direction: "asc" };
}
