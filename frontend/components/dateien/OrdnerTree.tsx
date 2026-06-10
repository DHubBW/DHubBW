/**
 * Folder tree for the Dateien screen.
 *
 * The tree exposes one root row labelled "Alle Dateien" plus the course's
 * top-level folders. Each branch loads its direct children lazily on first
 * expand via {@link useOrdnerChildren}. Selection is communicated to the
 * parent through `onSelect`; the active row is highlighted by a red
 * background per the demo design pattern.
 *
 * Per-row CRUD entry points are surfaced via {@link onContextRequest}:
 * long-press anywhere on the row (touch) or the always-visible 3-dot
 * icon (mouse / keyboard / touch fallback) hands the folder back to the
 * parent screen, which owns the menu, rename and delete dialogs.
 */
import {
  ChevronDown,
  ChevronRight,
  Folder,
  Globe2,
  Lock,
  MoreVertical,
} from "lucide-react-native";
import { useState } from "react";
import { ActivityIndicator, Pressable, View } from "react-native";

import { Text } from "@/components/ui/text";
import { useOrdnerChildren } from "@/hooks/useOrdnerChildren";
import type { OrdnerDTO } from "@/lib/api/types.gen";
import { folderIconColor } from "@/lib/theme/folderColors";
import { t } from "@/lib/i18n/t";
import { cn } from "@/lib/utils";

const ROW_HEIGHT_CLASS = "py-2";
const INDENT_PX_PER_LEVEL = 16;
const CHEVRON_HIT = 8;

export interface OrdnerTreeProps {
  /** Top-level folders of the current course. */
  rootFolders: OrdnerDTO[];
  /** ID of the currently selected folder (`null` = "Alle Dateien"). */
  selectedOrdnerId: string | null;
  /**
   * Called when the user picks a folder (or the root row). The folder
   * payload (or `null` for the root row) is forwarded so the parent does
   * not have to walk the lazy-loaded tree to map the ID back to its DTO.
   */
  onSelect: (folder: OrdnerDTO | null) => void;
  /** Called when an interaction should close a parent drawer (mobile only). */
  onAfterSelect?: () => void;
  /**
   * Called when the user requests row-level actions on a folder via
   * long-press or the 3-dot icon. The parent screen owns the resulting
   * menu / dialog state; the tree itself is purely presentational.
   */
  onContextRequest?: (folder: OrdnerDTO) => void;
}

/**
 * Renders the folder tree with the synthetic "Alle Dateien" root row.
 *
 * @param props - {@link OrdnerTreeProps}
 * @returns The folder-tree component.
 */
export function OrdnerTree({
  rootFolders,
  selectedOrdnerId,
  onSelect,
  onAfterSelect,
  onContextRequest,
}: OrdnerTreeProps) {
  return (
    <View className="flex-1">
      <RootRow
        selected={selectedOrdnerId === null}
        onPress={() => {
          onSelect(null);
          onAfterSelect?.();
        }}
      />
      <View>
        {rootFolders.map((folder) => (
          <TreeNode
            key={folder.ordnerID}
            folder={folder}
            depth={0}
            selectedOrdnerId={selectedOrdnerId}
            onSelect={onSelect}
            onAfterSelect={onAfterSelect}
            onContextRequest={onContextRequest}
          />
        ))}
      </View>
    </View>
  );
}

interface RootRowProps {
  selected: boolean;
  onPress: () => void;
}

/**
 * Synthetic top entry that selects the "all files" listing.
 *
 * @param props - {@link RootRowProps}
 * @returns The root-row pressable.
 */
function RootRow({ selected, onPress }: RootRowProps) {
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityState={{ selected }}
      className={cn(
        "flex-row items-center gap-2 px-3",
        ROW_HEIGHT_CLASS,
        selected && "bg-destructive/15",
      )}
    >
      <Folder size={16} color="hsl(var(--foreground))" />
      <Text
        className={cn(
          "flex-1 text-sm",
          selected ? "font-semibold text-foreground" : "text-foreground",
        )}
        numberOfLines={1}
      >
        {t("dateien.tree.allFiles", "Alle Dateien")}
      </Text>
    </Pressable>
  );
}

interface TreeNodeProps {
  folder: OrdnerDTO;
  depth: number;
  selectedOrdnerId: string | null;
  onSelect: (folder: OrdnerDTO | null) => void;
  onAfterSelect?: () => void;
  onContextRequest?: (folder: OrdnerDTO) => void;
}

/**
 * Single folder row plus, when expanded, its lazily-loaded children.
 *
 * The chevron and the row body are split into two press targets so users
 * can expand a folder without selecting it (same UX as VS Code's file
 * tree). Long-press on the row body and the 3-dot icon both surface
 * row-level actions through `onContextRequest`.
 *
 * @param props - {@link TreeNodeProps}
 * @returns The folder row and (when expanded) its children list.
 */
