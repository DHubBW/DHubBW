/**
 * Modal dialog for the "Datei verschieben"-Aktion.
 *
 * <p>The user picks a target folder via a lazy-loaded folder tree
 * (re-uses {@link useOrdnerTree} + {@link useOrdnerChildren}) and
 * confirms via a "Hierhin verschieben"-button. The mutation runs
 * through {@link useDateiMove}; errors surface as a status-aware
 * toast so the user sees the precise reason
 * (409 → "Eine Datei mit diesem Namen existiert bereits …",
 *  403 → "Keine Berechtigung im Zielordner",
 *  400 → "Datei liegt bereits in diesem Ordner").</p>
 *
 * <p>The currently-selected folder is excluded from the picker (you
 * can't move into the folder the file already lives in). The
 * synthetic "Alle Dateien" root is also hidden — files always live
 * inside an actual folder.</p>
 */
import { ChevronDown, ChevronRight, Folder } from "lucide-react-native";
import { useEffect, useState } from "react";
import { ActivityIndicator, Pressable, ScrollView, View } from "react-native";
import { toast } from "sonner-native";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Text } from "@/components/ui/text";
import { useDateiMove } from "@/hooks/useDateiMove";
import { useOrdnerChildren } from "@/hooks/useOrdnerChildren";
import { useOrdnerTree } from "@/hooks/useOrdnerTree";
import { toastForApiError } from "@/lib/api/toastForApiError";
import type { DateiDTO, OrdnerDTO } from "@/lib/api/types.gen";
import { folderIconColor } from "@/lib/theme/folderColors";
import { t } from "@/lib/i18n/t";
import { cn } from "@/lib/utils";

const ROW_HEIGHT_CLASS = "py-2";
const INDENT_PX_PER_LEVEL = 16;
const CHEVRON_HIT = 8;

export interface MoveDateiDialogProps {
  /** Controls dialog visibility. */
  open: boolean;
  /** Called when the dialog should open or close. */
  onOpenChange: (open: boolean) => void;
  /** File metadata. {@code null} while no row is targeted. */
  datei: DateiDTO | null;
  /**
   * UUID of the file's current folder, or {@code null} for the
   * synthetic root view. Forwarded to the move mutation for cache
   * invalidation.
   */
  sourceOrdnerId: string | null;
}

/**
 * Maps a thrown error to the toast message shown to the user.
 */
function _moveErrorMessage(err: unknown): string {
  return toastForApiError(err, {
    400: t(
      "dateien.move.error.sameFolder",
      "Datei liegt bereits in diesem Ordner.",
    ),
    403: t(
      "dateien.move.error.forbidden",
      "Keine Berechtigung zum Verschieben in diesen Ordner.",
    ),
    404: t(
      "dateien.move.error.notFound",
      "Datei oder Zielordner nicht gefunden.",
    ),
    409: t(
      "dateien.move.error.duplicateName",
      "Eine Datei mit diesem Namen existiert bereits im Zielordner.",
    ),
  });
}

/**
 * Renders the move-file dialog.
 *
 * @param props - {@link MoveDateiDialogProps}
 * @returns The dialog element.
 */
