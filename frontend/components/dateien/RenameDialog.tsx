/**
 * Modal dialog for the "Datei umbenennen" action.
 *
 * Pre-fills its single text input with the file's current name and
 * forwards the trimmed value to {@link useDateiRename} on confirm. The
 * confirm button is disabled while the input is empty (after trimming)
 * or unchanged from the original name. While the rename mutation is in
 * flight, both buttons are disabled and the confirm button shows a
 * spinner.
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
import { ApiError } from "@/lib/api/errors";
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
 * Maps an {@link ApiError} from the rename endpoint to the toast text
 * shown to the user.
 *
 * @param err - The thrown error from the mutation's onError callback.
 * @returns The localised toast message.
 */
function _renameErrorMessage(err: unknown): string {
  if (err instanceof ApiError) {
    if (err.status === 403) {
      return t(
        "dateien.rename.error.forbidden",
        "Keine Berechtigung zum Umbenennen.",
      );
    }
    if (err.status === 0 && err.message.includes("404")) {
      return t("dateien.rename.error.notFound", "Datei nicht gefunden.");
    }
    if (err.status === 0) {
      return t(
        "error.network",
        "Netzwerkfehler – Server nicht erreichbar",
      );
    }
  }
  return t("dateien.rename.error.unknown", "Umbenennen fehlgeschlagen.");
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
  const [value, setValue] = useState(currentName);
  // Reset the input whenever the dialog opens — a previous failed submit
  // could otherwise leak state into the next rename attempt.
  const lastOpenRef = useRef(open);
  useEffect(() => {
    if (open && !lastOpenRef.current) {
      setValue(currentName);
    }
    lastOpenRef.current = open;
  }, [open, currentName]);

  const trimmed = value.trim();
  const isUnchanged = trimmed === currentName.trim();
  const canSubmit =
    !rename.isPending &&
    dateiId !== null &&
    trimmed.length > 0 &&
    !isUnchanged;

  function handleOpenChange(nextOpen: boolean) {
    if (rename.isPending) return;
    onOpenChange(nextOpen);
  }

  function handleSubmit() {
    if (!canSubmit || dateiId === null) return;
    rename.mutate(
      { dateiId, newName: trimmed, ordnerId },
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
              "Gib einen neuen Namen für die Datei ein.",
            )}
          </DialogDescription>
        </DialogHeader>
        <View className="gap-1.5 py-2">
          <Label nativeID="rename-dialog-input-label">
            {t("dateien.rename.label", "Neuer Name")}
          </Label>
          <Input
            aria-labelledby="rename-dialog-input-label"
            testID="rename-dialog-input"
            value={value}
            onChangeText={setValue}
            autoCapitalize="none"
            autoCorrect={false}
            editable={!rename.isPending}
            onSubmitEditing={handleSubmit}
          />
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
