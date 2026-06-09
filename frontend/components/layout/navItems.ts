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
  /**
   * Base path used for active-route detection (without query string).
   * When set, isActive checks against this path instead of `href`.
   */
  activePath?: string;
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
  // KURSADMIN (exact): single Verwaltung entry → Kursverwaltung tab
  {
    label: "Verwaltung",
    href: "/(app)/verwaltung",
    icon: Settings,
    requiresRole: "KURSADMIN",
    exactRole: true,
  },
  // SYSTEMADMIN: single Verwaltung entry (tab switcher lives on the screen)
  {
    label: "Verwaltung",
    href: "/(app)/verwaltung",
    icon: Settings,
    requiresRole: "SYSTEMADMIN",
  },
];

/**
 * Decides whether a {@link NavItem} should be visible for the currently
 * logged-in user.
 */
export function visible(item: NavItem, user: AuthUser | null): boolean {
  if (!user) return false;

  if (item.requiresRole) {
    if (item.requiresRole === "KURSADMIN") {
      if (item.exactRole) {
        if (user.studentType !== "KURSADMIN") return false;
      } else {
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

  if (item.requiresKurs) {
    if (!item.requiresRole && user.studentType === "SYSTEMADMIN") {
      return true;
    }
    if (!user.kursName) return false;
  }

  return true;
}
