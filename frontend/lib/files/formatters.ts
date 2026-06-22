/**
 * Pure helpers shared by the file table and the mobile card list.
 */

/**
 * Formats an ISO `YYYY-MM-DD` date string as `dd.MM.yyyy` (German locale,
 * via `Date.toLocaleDateString`). Returns the input unchanged if the
 * backend ever delivers a non-parseable value, and an empty string for
 * `null`/`undefined`/empty input — the file table renders the cell
 * blank when no `zuletztBearbeitet` was ever set rather than printing
 * the unix-epoch fallback `01.01.1970`.
 */
export function formatIsoDate(iso: string | null | undefined): string {
  if (iso == null || iso === "") return "";
  const parsed = new Date(iso);
  if (Number.isNaN(parsed.getTime())) return iso;
  return parsed.toLocaleDateString("de-DE");
}

/**
 * Formats a file size given in MB. Values below 0.1 MB show one decimal
 * with a leading zero so very small files don't collapse to "0 MB".
 */
export function formatFileSize(mb: number): string {
  if (!Number.isFinite(mb) || mb < 0) return "—";
  if (mb < 0.1) return `${mb.toFixed(2)} MB`;
  if (mb < 10) return `${mb.toFixed(1)} MB`;
  return `${Math.round(mb)} MB`;
}
