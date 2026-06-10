/**
 * Dateien screen — folder-tree navigation plus the matching file list.
 *
 * Layout:
 * - Wide (≥ 768 px): folder tree on the left, file table on the right.
 * - Narrow (< 768 px): hamburger drawer for the tree, card list for the
 *   files.
 *
 * Selection state holds the currently focused folder UUID, or `null`
 * for the synthetic "Alle Dateien" root view; the file query reacts to
 * that selection.
 *
 * CRUD wiring lives at this level: the screen owns the Rename and
 * Delete dialog state, while {@link UploadButton} wraps the upload
 * mutation, {@link useDateiDownload} drives the browser's save dialog,
 * and the per-row icon group is rendered by {@link RowActions} via the
 * table/card-list slots.
 *
 * Folder CRUD lives here too: a "Ordner erstellen" button in the toolbar
 * and per-row long-press / 3-dot context menus on the tree drive the
 * create / rename / delete dialogs.
 */
import { FolderPlus } from "lucide-react-native";
import { useState } from "react";
import {
  ActivityIndicator,
  Platform,
  Pressable,
  ScrollView,
  useWindowDimensions,
  View,
} from "react-native";
import { toast } from "sonner-native";

import { CourseRoute } from "@/components/auth/CourseRoute";
import { ConfirmDialog } from "@/components/ConfirmDialog";
import { CreateOrdnerDialog } from "@/components/dateien/CreateOrdnerDialog";
import { DateienCardList } from "@/components/dateien/DateienCardList";
import { DateienTabelle } from "@/components/dateien/DateienTabelle";
import { DateienToolbar } from "@/components/dateien/DateienToolbar";
import { DeleteOrdnerConfirm } from "@/components/dateien/DeleteOrdnerConfirm";
import { ErrorHint, NoTopLevelHint } from "@/components/dateien/EmptyStates";
import { OrdnerKontextMenu } from "@/components/dateien/OrdnerKontextMenu";
import { OrdnerTree } from "@/components/dateien/OrdnerTree";
import { OrdnerTreeDrawer } from "@/components/dateien/OrdnerTreeDrawer";
import { RenameDialog } from "@/components/dateien/RenameDialog";
import { RenameOrdnerDialog } from "@/components/dateien/RenameOrdnerDialog";
import { UploadButton } from "@/components/dateien/UploadButton";
import { Text } from "@/components/ui/text";
import { useDateienListe } from "@/hooks/useDateienListe";
import { useDateiDelete } from "@/hooks/useDateiDelete";
import { useDateiDownload } from "@/hooks/useDateiDownload";
import { useOrdnerTree } from "@/hooks/useOrdnerTree";
import { ApiError } from "@/lib/api/errors";
import type { DateiDTO, OrdnerDTO } from "@/lib/api/types.gen";
import { useAuth } from "@/lib/auth/AuthContext";
import { t } from "@/lib/i18n/t";

const TABLE_BREAKPOINT_PX = 768;

/**
 * Files screen. Wrapped in {@link CourseRoute} so users without a course
 * are redirected to the dashboard rather than seeing an unauthorised
 * empty list.
 *
 * @returns The Dateien screen.
 */
export default function DateienScreen() {
  return (
    <CourseRoute>
      <DateienScreenInner />
    </CourseRoute>
  );
}

/**
 * Maps an {@link ApiError} from the delete endpoint to the toast text
 * shown to the user.
 *
 * @param err - The thrown error from the mutation's onError callback.
 * @returns The localised toast message.
 */
function _deleteErrorMessage(err: unknown): string {
  if (err instanceof ApiError) {
    if (err.status === 403) {
      return t(
        "dateien.delete.error.forbidden",
        "Keine Berechtigung zum Löschen.",
      );
    }
    if (err.status === 0 && err.message.includes("404")) {
      return t("dateien.delete.error.notFound", "Datei nicht gefunden.");
    }
    if (err.status === 0) {
      return t("error.network", "Netzwerkfehler – Server nicht erreichbar");
    }
  }
  return t("dateien.delete.error.unknown", "Löschen fehlgeschlagen.");
}

/**
 * Inner screen body — extracted so the {@link CourseRoute} guard can wrap
 * it without re-rendering its hooks until the user is course-cleared.
 *
 * @returns The screen body.
 */
