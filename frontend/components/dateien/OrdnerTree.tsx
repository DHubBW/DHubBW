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
 *
 * <strong>Expansion model:</strong> the set of currently-expanded folder
 * IDs is owned at the {@link OrdnerTree} level so that picking a new
 * folder can automatically collapse every branch that is no longer on
 * the path from the root to the new selection. The path is reconstructed
 * by walking each open node's `ueberOrdnerID`. The synthetic "Alle
 * Dateien" root and any leaf folder (`hasChildren === false`) collapse
 * everything else.
 */
import {
  ChevronDown,
  ChevronRight,
  Folder,
  Globe2,
  Lock,
  MoreVertical,
} from "lucide-react-native";
import { useCallback, useMemo, useState } from "react";
import { ActivityIndicator, Pressable, View } from "react-native";

import { Text } from "@/components/ui/text";
import { useOrdnerChildren } from "@/hooks/useOrdnerChildren";
import type { OrdnerDTO } from "@/lib/api/types.gen";
import { folderIconColor } from "@/lib/theme/folderColors";
import { t } from "@/lib/i18n/t";
import { cn } from "@/lib/utils";
import { useQueryClient } from "@tanstack/react-query";

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
  // Set of expanded folder IDs — owned here so a new selection can
  // collapse every branch that is no longer on the ancestor path.
  const [expanded, setExpanded] = useState<Set<string>>(new Set());
  const queryClient = useQueryClient();

  /**
   * Walk a folder upward via its `ueberOrdnerID`, hopping through the
   * already-cached children query results to find each parent. Returns
   * the set of ancestor IDs (excluding the folder itself).
   */
  const ancestorsOf = useCallback(
    (folder: OrdnerDTO): Set<string> => {
      const ancestors = new Set<string>();
      let parentId = folder.ueberOrdnerID;
      // Walk via cached children-query results — every parent we ever
      // expanded has its children list cached, and every child we render
      // came from that list. Limit the loop so we never spin on a
      // pathological cycle (defensive — the backend enforces a tree).
      const guard = 64;
      let hops = 0;
      while (parentId && hops < guard) {
        ancestors.add(parentId);
        const parent = findCachedFolder(
          parentId, rootFolders, queryClient,
        );
        if (!parent) break;
        parentId = parent.ueberOrdnerID;
        hops += 1;
      }
      return ancestors;
    },
    [rootFolders, queryClient],
  );

  const handleSelect = useCallback(
    (folder: OrdnerDTO | null) => {
      onSelect(folder);
      if (folder === null) {
        // "Alle Dateien" root: collapse everything.
        setExpanded(new Set());
        onAfterSelect?.();
        return;
      }
      // Keep only the selected folder + its ancestors expanded. Auto-
      // expand the selected folder itself when it has children so the
      // user immediately sees the next level after navigating in.
      const next = ancestorsOf(folder);
      if (folder.hasChildren) next.add(folder.ordnerID);
      setExpanded(next);
      onAfterSelect?.();
    },
    [ancestorsOf, onAfterSelect, onSelect],
  );

  const toggle = useCallback((folderId: string) => {
    setExpanded((prev) => {
      const next = new Set(prev);
      if (next.has(folderId)) next.delete(folderId);
      else next.add(folderId);
      return next;
    });
  }, []);

  const expandedRef = useMemo(() => expanded, [expanded]);

  return (
    <View className="flex-1">
      <RootRow
        selected={selectedOrdnerId === null}
        onPress={() => handleSelect(null)}
      />
      <View>
        {rootFolders.map((folder) => (
          <TreeNode
            key={folder.ordnerID}
            folder={folder}
            depth={0}
            selectedOrdnerId={selectedOrdnerId}
            onSelect={handleSelect}
            onContextRequest={onContextRequest}
            expandedSet={expandedRef}
            onToggle={toggle}
          />
        ))}
      </View>
    </View>
  );
}

/**
 * Look up a folder DTO by ID across the root list and every cached
 * children-query result. Returns {@code undefined} when the ID is not
 * present in any cached slice yet.
 */
function findCachedFolder(
  id: string,
  rootFolders: OrdnerDTO[],
  queryClient: ReturnType<typeof useQueryClient>,
): OrdnerDTO | undefined {
  const inRoot = rootFolders.find((f) => f.ordnerID === id);
  if (inRoot) return inRoot;
  // Scan every cached children-query: each one's data array might hold
  // the folder. The lookup is O(n) per cache miss but the cache size
  // is bounded by the depth/breadth of the user's open path, so this
  // stays cheap in practice.
  const queries = queryClient
    .getQueryCache()
    .findAll({ queryKey: ["ordner", "children"] });
  for (const q of queries) {
    const data = q.state.data as OrdnerDTO[] | undefined;
    if (!data) continue;
    const hit = data.find((f) => f.ordnerID === id);
    if (hit) return hit;
  }
  return undefined;
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
  onContextRequest?: (folder: OrdnerDTO) => void;
  expandedSet: Set<string>;
  onToggle: (folderId: string) => void;
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
  onContextRequest,
  expandedSet,
  onToggle,
}: TreeNodeProps) {
  // Leaf folders (no children) get neither a chevron nor any expanded
  // children area — there is nothing to load and nothing to show.
  const canExpand = folder.hasChildren;
  const expanded = canExpand && expandedSet.has(folder.ordnerID);
  const childrenQuery = useOrdnerChildren(
    expanded ? folder.ordnerID : null,
  );
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
        {canExpand ? (
          <Pressable
            onPress={() => onToggle(folder.ordnerID)}
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
        ) : (
          // Empty placeholder of the same width keeps the folder names
          // aligned with their expandable siblings — the indent column
          // is reserved for the chevron at this depth either way.
          <View style={{ width: 14 }} />
        )}
        <Pressable
          onPress={() => onSelect(folder)}
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
          onContextRequest={onContextRequest}
          expandedSet={expandedSet}
          onToggle={onToggle}
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
  onContextRequest?: (folder: OrdnerDTO) => void;
  expandedSet: Set<string>;
  onToggle: (folderId: string) => void;
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
  onContextRequest,
  expandedSet,
  onToggle,
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
          onContextRequest={onContextRequest}
          expandedSet={expandedSet}
          onToggle={onToggle}
        />
      ))}
    </View>
  );
}
