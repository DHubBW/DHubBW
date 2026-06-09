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
}: RowActionsProps) {
  return (
    <View className={cn("flex-row items-center justify-end gap-1", className)}>
      <TouchableOpacity
        onPress={onDownload}
        disabled={disabled}
        accessibilityRole="button"
        accessibilityLabel={t("dateien.download", "Herunterladen")}
        testID="row-action-download"
        className="rounded-md p-1.5 active:bg-accent"
      >
        <Download size={15} color="hsl(var(--primary))" />
      </TouchableOpacity>
      <TouchableOpacity
        onPress={onRename}
        disabled={disabled}
        accessibilityRole="button"
        accessibilityLabel={t("dateien.rename.button", "Umbenennen")}
        testID="row-action-rename"
        className="rounded-md p-1.5 active:bg-accent"
      >
        <Pencil size={15} color="hsl(var(--primary))" />
      </TouchableOpacity>
      <TouchableOpacity
        onPress={onDelete}
        disabled={disabled}
        accessibilityRole="button"
        accessibilityLabel={t("dateien.delete.button", "Löschen")}
        testID="row-action-delete"
        className="rounded-md p-1.5 active:bg-accent"
      >
        <Trash2 size={15} color="hsl(var(--destructive))" />
      </TouchableOpacity>
    </View>
  );
}