function DateienScreenInner() {
  const { width } = useWindowDimensions();
  const isWide = width >= TABLE_BREAKPOINT_PX;
  const isWeb = Platform.OS === "web";
  const { user } = useAuth();

  const canEditOrdner =
    user?.studentType === "KURSADMIN" ||
    user?.studentType === "SYSTEMADMIN";

  const [selectedFolder, setSelectedFolder] = useState<OrdnerDTO | null>(null);
  const selectedOrdnerId = selectedFolder?.ordnerID ?? null;

  // Datei-CRUD dialog state — `target` holds the row currently being acted on.
  const [renameTarget, setRenameTarget] = useState<DateiDTO | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<DateiDTO | null>(null);

  // Ordner-CRUD dialog state.
  const [createOrdnerOpen, setCreateOrdnerOpen] = useState(false);
  const [contextOrdner, setContextOrdner] = useState<OrdnerDTO | null>(null);
  const [contextMenuOpen, setContextMenuOpen] = useState(false);
  const [renameOrdnerOpen, setRenameOrdnerOpen] = useState(false);
  const [deleteOrdnerOpen, setDeleteOrdnerOpen] = useState(false);

  const ordnerTree = useOrdnerTree();
  const dateienListe = useDateienListe(selectedOrdnerId);
  const download = useDateiDownload();
  const deleteMutation = useDateiDelete();

  const rootFolders = ordnerTree.data ?? [];

  function handleDownload(datei: DateiDTO) {
    if (!isWeb) {
      toast(
        t(
          "dateien.download.nativeStub",
          "Datei-Download ist auf Native im MVP noch nicht verfügbar.",
        ),
      );
      return;
    }
    download.mutate(
      {
        dateiId: datei.dateiID,
        name: datei.name,
        ordnerId: selectedOrdnerId,
      },
      {
        onSuccess: () => {
          toast.success(
            t("dateien.download.success", "Datei heruntergeladen"),
          );
        },
        onError: (err) => {
          if (err.code === "forbidden") {
            toast.error(
              t(
                "dateien.download.error.forbidden",
                "Keine Berechtigung zum Herunterladen.",
              ),
            );
            return;
          }
          if (err.code === "not-found") {
            toast.error(
              t(
                "dateien.download.error.notFound",
                "Datei nicht gefunden.",
              ),
            );
            return;
          }
          if (err.code === "network") {
            toast.error(
              t(
                "error.network",
                "Netzwerkfehler – Server nicht erreichbar",
              ),
            );
            return;
          }
          toast.error(
            t(
              "dateien.download.error.unknown",
              "Download fehlgeschlagen.",
            ),
          );
        },
      },
    );
  }

  async function handleConfirmDelete() {
    if (!deleteTarget) return;
    try {
      await deleteMutation.mutateAsync({
        dateiId: deleteTarget.dateiID,
        ordnerId: selectedOrdnerId,
      });
      toast.success(t("dateien.delete.success", "Datei gelöscht"));
      setDeleteTarget(null);
    } catch (err) {
      toast.error(_deleteErrorMessage(err));
      // ConfirmDialog stays open on rejection; rethrow so it knows.
      throw err;
    }
  }

  function handleContextRequest(folder: OrdnerDTO) {
    setContextOrdner(folder);
    setContextMenuOpen(true);
  }

  if (ordnerTree.isLoading) {
    return (
      <View className="flex-1 items-center justify-center bg-background">
        <ActivityIndicator color="hsl(var(--muted-foreground))" />
      </View>
    );
  }
  if (ordnerTree.isError) {
    return (
      <View className="flex-1 bg-background">
        <ErrorHint />
      </View>
    );
  }
  if (rootFolders.length === 0 && selectedOrdnerId === null) {
    // Course exists but has no top-level folders — show the hint plus a
    // toolbar holding the "Ordner erstellen"-button so the user can
    // bootstrap the tree without any other entry point.
    return (
      <View className="flex-1 bg-background">
        <Header />
        <View className="flex-row justify-end px-4 py-3">
          <CreateOrdnerButton onPress={() => setCreateOrdnerOpen(true)} />
        </View>
        <NoTopLevelHint />
        <CreateOrdnerDialog
          open={createOrdnerOpen}
          onOpenChange={setCreateOrdnerOpen}
          ueberOrdnerId={selectedOrdnerId}
        />
      </View>
    );
  }

  return (
    <View className="flex-1 flex-row bg-background">
      {isWide && (
        <View className="w-72 border-r border-border">
          <ScrollView className="flex-1">
            <View className="py-3">
              <View className="flex-row items-center justify-between gap-2 px-4 pb-2">
                <Text className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                  {t("dateien.tree.heading", "Ordner")}
                </Text>
                <CreateOrdnerButton
                  onPress={() => setCreateOrdnerOpen(true)}
                  compact
                />
              </View>
              <OrdnerTree
                rootFolders={rootFolders}
                selectedOrdnerId={selectedOrdnerId}
                onSelect={setSelectedFolder}
                onContextRequest={
                  canEditOrdner ? handleContextRequest : undefined
                }
              />
            </View>
          </ScrollView>
        </View>
      )}
      <View className="flex-1">
        <Header />
        {!isWide && (
          <View className="flex-row items-center justify-between gap-2 px-4 pt-3">
            <OrdnerTreeDrawer
              rootFolders={rootFolders}
              selectedOrdnerId={selectedOrdnerId}
              onSelect={setSelectedFolder}
            />
            <CreateOrdnerButton onPress={() => setCreateOrdnerOpen(true)} />
          </View>
        )}
        <DateienToolbar
          selected={selectedFolder}
          actions={<UploadButton ueberordnerId={selectedOrdnerId} />}
        />
        {isWide ? (
          <DateienTabelle
            dateien={dateienListe.data ?? []}
            isLoading={dateienListe.isLoading}
            isError={dateienListe.isError}
            onDownload={handleDownload}
            onRename={setRenameTarget}
            onDelete={setDeleteTarget}
          />
        ) : (
          <DateienCardList
            dateien={dateienListe.data ?? []}
            isLoading={dateienListe.isLoading}
            isError={dateienListe.isError}
            onDownload={handleDownload}
            onRename={setRenameTarget}
            onDelete={setDeleteTarget}
          />
        )}
      </View>

      <RenameDialog
        open={renameTarget !== null}
        onOpenChange={(next) => {
          if (!next) setRenameTarget(null);
        }}
        dateiId={renameTarget?.dateiID ?? null}
        currentName={renameTarget?.name ?? ""}
        ordnerId={selectedOrdnerId}
      />

      <ConfirmDialog
        open={deleteTarget !== null}
        onOpenChange={(next) => {
          if (!next) setDeleteTarget(null);
        }}
        title={t("dateien.delete.title", "Datei löschen?")}
        message={t(
          "dateien.delete.message",
          "'{name}' wird unwiderruflich gelöscht.",
        ).replace("{name}", deleteTarget?.name ?? "")}
        confirmLabel={t("dateien.delete.confirm", "Löschen")}
        variant="destructive"
        onConfirm={handleConfirmDelete}
      />

      <CreateOrdnerDialog
        open={createOrdnerOpen}
        onOpenChange={setCreateOrdnerOpen}
        ueberOrdnerId={selectedOrdnerId}
      />

      <OrdnerKontextMenu
        open={contextMenuOpen}
        onOpenChange={setContextMenuOpen}
        ordner={contextOrdner}
        onRename={() => setRenameOrdnerOpen(true)}
        onDelete={() => setDeleteOrdnerOpen(true)}
      />

      <RenameOrdnerDialog
        open={renameOrdnerOpen}
        onOpenChange={(next) => {
          setRenameOrdnerOpen(next);
          if (!next) setContextOrdner(null);
        }}
        ordnerId={contextOrdner?.ordnerID ?? null}
        currentName={contextOrdner?.name ?? ""}
        ueberOrdnerId={contextOrdner?.ueberOrdnerID ?? null}
      />

      <DeleteOrdnerConfirm
        open={deleteOrdnerOpen}
        onOpenChange={(next) => {
          setDeleteOrdnerOpen(next);
          if (!next) setContextOrdner(null);
        }}
        ordnerId={contextOrdner?.ordnerID ?? null}
        ordnerName={contextOrdner?.name ?? ""}
        ueberOrdnerId={contextOrdner?.ueberOrdnerID ?? null}
      />
    </View>
  );
}

