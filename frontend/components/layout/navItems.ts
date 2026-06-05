import type { AuthUser } from '@/lib/auth/__stub';

export type NavItem = {
  label: string;
  href: string;
  requiresKurs?: boolean;
  requiresRole?: 'KURSADMIN' | 'SYSTEMADMIN';
};

export const ITEMS: NavItem[] = [
  { label: 'Dashboard', href: '/(app)/dashboard' },
  { label: 'Mensa', href: '/(app)/mensa' },
  { label: 'Dateien', href: '/(app)/dateien', requiresKurs: true },
  { label: 'Verwaltung', href: '/(app)/verwaltung', requiresRole: 'KURSADMIN' },
];

// TODO FE-S1 (#206): user-Type kommt dann aus dem echten AuthContext.
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
