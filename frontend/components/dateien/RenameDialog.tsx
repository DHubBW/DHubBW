/**
 * Modal dialog for the "Datei umbenennen" action.
 *
 * The dialog only lets the user edit the file's <strong>base
 * name</strong>; the original extension is rendered read-only next
 * to the input so it cannot be changed via rename. The submit
 * payload is reassembled as {@code <base>.<originalExt>} before it
 * hits the API. The backend re-applies the same rule defensively, so
 * even a hand-rolled HTTP call can't flip the extension.
 *
 * Errors surface as toasts mapped from the {@link ApiError} status —
 * dialog stays open on failure so the user can retry. On success the
 * caller's `onClose` runs and a success toast fires.
 */
import { useEffect, useRef, useState } from "react";
import { ActivityIndicator, View } from "react-native";
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
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Text } from "@/components/ui/text";
import { useDateiRename } from "@/hooks/useDateiRename";
import { toastForApiError } from "@/lib/api/toastForApiError";
import { t } from "@/lib/i18n/t";

export interface RenameDialogProps {
  /** Controls dialog visibility. */
  open: boolean;
  /** Called when the dialog should open or close. */
  onOpenChange: (open: boolean) => void;
  /** UUID of the file to rename. `null` while no row is targeted. */
  dateiId: string | null;
  /** Current display name of the file. Pre-fills the input. */
  currentName: string;
  /**
   * UUID of the folder the file belongs to, or `null` for the root
   * view. Forwarded to the rename mutation for cache invalidation.
   */
  ordnerId: string | null;
}

/**
 * Splits a file name into {@code (base, ext)}. {@code ext} is
 * returned without the leading dot. When the name has no
 * extension, {@code ext} is an empty string.
 */
function _splitName(name: string): { base: string; ext: string } {
  const trimmed = name.trim();
  const dot = trimmed.lastIndexOf(".");
  if (dot <= 0 || dot === trimmed.length - 1) {
    return { base: trimmed, ext: "" };
  }
  return {
    base: trimmed.slice(0, dot),
    ext: trimmed.slice(dot + 1),
  };
}

/**
 * Maps an error from the rename endpoint to the toast text shown to
 * the user. Always prefers the backend's own German message text so
 * users see the precise reason (e.g. "Eine Datei mit diesem Namen
 * existiert bereits in diesem Ordner") and only falls back to the
 * localised hint when no body is available.
 *
 * @param err - The thrown error from the mutation's onError callback.
 * @returns The localised toast message.
 */
function _renameErrorMessage(err: unknown): string {
  return toastForApiError(err, {
    400: t(
      "dateien.rename.error.invalidName",
      "Dateiname darf nicht leer sein.",
    ),
    403: t(
      "dateien.rename.error.forbidden",
      "Keine Berechtigung zum Umbenennen.",
    ),
    404: t("dateien.rename.error.notFound", "Datei nicht gefunden."),
    409: t(
      "dateien.rename.error.duplicateName",
      "Eine Datei mit diesem Namen existiert bereits in diesem Ordner.",
    ),
  });
}

/**
 * Renders the rename dialog.
 *
 * @param props - {@link RenameDialogProps}
 * @returns The dialog element.
 */
export function RenameDialog({
  open,
  onOpenChange,
  dateiId,
  currentName,
  ordnerId,
}: RenameDialogProps) {
  const rename = useDateiRename();
  const { base: initialBase, ext: extension } = _splitName(currentName);
  const [base, setBase] = useState(initialBase);
  // Reset the input whenever the dialog opens — a previous failed submit
  // could otherwise leak state into the next rename attempt.
  const lastOpenRef = useRef(open);
  useEffect(() => {
    if (open && !lastOpenRef.current) {
      setBase(initialBase);
    }
    lastOpenRef.current = open;
  }, [open, initialBase]);

  const trimmedBase = base.trim();
  // UX: if the user types/pastes the full name including the locked
  // extension (e.g. copies "neu.pdf" from the explorer instead of just
  // the stem), strip the trailing ".<extension>" once before
  // re-appending it. Without this we'd ship "neu.pdf.pdf" to the
  // backend which is both wrong and silently rejected as a duplicate
  // for matching files.
  const baseWithoutDuplicateExt =
    extension.length > 0 &&
    trimmedBase.toLowerCase().endsWith(`.${extension.toLowerCase()}`)
      ? trimmedBase.slice(0, trimmedBase.length - extension.length - 1)
      : trimmedBase;
  const fullName =
    extension.length > 0
      ? `${baseWithoutDuplicateExt}.${extension}`
      : baseWithoutDuplicateExt;
  const isUnchanged = baseWithoutDuplicateExt === initialBase.trim();
  const canSubmit =
    !rename.isPending &&
    dateiId !== null &&
    baseWithoutDuplicateExt.length > 0 &&
    !isUnchanged;

  function handleOpenChange(nextOpen: boolean) {
    if (rename.isPending) return;
    onOpenChange(nextOpen);
  }

  function handleSubmit() {
    if (!canSubmit || dateiId === null) return;
    rename.mutate(
      { dateiId, newName: fullName, ordnerId },
      {
        onSuccess: () => {
          toast.success(t("dateien.rename.success", "Datei umbenannt"));
          onOpenChange(false);
        },
        onError: (err) => {
          toast.error(_renameErrorMessage(err));
        },
      },
    );
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>
            {t("dateien.rename.title", "Datei umbenennen")}
          </DialogTitle>
          <DialogDescription>
            {t(
              "dateien.rename.description",
              "Gib einen neuen Namen für die Datei ein. Die "
                + "Dateiendung bleibt unverändert.",
            )}
          </DialogDescription>
        </DialogHeader>
        <View className="gap-1.5 py-2">
          <Label nativeID="rename-dialog-input-label">
            {t("dateien.rename.label", "Neuer Name")}
          </Label>
          <View className="flex-row items-center gap-2">
            <View className="flex-1">
              <Input
                aria-labelledby="rename-dialog-input-label"
                testID="rename-dialog-input"
                value={base}
                onChangeText={setBase}
                autoCapitalize="none"
                autoCorrect={false}
                editable={!rename.isPending}
                onSubmitEditing={handleSubmit}
              />
            </View>
            {extension.length > 0 && (
              <View
                className="rounded-md border border-border bg-muted px-3 py-2"
                accessibilityLabel={t(
                  "dateien.rename.extensionLocked",
                  "Dateiendung ist gesperrt",
                )}
                testID="rename-dialog-extension"
              >
                <Text className="text-sm font-medium tabular-nums text-muted-foreground">
                  .{extension}
                </Text>
              </View>
            )}
          </View>
        </View>
        <DialogFooter>
          <Button
            testID="rename-dialog-cancel"
            variant="outline"
            onPress={() => handleOpenChange(false)}
            disabled={rename.isPending}
          >
            <Text>{t("confirm.cancel", "Abbrechen")}</Text>
          </Button>
          <Button
            testID="rename-dialog-confirm"
            onPress={handleSubmit}
            disabled={!canSubmit}
          >
            {rename.isPending ? (
              <ActivityIndicator
                testID="rename-dialog-spinner"
                size="small"
                color="white"
              />
            ) : (
              <Text>{t("dateien.rename.confirm", "Umbenennen")}</Text>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