/**
 * Page header above the table / card list.
 *
 * @returns The header strip.
 */
function Header() {
  return (
    <View className="border-b border-border px-4 py-3">
      <Text className="text-xl font-semibold text-foreground">
        {t("dateien.title", "Dateien")}
      </Text>
    </View>
  );
}

interface CreateOrdnerButtonProps {
  onPress: () => void;
  /** Compact label-less variant for the tree header. */
  compact?: boolean;
}

/**
 * Trigger button that opens the {@link CreateOrdnerDialog}.
 *
 * @param props - {@link CreateOrdnerButtonProps}
 * @returns A pressable button styled like the rest of the toolbar.
 */
function CreateOrdnerButton({
  onPress,
  compact = false,
}: CreateOrdnerButtonProps) {
  const label = t("ordner.create.button", "Ordner erstellen");
  return (
    <Pressable
      testID="create-ordner-button"
      accessibilityRole="button"
      accessibilityLabel={label}
      onPress={onPress}
      className={
        compact
          ? "rounded-md p-1.5 active:bg-accent"
          : "flex-row items-center gap-2 self-start rounded-md border border-border bg-card px-3 py-2 active:bg-accent"
      }
    >
      <FolderPlus size={16} color="hsl(var(--foreground))" />
      {!compact && (
        <Text className="text-sm font-medium text-foreground">{label}</Text>
      )}
    </Pressable>
  );
}