function TreeNode({
  folder,
  depth,
  selectedOrdnerId,
  onSelect,
  onAfterSelect,
  onContextRequest,
}: TreeNodeProps) {
  const [expanded, setExpanded] = useState(false);
  const childrenQuery = useOrdnerChildren(expanded ? folder.ordnerID : null);
  const isSelected = selectedOrdnerId === folder.ordnerID;

  const PublicIcon = folder.public ? Globe2 : Lock;
  const publicLabel = folder.public
    ? t("dateien.tree.public", "Öffentlich")
    : t("dateien.tree.private", "Privat");

  return (
    <View>
      <View
        className={cn(
          "flex-row items-center gap-2 px-3",
          ROW_HEIGHT_CLASS,
          isSelected && "bg-destructive/15",
        )}
        style={{ paddingLeft: 12 + depth * INDENT_PX_PER_LEVEL }}
      >
        <Pressable
          onPress={() => setExpanded((v) => !v)}
          hitSlop={CHEVRON_HIT}
          accessibilityRole="button"
          accessibilityLabel={
            expanded
              ? t("dateien.tree.collapse", "Ordner einklappen")
              : t("dateien.tree.expand", "Ordner aufklappen")
          }
        >
          {expanded ? (
            <ChevronDown size={14} color="hsl(var(--muted-foreground))" />
          ) : (
            <ChevronRight size={14} color="hsl(var(--muted-foreground))" />
          )}
        </Pressable>
        <Pressable
          onPress={() => {
            onSelect(folder);
            setExpanded(true);
            onAfterSelect?.();
          }}
          onLongPress={
            onContextRequest ? () => onContextRequest(folder) : undefined
          }
          accessibilityRole="button"
          accessibilityState={{ selected: isSelected }}
          className="flex-1 flex-row items-center gap-2"
        >
          <Folder size={16} color={folderIconColor(folder.farbe)} />
          <Text
            className={cn(
              "flex-1 text-sm",
              isSelected
                ? "font-semibold text-foreground"
                : "text-foreground",
            )}
            numberOfLines={1}
          >
            {folder.name}
          </Text>
          <View
            className="flex-row items-center gap-1"
            accessibilityLabel={publicLabel}
          >
            <PublicIcon size={12} color="hsl(var(--muted-foreground))" />
          </View>
        </Pressable>
        {onContextRequest && (
          <Pressable
            onPress={() => onContextRequest(folder)}
            hitSlop={CHEVRON_HIT}
            accessibilityRole="button"
            accessibilityLabel={t(
              "dateien.tree.actions",
              "Ordner-Aktionen öffnen",
            )}
            testID={`ordner-tree-actions-${folder.ordnerID}`}
            className="rounded-md p-1 active:bg-accent"
          >
            <MoreVertical size={14} color="hsl(var(--muted-foreground))" />
          </Pressable>
        )}
      </View>
      {expanded && (
        <ChildrenList
          isLoading={childrenQuery.isLoading}
          isError={childrenQuery.isError}
          items={childrenQuery.data ?? []}
          depth={depth + 1}
          selectedOrdnerId={selectedOrdnerId}
          onSelect={onSelect}
          onAfterSelect={onAfterSelect}
          onContextRequest={onContextRequest}
        />
      )}
    </View>
  );
}

interface ChildrenListProps {
  isLoading: boolean;
  isError: boolean;
  items: OrdnerDTO[];
  depth: number;
  selectedOrdnerId: string | null;
  onSelect: (folder: OrdnerDTO | null) => void;
  onAfterSelect?: () => void;
  onContextRequest?: (folder: OrdnerDTO) => void;
}

/**
 * Inline loading / error / empty / list state for an expanded node.
 *
 * @param props - {@link ChildrenListProps}
 * @returns The children-area renderable.
 */
function ChildrenList({
  isLoading,
  isError,
  items,
  depth,
  selectedOrdnerId,
  onSelect,
  onAfterSelect,
  onContextRequest,
}: ChildrenListProps) {
  if (isLoading) {
    return (
      <View
        className="flex-row items-center gap-2 py-2"
        style={{ paddingLeft: 12 + depth * INDENT_PX_PER_LEVEL }}
      >
        <ActivityIndicator size="small" color="hsl(var(--muted-foreground))" />
        <Text className="text-xs text-muted-foreground">
          {t("dateien.tree.loading", "Lade Unterordner …")}
        </Text>
      </View>
    );
  }
  if (isError) {
    return (
      <View
        className="py-2"
        style={{ paddingLeft: 12 + depth * INDENT_PX_PER_LEVEL }}
      >
        <Text className="text-xs text-destructive">
          {t(
            "dateien.tree.error",
            "Unterordner konnten nicht geladen werden.",
          )}
        </Text>
      </View>
    );
  }
  if (items.length === 0) {
    return (
      <View
        className="py-2"
        style={{ paddingLeft: 12 + depth * INDENT_PX_PER_LEVEL }}
      >
        <Text className="text-xs text-muted-foreground">
          {t("dateien.tree.empty", "Keine Unterordner")}
        </Text>
      </View>
    );
  }
  return (
    <View>
      {items.map((child) => (
        <TreeNode
          key={child.ordnerID}
          folder={child}
          depth={depth}
          selectedOrdnerId={selectedOrdnerId}
          onSelect={onSelect}
          onAfterSelect={onAfterSelect}
          onContextRequest={onContextRequest}
        />
      ))}
    </View>
  );
}
