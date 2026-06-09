/**
 * Toolbar above the file list. Hosts a slot on the right for the upload
 * button — populated by the upload work in a follow-up issue. Today the
 * toolbar shows the breadcrumb path of the active folder (or a generic
 * "Alle Dateien" label when the root view is selected).
 */
import { ChevronRight } from "lucide-react-native";
import type { ReactNode } from "react";
import { View } from "react-native";

import { Text } from "@/components/ui/text";
import type { OrdnerDTO } from "@/lib/api/types.gen";
import { t } from "@/lib/i18n/t";

export interface DateienToolbarProps {
  /**
   * Folder currently displayed in the table, or `null` when the root view
   * (`Alle Dateien`) is active.
   */
  selected: OrdnerDTO | null;
  /** Right-aligned action slot — empty by default. */
  actions?: ReactNode;
}

/**
 * Renders the file-list header with breadcrumb and an action slot.
 *
 * @param props - {@link DateienToolbarProps}
 * @returns The toolbar element.
 */
export function DateienToolbar({ selected, actions }: DateienToolbarProps) {
  const rootLabel = t("dateien.toolbar.allFiles", "Alle Dateien");
  return (
    <View className="flex-row items-center justify-between gap-3 border-b border-border px-4 py-3">
      <View className="flex-1 flex-row items-center gap-1">
        <Text className="text-sm text-muted-foreground">{rootLabel}</Text>
        {selected && (
          <>
            <ChevronRight size={14} color="hsl(var(--muted-foreground))" />
            <Text
              className="text-sm font-semibold text-foreground"
              numberOfLines={1}
            >
              {selected.name}
            </Text>
          </>
        )}
      </View>
      <View className="flex-row items-center gap-2">{actions}</View>
    </View>
  );
}
