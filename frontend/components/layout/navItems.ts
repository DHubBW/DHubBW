import type { LucideIcon } from "lucide-react-native";
import {
  FileText,
  LayoutDashboard,
  Settings,
  UtensilsCrossed,
} from "lucide-react-native";

import type { AuthUser } from "@/lib/auth/AuthContext";

/** A single navigation entry shown in the sidebar and bottom nav. */
export type NavItem = {
  /** Display label of the navigation entry. */
  label: string;
  /** Expo Router path of the target screen. */
  href: string;
  /** Lucide icon for the nav entry. */
  icon: LucideIcon;
  /** When `true`, the entry is only shown if the user belongs to a course. */
  requiresKurs?: boolean;
  /** Minimum role the user must have for the entry to be visible. */
  requiresRole?: "KURSADMIN" | "SYSTEMADMIN";
};

export const ITEMS: NavItem[] = [
  { label: "Dashboard", href: "/(app)/dashboard", icon: LayoutDashboard },
  {
    label: "Dateien",
    href: "/(app)/dateien",
    icon: FileText,
    requiresKurs: true,
  },
  { label: "Mensa", href: "/(app)/mensa", icon: UtensilsCrossed },
  {
    label: "Verwaltung",
    href: "/(app)/verwaltung",
    icon: Settings,
    requiresRole: "KURSADMIN",
  },
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
export function visible(item: NavItem, user: AuthUser | null): boolean {
  if (!user) return false;
  if (item.requiresKurs && !user.kursName) return false;
  if (item.requiresRole) {
    if (item.requiresRole === "KURSADMIN") {
      if (
        user.studentType !== "KURSADMIN" &&
        user.studentType !== "SYSTEMADMIN"
      )
        return false;
    } else if (item.requiresRole === "SYSTEMADMIN") {
      if (user.studentType !== "SYSTEMADMIN") return false;
    }
  }
  return true;
}
