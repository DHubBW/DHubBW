/**
 * Inline, collapsible folder section used on the mobile Dateien screen.
 *
 * Replaces the previous hamburger-trigger + dialog pair: instead of
 * hiding the tree behind a popup, the section sits permanently above
 * the file list and the user can fold it shut when they want more
 * vertical room. The collapsed header always shows the name of the
 * currently active folder so the user keeps their bearings even when
 * the tree itself is hidden.
 *
 * The design mirrors the desktop sidebar's tree header (small uppercase
 * caption + create button) so the mental model stays the same across
 * breakpoints.
 */
import { ChevronDown, ChevronRight, FolderTree } from "lucide-react-native";
import { useState, type ReactNode } from "react";
import { Pressable, ScrollView, View } from "react-native";

import { Text } from "@/components/ui/text";
import type { OrdnerDTO } from "@/lib/api/types.gen";
import { t } from "@/lib/i18n/t";

import { OrdnerTree } from "./OrdnerTree";

export interface OrdnerTreeInlineProps {
  rootFolders: OrdnerDTO[];
  selectedOrdnerId: string | null;
  selectedFolderName: string | null;
  onSelect: (folder: OrdnerDTO | null) => void;
  onContextRequest?: (folder: OrdnerDTO) => void;
  /** Optional trailing action slot (e.g. the "Ordner erstellen" button). */
  actions?: ReactNode;
}

/**
 * Renders an always-visible, expandable folder-tree panel.
 *
 * @param props - {@link OrdnerTreeInlineProps}
 * @returns The inline tree panel.
 */
export function OrdnerTreeInline({
  rootFolders,
  selectedOrdnerId,
  selectedFolderName,
  onSelect,
  onContextRequest,
  actions,
}: OrdnerTreeInlineProps) {
  // Default to expanded: on a fresh page-load the user usually wants to
  // see the structure. Collapsing is one tap away.
  const [expanded, setExpanded] = useState(true);
  const heading = t("dateien.tree.heading", "Ordner");
  const rootLabel = t("dateien.tree.allFiles", "Alle Dateien");
  const activeLabel = selectedFolderName ?? rootLabel;

  return (
    <View className="overflow-hidden rounded-xl border border-border bg-card">
      <View className="flex-row items-center gap-2 border-b border-border px-3 py-2">
        <Pressable
          onPress={() => setExpanded((v) => !v)}
          accessibilityRole="button"
          accessibilityState={{ expanded }}
          accessibilityLabel={
            expanded
              ? t("dateien.tree.inline.collapse", "Ordner-Übersicht einklappen")
              : t("dateien.tree.inline.expand", "Ordner-Übersicht ausklappen")
          }
          className="flex-1 flex-row items-center gap-2 py-1"
        >
          {expanded ? (
            <ChevronDown size={16} color="hsl(var(--muted-foreground))" />
          ) : (
            <ChevronRight size={16} color="hsl(var(--muted-foreground))" />
          )}
          <FolderTree size={16} color="hsl(var(--foreground))" />
          <Text className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
            {heading}
          </Text>
          {/* Show the active folder name when collapsed so the user
              still sees where they are. */}
          {!expanded && (
            <>
              <Text className="text-xs text-muted-foreground"> · </Text>
              <Text
                className="flex-1 text-xs font-medium text-foreground"
                numberOfLines={1}
              >
                {activeLabel}
              </Text>
            </>
          )}
        </Pressable>
        {actions}
      </View>
      {expanded && (
        <ScrollView
          className="max-h-72"
          // Sub-tree contents may exceed the cap on courses with many
          // root folders; nestedScrollEnabled lets it scroll
          // independently of the outer page on Android.
          nestedScrollEnabled
        >
          <View className="py-1">
            <OrdnerTree
              rootFolders={rootFolders}
              selectedOrdnerId={selectedOrdnerId}
              onSelect={onSelect}
              onContextRequest={onContextRequest}
            />
          </View>
        </ScrollView>
      )}
    </View>
  );
}
