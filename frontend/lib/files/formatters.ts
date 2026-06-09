/**
 * Pure helpers shared by the file table and the mobile card list.
 */

/**
 * Formats an ISO `YYYY-MM-DD` date string as `dd.MM.yyyy` (German locale,
 * via `Date.toLocaleDateString`). Returns the input unchanged if the
 * backend ever delivers a non-parseable value.
 */
export function formatIsoDate(iso: string): string {
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
