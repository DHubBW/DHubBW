import type { LucideIcon } from "lucide-react-native";
import {
  FileText,
  LayoutDashboard,
  Settings,
  Shield,
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
  /**
   * When `true`, the entry is only shown if the user belongs to a course.
   * For SYSTEMADMIN: also requires kursName to be set.
   */
  requiresKurs?: boolean;
  /** Minimum role the user must have for the entry to be visible. */
  requiresRole?: "KURSADMIN" | "SYSTEMADMIN";
  /**
   * When `true`, only the exact role matches — SYSTEMADMIN does NOT
   * inherit this entry via the KURSADMIN ≤ SYSTEMADMIN hierarchy.
   */
  exactRole?: boolean;
};

export const ITEMS: NavItem[] = [
  { label: "Dashboard", href: "/(app)/dashboard", icon: LayoutDashboard },
  {
    label: "Dateien",
    href: "/(app)/dateien",
    icon: FileText,
    // STUDENT with kurs, KURSADMIN, or SYSTEMADMIN (regardless of kurs)
    requiresKurs: true,
  },
  { label: "Mensa", href: "/(app)/mensa", icon: UtensilsCrossed },
  // KURSADMIN (not SYSTEMADMIN): single "Verwaltung" entry
  {
    label: "Verwaltung",
    href: "/(app)/verwaltung",
    icon: Settings,
    requiresRole: "KURSADMIN",
    exactRole: true,
  },
  // SYSTEMADMIN with kurs: "Kursverwaltung"
  {
    label: "Kursverwaltung",
    href: "/(app)/verwaltung",
    icon: Settings,
    requiresRole: "SYSTEMADMIN",
    requiresKurs: true,
  },
  // SYSTEMADMIN always: "Systemverwaltung"
  {
    label: "Systemverwaltung",
    href: "/(app)/verwaltung",
    icon: Shield,
    requiresRole: "SYSTEMADMIN",
  },
];

/**
 * Decides whether a {@link NavItem} should be visible for the currently
 * logged-in user.
 *
 * Rules:
 * - Unauthenticated users see no entries.
 * - `requiresKurs` hides the entry when the user has no course membership,
 *   UNLESS the user is SYSTEMADMIN (who can access files without a course)
 *   — this exception does NOT apply to entries that also carry `requiresRole:
 *   "SYSTEMADMIN"` with `requiresKurs`, where kurs is genuinely required.
 * - `requiresRole === 'KURSADMIN'` with `exactRole` requires exactly KURSADMIN.
 * - `requiresRole === 'KURSADMIN'` without `exactRole` requires KURSADMIN or
 *   SYSTEMADMIN (upward compatibility).
 * - `requiresRole === 'SYSTEMADMIN'` requires exactly SYSTEMADMIN.
 *
 * @param item - The navigation entry to evaluate.
 * @param user - The currently logged-in user, or `null`.
 * @returns `true` if the entry should be displayed.
 */
export function visible(item: NavItem, user: AuthUser | null): boolean {
  if (!user) return false;

  // Role gate
  if (item.requiresRole) {
    if (item.requiresRole === "KURSADMIN") {
      if (item.exactRole) {
        // Exact match: only KURSADMIN, not SYSTEMADMIN
        if (user.studentType !== "KURSADMIN") return false;
      } else {
        // At-least-KURSADMIN: KURSADMIN or SYSTEMADMIN
        if (
          user.studentType !== "KURSADMIN" &&
          user.studentType !== "SYSTEMADMIN"
        )
          return false;
      }
    } else if (item.requiresRole === "SYSTEMADMIN") {
      if (user.studentType !== "SYSTEMADMIN") return false;
    }
  }

  // Kurs gate — applies to all roles including SYSTEMADMIN when explicitly set
  if (item.requiresKurs) {
    // For the plain Dateien entry (no requiresRole): SYSTEMADMIN bypasses kurs check
    if (!item.requiresRole && user.studentType === "SYSTEMADMIN") {
      return true;
    }
    if (!user.kursName) return false;
  }

  return true;
}
