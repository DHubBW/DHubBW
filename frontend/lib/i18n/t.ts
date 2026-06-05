/**
 * i18n shim — wird in FE-i18n (Post-Demo) durch i18next/react-intl ersetzt.
 * Aktuell: Fallback wird 1:1 zurueckgegeben, damit Texte schon strukturiert
 * vorbereitet sind. Aufrufer schreiben t('module.action', 'Aktion').
 */
export function t(_key: string, fallback: string): string {
  return fallback;
}
