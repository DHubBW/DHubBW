/**
 * Modal dialog showing the comment thread of a single file.
 *
 * Loads the comment list via {@link useKommentareForDatei} and lets
 * any user with read access to the file post a new comment via
 * {@link useKommentarCreate}. Each comment row offers Edit / Delete
 * affordances — gated by the per-row {@code canEdit} / {@code canDelete}
 * flags returned from the backend, which already encode the
 * Author-only edit and Author/KURSADMIN-of-folder/SYSTEMADMIN delete
 * rules. The dialog therefore never derives those rules from the
 * caller's role itself.
 *
 * Errors surface as toasts mapped from the {@link ApiError} status; the
 * dialog stays open on failure so the user can retry.
 */
import { Pencil, Trash2 } from "lucide-react-native";
import { useEffect, useState } from "react";
import { ActivityIndicator, Pressable, ScrollView, View } from "react-native";
import { toast } from "sonner-native";

import { ConfirmDialog } from "@/components/ConfirmDialog";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Text } from "@/components/ui/text";
import { useKommentareForDatei } from "@/hooks/useKommentareForDatei";
import { useKommentarCreate } from "@/hooks/useKommentarCreate";
import { useKommentarDelete } from "@/hooks/useKommentarDelete";
import { useKommentarUpdate } from "@/hooks/useKommentarUpdate";
import { toastForApiError } from "@/lib/api/toastForApiError";
import type { DateiDTO, KommentarDTO } from "@/lib/api/types.gen";
import { t } from "@/lib/i18n/t";

export interface KommentarDialogProps {
  /** Controls dialog visibility. */
  open: boolean;
  /** Called when the dialog should open or close. */
  onOpenChange: (open: boolean) => void;
  /** File whose comments are shown. `null` while no row is targeted. */
  datei: DateiDTO | null;
}

/**
 * Maps an error from the create endpoint to a localised toast.
 *
 * @param err - thrown error from the mutation.
 * @returns the toast text.
 */
function _createErrorMessage(err: unknown): string {
  return toastForApiError(err, {
    400: t(
      "kommentare.create.error.invalidInhalt",
      "Kommentar darf nicht leer sein.",
    ),
    403: t(
      "kommentare.create.error.forbidden",
      "Keine Berechtigung zum Kommentieren.",
    ),
    404: t("kommentare.create.error.notFound", "Datei nicht gefunden."),
  });
}

/**
 * Maps an error from the update endpoint to a localised toast.
 *
 * @param err - thrown error from the mutation.
 * @returns the toast text.
 */
function _updateErrorMessage(err: unknown): string {
  return toastForApiError(err, {
    400: t(
      "kommentare.update.error.invalidInhalt",
      "Kommentar darf nicht leer sein.",
    ),
    403: t(
      "kommentare.update.error.forbidden",
      "Nur der Autor darf den Kommentar bearbeiten.",
    ),
    404: t("kommentare.update.error.notFound", "Kommentar nicht gefunden."),
  });
}

/**
 * Maps an error from the delete endpoint to a localised toast.
 *
 * @param err - thrown error from the mutation.
 * @returns the toast text.
 */
function _deleteErrorMessage(err: unknown): string {
  return toastForApiError(err, {
    403: t(
      "kommentare.delete.error.forbidden",
      "Keine Berechtigung zum Löschen des Kommentars.",
    ),
    404: t("kommentare.delete.error.notFound", "Kommentar nicht gefunden."),
  });
}

/**
 * Format an ISO `LocalDateTime` string ({@code "YYYY-MM-DDTHH:mm:ss"})
 * into a human-readable German timestamp. Falls back to the raw input
 * when parsing fails.
 *
 * @param iso - ISO date-time string.
 * @returns formatted timestamp like {@code "26.04.2026, 09:15"}.
 */
