/**
 * Datei-row context menu shown via the row's 3-dot icon.
 *
 * <p>The menu is implemented as a small modal sheet rather than a
 * floating popover so the same component renders correctly on Web,
 * iOS and Android — the project does not bundle a cross-platform
 * popover primitive yet. Mirrors {@link OrdnerKontextMenu} for the
 * folder side.</p>
 *
 * <p>Hosts the secondary file actions:</p>
 * <ul>
 *   <li>Verschieben → opens {@link MoveDateiDialog}.</li>
 *   <li>Umbenennen → opens {@link RenameDialog}.</li>
 *   <li>Löschen → opens the destructive confirm dialog.</li>
 * </ul>
 *
 * <p>Authorisation is enforced by the backend — on failure the
 * downstream dialog surfaces the 403 toast.</p>
 */
import { FolderInput, Pencil, Trash2 } from "lucide-react-native";
import { Pressable, View } from "react-native";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Text } from "@/components/ui/text";
import type { DateiDTO } from "@/lib/api/types.gen";
import { t } from "@/lib/i18n/t";

export interface DateiKontextMenuProps {
  /** Controls menu visibility. */
  open: boolean;
  /** Called when the menu should open or close. */
  onOpenChange: (open: boolean) => void;
  /** File the menu acts on, or {@code null} while none is targeted. */
  datei: DateiDTO | null;
  /** Invoked when the user picks "Verschieben". */
  onMove: () => void;
  /** Invoked when the user picks "Umbenennen". */
  onRename: () => void;
  /** Invoked when the user picks "Löschen". */
  onDelete: () => void;
}

/**
 * Renders the file context menu.
 *
 * @param props - {@link DateiKontextMenuProps}
 * @returns The dialog-based menu element.
 */
export function DateiKontextMenu({
  open,
  onOpenChange,
  datei,
  onMove,
  onRename,
  onDelete,
}: DateiKontextMenuProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>
            {datei?.name ?? t("dateien.menu.title", "Datei-Aktionen")}
          </DialogTitle>
          <DialogDescription>
            {t(
              "dateien.menu.description",
              "Wähle eine Aktion für die Datei.",
            )}
          </DialogDescription>
        </DialogHeader>
        <View className="gap-2 py-2">
          <MenuItem
            testID="datei-menu-move"
            icon={
              <FolderInput size={16} color="hsl(var(--foreground))" />
            }
            label={t("dateien.menu.move", "Verschieben")}
            onPress={() => {
              onOpenChange(false);
              onMove();
            }}
          />
          <MenuItem
            testID="datei-menu-rename"
            icon={<Pencil size={16} color="hsl(var(--foreground))" />}
            label={t("dateien.menu.rename", "Umbenennen")}
            onPress={() => {
              onOpenChange(false);
              onRename();
            }}
          />
          <MenuItem
            testID="datei-menu-delete"
            icon={<Trash2 size={16} color="hsl(var(--destructive))" />}
            label={t("dateien.menu.delete", "Löschen")}
            onPress={() => {
              onOpenChange(false);
              onDelete();
            }}
            destructive
          />
        </View>
      </DialogContent>
    </Dialog>
  );
}

interface MenuItemProps {
  testID: string;
  icon: React.ReactNode;
  label: string;
  onPress: () => void;
  destructive?: boolean;
}

/**
 * Single tappable row inside the context menu.
 */
function MenuItem({
  testID,
  icon,
  label,
  onPress,
  destructive = false,
}: MenuItemProps) {
  return (
    <Pressable
      testID={testID}
      accessibilityRole="button"
      onPress={onPress}
      className="flex-row items-center gap-3 rounded-md border border-border px-3 py-3 active:bg-accent"
    >
      {icon}
      <Text
        className={
          destructive
            ? "text-sm font-medium text-destructive"
            : "text-sm font-medium text-foreground"
        }
      >
        {label}
      </Text>
    </Pressable>
  );
}
