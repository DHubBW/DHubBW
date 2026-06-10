/**
 * Folder-row context menu shown via long-press (touch) or the row's
 * 3-dot icon (mouse / keyboard / touch fallback).
 *
 * The menu is implemented as a small modal sheet rather than a floating
 * popover so the same component renders correctly on Web, iOS and
 * Android — `onContextMenu` is React-DOM-only and proper cross-platform
 * popovers would need a third-party primitive that isn't currently
 * installed.
 *
 * The menu is purely orchestrational — it owns no mutation state of
 * its own and relays the selected action back to the parent through
 * `onRename` / `onDelete`. Authorisation is enforced by the backend
 * (`KURSADMIN`, `SYSTEMADMIN`, or folder creator); on failure the
 * downstream dialog surfaces the 403 toast.
 */
import { Pencil, Trash2 } from "lucide-react-native";
import { Pressable, View } from "react-native";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Text } from "@/components/ui/text";
import type { OrdnerDTO } from "@/lib/api/types.gen";
import { t } from "@/lib/i18n/t";

export interface OrdnerKontextMenuProps {
  /** Controls menu visibility. */
  open: boolean;
  /** Called when the menu should open or close. */
  onOpenChange: (open: boolean) => void;
  /** Folder the menu acts on, or `null` while none is targeted. */
  ordner: OrdnerDTO | null;
  /** Invoked when the user picks "Umbenennen". */
  onRename: () => void;
  /** Invoked when the user picks "Löschen". */
  onDelete: () => void;
}

/**
 * Renders the folder context menu.
 *
 * @param props - {@link OrdnerKontextMenuProps}
 * @returns The dialog-based menu element.
 */
export function OrdnerKontextMenu({
  open,
  onOpenChange,
  ordner,
  onRename,
  onDelete,
}: OrdnerKontextMenuProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>
            {ordner?.name ?? t("ordner.menu.title", "Ordner-Aktionen")}
          </DialogTitle>
          <DialogDescription>
            {t("ordner.menu.description", "Wähle eine Aktion für den Ordner.")}
          </DialogDescription>
        </DialogHeader>
        <View className="gap-2 py-2">
          <MenuItem
            testID="ordner-menu-rename"
            icon={<Pencil size={16} color="hsl(var(--foreground))" />}
            label={t("ordner.menu.rename", "Umbenennen")}
            onPress={() => {
              onOpenChange(false);
              onRename();
            }}
          />
          <MenuItem
            testID="ordner-menu-delete"
            icon={<Trash2 size={16} color="hsl(var(--destructive))" />}
            label={t("ordner.menu.delete", "Löschen")}
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
 *
 * @param props - {@link MenuItemProps}
 * @returns The pressable menu-item element.
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