function _formatTimestamp(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return iso;
  return date.toLocaleString("de-DE", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

/**
 * Renders the comment-thread dialog for a single file.
 *
 * @param props - {@link KommentarDialogProps}
 * @returns the dialog element.
 */
export function KommentarDialog({
  open,
  onOpenChange,
  datei,
}: KommentarDialogProps) {
  const dateiId = datei?.dateiID ?? null;
  const kommentare = useKommentareForDatei(dateiId);
  const create = useKommentarCreate();
  const update = useKommentarUpdate();
  const remove = useKommentarDelete();

  const [draft, setDraft] = useState("");
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editingDraft, setEditingDraft] = useState("");
  const [deleteTarget, setDeleteTarget] = useState<KommentarDTO | null>(null);

  // Reset transient state when the dialog opens for a different file
  // or closes — otherwise an aborted edit would leak between targets.
  useEffect(() => {
    if (!open) {
      setDraft("");
      setEditingId(null);
      setEditingDraft("");
      setDeleteTarget(null);
    }
  }, [open, dateiId]);

  function handleOpenChange(nextOpen: boolean) {
    if (create.isPending || update.isPending || remove.isPending) {
      return;
    }
    onOpenChange(nextOpen);
  }

  function handleCreate() {
    if (!dateiId) return;
    const trimmed = draft.trim();
    if (trimmed.length === 0) return;
    create.mutate(
      { dateiId, inhalt: trimmed },
      {
        onSuccess: () => {
          setDraft("");
          toast.success(
            t("kommentare.create.success", "Kommentar veröffentlicht"),
          );
        },
        onError: (err) => {
          toast.error(_createErrorMessage(err));
        },
      },
    );
  }

  function startEdit(kommentar: KommentarDTO) {
    setEditingId(kommentar.kommentarID);
    setEditingDraft(kommentar.inhalt);
  }

  function cancelEdit() {
    setEditingId(null);
    setEditingDraft("");
  }

  function handleUpdate() {
    if (!dateiId || !editingId) return;
    const trimmed = editingDraft.trim();
    if (trimmed.length === 0) return;
    update.mutate(
      { kommentarId: editingId, inhalt: trimmed, dateiId },
      {
        onSuccess: () => {
          setEditingId(null);
          setEditingDraft("");
          toast.success(
            t("kommentare.update.success", "Kommentar aktualisiert"),
          );
        },
        onError: (err) => {
          toast.error(_updateErrorMessage(err));
        },
      },
    );
  }

  async function handleConfirmDelete() {
    if (!dateiId || !deleteTarget) return;
    try {
      await remove.mutateAsync({
        kommentarId: deleteTarget.kommentarID,
        dateiId,
      });
      toast.success(t("kommentare.delete.success", "Kommentar gelöscht"));
      setDeleteTarget(null);
    } catch (err) {
      toast.error(_deleteErrorMessage(err));
      throw err;
    }
  }

  const items = kommentare.data ?? [];
  const isCreateDisabled =
    !dateiId || create.isPending || draft.trim().length === 0;

  return (
    <>
      <Dialog open={open} onOpenChange={handleOpenChange}>
        <DialogContent
          className="h-[80vh] w-[95vw] max-w-[640px] sm:max-w-[640px]"
          testID="kommentar-dialog"
        >
          <DialogHeader>
            <DialogTitle>
              {t("kommentare.title", "Kommentare")}
              {datei?.name ? ` – ${datei.name}` : ""}
            </DialogTitle>
            <DialogDescription>
              {t(
                "kommentare.description",
                "Diskutiere die Datei mit deinen Kursteilnehmer:innen.",
              )}
            </DialogDescription>
          </DialogHeader>

          <ScrollView
            className="flex-1"
            contentContainerClassName="gap-3 py-2"
            testID="kommentar-list"
          >
            {kommentare.isLoading && (
              <View className="items-center justify-center py-8">
                <ActivityIndicator color="hsl(var(--muted-foreground))" />
              </View>
            )}
            {!kommentare.isLoading && items.length === 0 && (
              <View className="items-center justify-center py-8">
                <Text className="text-sm text-muted-foreground">
                  {t(
                    "kommentare.empty",
                    "Noch keine Kommentare. Sei der erste!",
                  )}
                </Text>
              </View>
            )}
            {items.map((kommentar) => (
              <KommentarRow
                key={kommentar.kommentarID}
                kommentar={kommentar}
                isEditing={editingId === kommentar.kommentarID}
                editingDraft={editingDraft}
                onEditDraftChange={setEditingDraft}
                onStartEdit={() => startEdit(kommentar)}
                onCancelEdit={cancelEdit}
                onSaveEdit={handleUpdate}
                onRequestDelete={() => setDeleteTarget(kommentar)}
                isUpdatePending={update.isPending}
              />
            ))}
          </ScrollView>

          <View className="gap-1.5 border-t border-border pt-3">
            <Label nativeID="kommentar-new-input-label">
              {t("kommentare.create.label", "Neuer Kommentar")}
            </Label>
            <Input
              aria-labelledby="kommentar-new-input-label"
              testID="kommentar-new-input"
              value={draft}
              onChangeText={setDraft}
              placeholder={t(
                "kommentare.create.placeholder",
                "Schreibe einen Kommentar …",
              )}
              editable={!create.isPending && dateiId !== null}
              onSubmitEditing={handleCreate}
            />
          </View>

          <DialogFooter>
            <Button
              testID="kommentar-dialog-close"
              variant="outline"
              onPress={() => handleOpenChange(false)}
              disabled={
                create.isPending || update.isPending || remove.isPending
              }
            >
              <Text>{t("confirm.close", "Schließen")}</Text>
            </Button>
            <Button
              testID="kommentar-dialog-submit"
              onPress={handleCreate}
              disabled={isCreateDisabled}
            >
              {create.isPending ? (
                <ActivityIndicator size="small" color="white" />
              ) : (
                <Text>{t("kommentare.create.submit", "Kommentar senden")}</Text>
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={deleteTarget !== null}
        onOpenChange={(next) => {
          if (!next) setDeleteTarget(null);
        }}
        title={t("kommentare.delete.title", "Kommentar löschen?")}
        message={t(
          "kommentare.delete.message",
          "Der Kommentar wird unwiderruflich gelöscht.",
        )}
        confirmLabel={t("kommentare.delete.confirm", "Löschen")}
        variant="destructive"
        onConfirm={handleConfirmDelete}
      />
    </>
  );
}

interface KommentarRowProps {
  kommentar: KommentarDTO;
  isEditing: boolean;
  editingDraft: string;
  onEditDraftChange: (value: string) => void;
  onStartEdit: () => void;
  onCancelEdit: () => void;
  onSaveEdit: () => void;
  onRequestDelete: () => void;
  isUpdatePending: boolean;
}

/**
 * Single comment row inside the thread. Renders either the read-only
 * display or the inline edit form, depending on {@code isEditing}.
 */
function KommentarRow({
  kommentar,
  isEditing,
  editingDraft,
  onEditDraftChange,
  onStartEdit,
  onCancelEdit,
  onSaveEdit,
  onRequestDelete,
  isUpdatePending,
}: KommentarRowProps) {
  const author =
    kommentar.autorName ?? t("kommentare.unknownAuthor", "Unbekannt");
  const created = _formatTimestamp(kommentar.erstelltAm);
  const editedAt = kommentar.zuletztBearbeitet
    ? _formatTimestamp(kommentar.zuletztBearbeitet)
    : null;

  return (
    <View
      className="rounded-md border border-border bg-card p-3"
      testID={`kommentar-row-${kommentar.kommentarID}`}
    >
      <View className="flex-row items-start justify-between gap-2">
        <View className="flex-1">
          <Text className="text-sm font-semibold text-foreground">
            {author}
          </Text>
          <Text className="text-xs text-muted-foreground">
            {created}
            {kommentar.bearbeitet && editedAt
              ? ` · ${t("kommentare.editedAt", "bearbeitet")} ${editedAt}`
              : ""}
          </Text>
        </View>
        {!isEditing && (kommentar.canEdit || kommentar.canDelete) && (
          <View className="flex-row gap-1">
            {kommentar.canEdit && (
              <Pressable
                accessibilityRole="button"
                accessibilityLabel={t(
                  "kommentare.actions.edit",
                  "Kommentar bearbeiten",
                )}
                onPress={onStartEdit}
                className="rounded-md p-1.5 active:bg-accent"
                testID={`kommentar-edit-${kommentar.kommentarID}`}
              >
                <Pencil size={14} color="hsl(var(--foreground))" />
              </Pressable>
            )}
            {kommentar.canDelete && (
              <Pressable
                accessibilityRole="button"
                accessibilityLabel={t(
                  "kommentare.actions.delete",
                  "Kommentar löschen",
                )}
                onPress={onRequestDelete}
                className="rounded-md p-1.5 active:bg-accent"
                testID={`kommentar-delete-${kommentar.kommentarID}`}
              >
                <Trash2 size={14} color="hsl(var(--destructive))" />
              </Pressable>
            )}
          </View>
        )}
      </View>
      {isEditing ? (
        <View className="mt-2 gap-2">
          <Input
            value={editingDraft}
            onChangeText={onEditDraftChange}
            editable={!isUpdatePending}
            onSubmitEditing={onSaveEdit}
            testID={`kommentar-edit-input-${kommentar.kommentarID}`}
          />
          <View className="flex-row justify-end gap-2">
            <Button
              variant="outline"
              onPress={onCancelEdit}
              disabled={isUpdatePending}
              testID={`kommentar-edit-cancel-${kommentar.kommentarID}`}
            >
              <Text>{t("confirm.cancel", "Abbrechen")}</Text>
            </Button>
            <Button
              onPress={onSaveEdit}
              disabled={isUpdatePending || editingDraft.trim().length === 0}
              testID={`kommentar-edit-save-${kommentar.kommentarID}`}
            >
              {isUpdatePending ? (
                <ActivityIndicator size="small" color="white" />
              ) : (
                <Text>{t("kommentare.update.submit", "Speichern")}</Text>
              )}
            </Button>
          </View>
        </View>
      ) : (
        <Text className="mt-2 text-sm text-foreground">{kommentar.inhalt}</Text>
      )}
    </View>
  );
}
