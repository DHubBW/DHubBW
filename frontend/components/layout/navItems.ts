import type { AuthUser } from '@/lib/auth/AuthContext';

/** A single navigation entry shown in the sidebar and bottom nav. */
export type NavItem = {
  /** Display label of the navigation entry. */
  label: string;
  /** Expo Router path of the target screen. */
  href: string;
  /** When `true`, the entry is only shown if the user belongs to a course. */
  requiresKurs?: boolean;
  /** Minimum role the user must have for the entry to be visible. */
  requiresRole?: 'KURSADMIN' | 'SYSTEMADMIN';
};

export const ITEMS: NavItem[] = [
  { label: 'Dashboard', href: '/(app)/dashboard' },
  { label: 'Mensa', href: '/(app)/mensa' },
  { label: 'Dateien', href: '/(app)/dateien', requiresKurs: true },
  { label: 'Verwaltung', href: '/(app)/verwaltung', requiresRole: 'KURSADMIN' },
];

/**
 * Decides whether a {@link NavItem} should be visible for the currently
 * logged-in user.
 *
 * Rules:
 * - Unauthenticated users see no entries.
 * - `requiresKurs` hides the entry if the user has no course membership.
 * - `requiresRole === 'KURSADMIN'` requires at least the KURSADMIN role.
 * - `requiresRole === 'SYSTEMADMIN'` requires exactly the SYSTEMADMIN role.
 *
 * @param item - The navigation entry to evaluate.
 * @param user - The currently logged-in user, or `null`.
 * @returns `true` if the entry should be displayed.
 */
// TODO: when SYSTEMADMIN-only routes appear, add a `requiresRole: 'SYSTEMADMIN'`
// item — the visible() helper already supports it.
export function visible(item: NavItem, user: AuthUser | null): boolean {
  if (!user) return false;
  if (item.requiresKurs && !user.kursName) return false;
  if (item.requiresRole) {
    if (item.requiresRole === 'KURSADMIN') {
      if (user.studentType !== 'KURSADMIN' && user.studentType !== 'SYSTEMADMIN') return false;
    } else if (item.requiresRole === 'SYSTEMADMIN') {
      if (user.studentType !== 'SYSTEMADMIN') return false;
    }
  }
  return true;
}
