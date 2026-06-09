/**
 * Mobile drawer that wraps the {@link OrdnerTree} in a Dialog. Triggered
 * by a hamburger-style button on screens narrower than 768 px so the tree
 * does not have to share width with the file list.
 */
import { Menu } from "lucide-react-native";
import { useState } from "react";
import { Pressable, ScrollView, View } from "react-native";

import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Text } from "@/components/ui/text";
import type { OrdnerDTO } from "@/lib/api/types.gen";
import { t } from "@/lib/i18n/t";

import { OrdnerTree } from "./OrdnerTree";

export interface OrdnerTreeDrawerProps {
  rootFolders: OrdnerDTO[];
  selectedOrdnerId: string | null;
  onSelect: (folder: OrdnerDTO | null) => void;
}

/**
 * Renders the hamburger trigger that opens a modal containing the tree.
 *
 * @param props - {@link OrdnerTreeDrawerProps}
 * @returns The drawer trigger and dialog.
 */
export function OrdnerTreeDrawer({
  rootFolders,
  selectedOrdnerId,
  onSelect,
}: OrdnerTreeDrawerProps) {
  const [open, setOpen] = useState(false);
  const triggerLabel = t("dateien.drawer.open", "Ordner");
  return (
    <>
      <Pressable
        onPress={() => setOpen(true)}
        accessibilityRole="button"
        accessibilityLabel={triggerLabel}
        className="flex-row items-center gap-2 self-start rounded-md border border-border bg-card px-3 py-2"
      >
        <Menu size={16} color="hsl(var(--foreground))" />
        <Text className="text-sm font-medium text-foreground">
          {triggerLabel}
        </Text>
      </Pressable>
      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="max-h-[80vh] sm:max-w-md">
          <DialogHeader>
            <DialogTitle>
              {t("dateien.drawer.title", "Ordner-Übersicht")}
            </DialogTitle>
          </DialogHeader>
          <ScrollView className="max-h-[60vh]">
            <View>
              <OrdnerTree
                rootFolders={rootFolders}
                selectedOrdnerId={selectedOrdnerId}
                onSelect={onSelect}
                onAfterSelect={() => setOpen(false)}
              />
            </View>
          </ScrollView>
        </DialogContent>
      </Dialog>
    </>
  );
}
