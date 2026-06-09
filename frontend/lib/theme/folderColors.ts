/**
 * NativeWind class mappings for the {@link OrdnerFarbe} enum.
 *
 * Each colour resolves to a foreground / background pair the folder-tree
 * renderer applies to the icon and an optional pill behind it. Backgrounds
 * are kept at low opacity so the active-row highlight on top of them stays
 * legible.
 */
import type { OrdnerFarbe } from "@/lib/api/types.gen";

export interface FolderColorClasses {
  /** Tailwind text-colour class for the folder icon. */
  text: string;
  /** Tailwind background-colour class (low opacity) for an icon pill. */
  bg: string;
}

const FOLDER_COLORS: Record<OrdnerFarbe, FolderColorClasses> = {
  DEFAULT: { text: "text-zinc-400", bg: "bg-zinc-400/10" },
  RED: { text: "text-red-500", bg: "bg-red-500/10" },
  BLUE: { text: "text-blue-500", bg: "bg-blue-500/10" },
  GREEN: { text: "text-green-500", bg: "bg-green-500/10" },
};

/**
 * Returns the {@link FolderColorClasses} for a folder colour. Unknown values
 * fall back to the `DEFAULT` palette so a future backend colour does not
 * crash the UI.
 *
 * @param farbe - Backend folder colour.
 * @returns Tailwind class pair for the icon and its background pill.
 */
export function folderColorClasses(farbe: OrdnerFarbe): FolderColorClasses {
  return FOLDER_COLORS[farbe] ?? FOLDER_COLORS.DEFAULT;
}

/**
 * Returns the raw HSL/RGB colour string for the Lucide icon `color` prop.
 * Mirrors the Tailwind palette used by {@link folderColorClasses} so the
 * icon stroke matches the surrounding text class.
 *
 * @param farbe - Backend folder colour.
 * @returns Inline colour string for the Lucide `color` prop.
 */
export function folderIconColor(farbe: OrdnerFarbe): string {
  switch (farbe) {
    case "RED":
      return "rgb(239 68 68)"; // red-500
    case "BLUE":
      return "rgb(59 130 246)"; // blue-500
    case "GREEN":
      return "rgb(34 197 94)"; // green-500
    case "DEFAULT":
    default:
      return "rgb(161 161 170)"; // zinc-400
  }
}
