/**
 * i18n shim — will be replaced by i18next/react-intl in a future sprint.
 * Currently returns the fallback string directly so UI texts are already
 * structured for the real i18n layer. Callers write t('module.action', 'Text').
 *
 * @param _key - Hierarchical key in the format `module.section.action`.
 * @param fallback - Displayed text while no real i18n is active.
 * @returns The fallback string (later: the translated string).
 */
export function t(_key: string, fallback: string): string {
  return fallback;
}