export function MoveDateiDialog({
  open,
  onOpenChange,
  datei,
  sourceOrdnerId,
}: MoveDateiDialogProps) {
  const move = useDateiMove();
  const [selectedFolder, setSelectedFolder] = useState<OrdnerDTO | null>(null);

  // Reset the selection whenever the dialog re-opens.
  useEffect(() => {
    if (open) setSelectedFolder(null);
  }, [open]);

  const tree = useOrdnerTree();
  const rootFolders = tree.data ?? [];

  const canSubmit =
    !move.isPending
    && datei !== null
    && selectedFolder !== null
    && selectedFolder.ordnerID !== sourceOrdnerId;

  function handleOpenChange(nextOpen: boolean) {
    if (move.isPending) return;
    onOpenChange(nextOpen);
  }

  function handleSubmit() {
    if (!canSubmit || datei === null || selectedFolder === null) return;
    move.mutate(
      {
        dateiId: datei.dateiID,
        targetOrdnerId: selectedFolder.ordnerID,
        sourceOrdnerId,
      },
      {
        onSuccess: () => {
          toast.success(t("dateien.move.success", "Datei verschoben"));
          onOpenChange(false);
        },
        onError: (err) => {
          toast.error(_moveErrorMessage(err));
        },
      },
    );
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>
            {t("dateien.move.title", "Datei verschieben")}
          </DialogTitle>
          <DialogDescription>
            {datei
              ? t(
                  "dateien.move.descriptionWithName",
                  "Wähle den Zielordner für \"{name}\".",
                ).replace("{name}", datei.name)
              : t(
                  "dateien.move.description",
                  "Wähle den Zielordner.",
                )}
          </DialogDescription>
        </DialogHeader>
        <ScrollView className="max-h-72 rounded-md border border-border bg-background">
          {tree.isLoading && (
            <View className="flex-row items-center gap-2 px-3 py-3">
              <ActivityIndicator
                size="small"
                color="hsl(var(--muted-foreground))"
              />
              <Text className="text-xs text-muted-foreground">
                {t("dateien.move.loading", "Lade Ordner …")}
              </Text>
            </View>
          )}
          {tree.isError && (
            <View className="px-3 py-3">
              <Text className="text-xs text-destructive">
                {t(
                  "dateien.move.error.tree",
                  "Ordnerbaum konnte nicht geladen werden.",
                )}
              </Text>
            </View>
          )}
          {!tree.isLoading
            && !tree.isError
            && rootFolders.length === 0 && (
            <View className="px-3 py-3">
              <Text className="text-xs text-muted-foreground">
                {t(
                  "dateien.move.empty",
                  "Es sind noch keine Ordner verfügbar.",
                )}
              </Text>
            </View>
          )}
          {!tree.isLoading
            && !tree.isError
            && rootFolders.map((folder) => (
              <FolderNode
                key={folder.ordnerID}
                folder={folder}
                depth={0}
                disabledOrdnerId={sourceOrdnerId}
                selectedOrdnerId={selectedFolder?.ordnerID ?? null}
                onSelect={setSelectedFolder}
              />
            ))}
        </ScrollView>
        <DialogFooter>
          <Button
            testID="move-dialog-cancel"
            variant="outline"
            onPress={() => handleOpenChange(false)}
            disabled={move.isPending}
          >
            <Text>{t("confirm.cancel", "Abbrechen")}</Text>
          </Button>
          <Button
            testID="move-dialog-confirm"
            onPress={handleSubmit}
            disabled={!canSubmit}
          >
            {move.isPending ? (
              <ActivityIndicator size="small" color="white" />
            ) : (
              <Text>
                {t("dateien.move.confirm", "Hierhin verschieben")}
              </Text>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

interface FolderNodeProps {
  folder: OrdnerDTO;
  depth: number;
  disabledOrdnerId: string | null;
  selectedOrdnerId: string | null;
  onSelect: (folder: OrdnerDTO) => void;
}

/**
 * Single folder row inside the move-target tree. The chevron
 * expands the branch (firing {@link useOrdnerChildren} lazily); the
 * row body picks the folder as the move target. Folders matching
 * {@code disabledOrdnerId} are rendered greyed-out and ignore
 * selection — that's the file's current folder.
 */
function FolderNode({
  folder,
  depth,
  disabledOrdnerId,
  selectedOrdnerId,
  onSelect,
}: FolderNodeProps) {
  const [expanded, setExpanded] = useState(false);
  const canExpand = folder.hasChildren;
  const childrenQuery = useOrdnerChildren(
    canExpand && expanded ? folder.ordnerID : null,
  );
  const isDisabled = folder.ordnerID === disabledOrdnerId;
  const isSelected = folder.ordnerID === selectedOrdnerId;

  return (
    <View>
      <View
        className={cn(
          "flex-row items-center gap-2 px-3",
          ROW_HEIGHT_CLASS,
          isSelected && "bg-primary/10",
          isDisabled && "opacity-50",
        )}
        style={{ paddingLeft: 12 + depth * INDENT_PX_PER_LEVEL }}
      >
        {canExpand ? (
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
        ) : (
          <View style={{ width: 14 }} />
        )}
        <Pressable
          onPress={() => {
            if (isDisabled) return;
            onSelect(folder);
          }}
          accessibilityRole="button"
          accessibilityState={{ selected: isSelected, disabled: isDisabled }}
          disabled={isDisabled}
          className="flex-1 flex-row items-center gap-2"
        >
          <Folder size={16} color={folderIconColor(folder.farbe)} />
          <Text
            className={cn(
              "flex-1 text-sm",
              isSelected ? "font-semibold text-foreground" : "text-foreground",
            )}
            numberOfLines={1}
          >
            {folder.name}
          </Text>
          {isDisabled && (
            <Text className="text-[10px] uppercase tracking-wide text-muted-foreground">
              {t("dateien.move.currentFolder", "Aktuell")}
            </Text>
          )}
        </Pressable>
      </View>
      {expanded && (
        <ChildrenList
          isLoading={childrenQuery.isLoading}
          isError={childrenQuery.isError}
          items={childrenQuery.data ?? []}
          depth={depth + 1}
          disabledOrdnerId={disabledOrdnerId}
          selectedOrdnerId={selectedOrdnerId}
          onSelect={onSelect}
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
  disabledOrdnerId: string | null;
  selectedOrdnerId: string | null;
  onSelect: (folder: OrdnerDTO) => void;
}

function ChildrenList({
  isLoading,
  isError,
  items,
  depth,
  disabledOrdnerId,
  selectedOrdnerId,
  onSelect,
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
        <FolderNode
          key={child.ordnerID}
          folder={child}
          depth={depth}
          disabledOrdnerId={disabledOrdnerId}
          selectedOrdnerId={selectedOrdnerId}
          onSelect={onSelect}
        />
      ))}
    </View>
  );
}
