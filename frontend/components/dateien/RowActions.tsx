/**
 * Row-level action icons for a single file: Download, Rename, Delete.
 *
 * Used by both {@link DateienTabelle} (table layout) and
 * {@link DateienCardList} (narrow layout). Each icon is wired up via
 * a callback prop — the parent screen owns the dialogs and the
 * download mutation, so this component stays presentational.
 */
import { Download, Pencil, Trash2 } from "lucide-react-native";
import { TouchableOpacity, View } from "react-native";

import { t } from "@/lib/i18n/t";
import { cn } from "@/lib/utils";

export interface RowActionsProps {
  onDownload: () => void;
  onRename: () => void;
  onDelete: () => void;
  /** Disable interaction while a mutation is in flight for this row. */
  disabled?: boolean;
  /** Extra className for the container — used to align inside columns. */
  className?: string;
  /**
   * `compact` keeps the original 15 px icons + tight padding for the
   * desktop table; `comfortable` gives bigger touch targets for the
   * mobile card layout. Defaults to `compact`.
   */
  size?: "compact" | "comfortable";
}

/**
 * Renders the three row-level icons.
 *
 * @param props - {@link RowActionsProps}
 * @returns The action button group.
 */
export function RowActions({
  onDownload,
  onRename,
  onDelete,
  disabled = false,
  className,
  size = "compact",
}: RowActionsProps) {
  const iconSize = size === "comfortable" ? 18 : 15;
  const buttonClass =
    size === "comfortable"
      ? "rounded-lg p-2 active:bg-accent"
      : "rounded-md p-1.5 active:bg-accent";
  return (
    <View className={cn("flex-row items-center justify-end gap-1", className)}>
      <TouchableOpacity
        onPress={onDownload}
        disabled={disabled}
        accessibilityRole="button"
        accessibilityLabel={t("dateien.download", "Herunterladen")}
        testID="row-action-download"
        className={buttonClass}
      >
        <Download size={iconSize} color="hsl(var(--primary))" />
      </TouchableOpacity>
      <TouchableOpacity
        onPress={onRename}
        disabled={disabled}
        accessibilityRole="button"
        accessibilityLabel={t("dateien.rename.button", "Umbenennen")}
        testID="row-action-rename"
        className={buttonClass}
      >
        <Pencil size={iconSize} color="hsl(var(--primary))" />
      </TouchableOpacity>
      <TouchableOpacity
        onPress={onDelete}
        disabled={disabled}
        accessibilityRole="button"
        accessibilityLabel={t("dateien.delete.button", "Löschen")}
        testID="row-action-delete"
        className={buttonClass}
      >
        <Trash2 size={iconSize} color="hsl(var(--destructive))" />
      </TouchableOpacity>
    </View>
  );
}
