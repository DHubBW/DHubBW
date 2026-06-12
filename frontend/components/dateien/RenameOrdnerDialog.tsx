/**
 * Modal dialog for the "Ordner umbenennen" action.
 *
 * Pre-fills its single text input with the folder's current name and
 * forwards the trimmed value to {@link useOrdnerRename} on confirm. The
 * confirm button is disabled while the input is empty (after trimming)
 * or unchanged from the original name. While the rename mutation is in
 * flight, both buttons are disabled and the confirm button shows a
 * spinner.
 *
 * Errors surface as toasts mapped from the {@link ApiError} status —
 * dialog stays open on failure so the user can retry. On success the
 * caller's `onOpenChange(false)` runs and a success toast fires.
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
import { useOrdnerRename } from "@/hooks/useOrdnerRename";
import { toastForApiError } from "@/lib/api/toastForApiError";
import { t } from "@/lib/i18n/t";

export interface RenameOrdnerDialogProps {
  /** Controls dialog visibility. */
  open: boolean;
  /** Called when the dialog should open or close. */
  onOpenChange: (open: boolean) => void;
  /** UUID of the folder to rename. `null` while no folder is targeted. */
  ordnerId: string | null;
  /** Current display name of the folder. Pre-fills the input. */
  currentName: string;
  /**
   * Parent folder UUID, or `null` for a top-level folder. Forwarded to
   * the rename mutation for cache invalidation.
   */
  ueberOrdnerId: string | null;
}

/**
 * Maps an error from the rename endpoint to the toast text shown to
 * the user. Always prefers the backend's own German message text so
 * users see the precise reason (e.g. "Ein Ordner mit diesem Namen
 * existiert bereits an gleicher Stelle") and only falls back to the
 * localised hint when no body is available.
 *
 * @param err - The thrown error from the mutation's onError callback.
 * @returns The localised toast message.
 */
function _renameErrorMessage(err: unknown): string {
  return toastForApiError(err, {
    403: t(
      "ordner.rename.error.forbidden",
      "Keine Berechtigung zum Umbenennen.",
    ),
    404: t("ordner.rename.error.notFound", "Ordner nicht gefunden."),
    409: t(
      "ordner.rename.error.duplicateName",
      "Ein Ordner mit diesem Namen existiert bereits an gleicher Stelle.",
    ),
  });
}

/**
 * Renders the rename-folder dialog.
 *
 * @param props - {@link RenameOrdnerDialogProps}
 * @returns The dialog element.
 */
export function RenameOrdnerDialog({
  open,
  onOpenChange,
  ordnerId,
  currentName,
  ueberOrdnerId,
}: RenameOrdnerDialogProps) {
  const rename = useOrdnerRename();
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
    ordnerId !== null &&
    trimmed.length > 0 &&
    !isUnchanged;

  function handleOpenChange(nextOpen: boolean) {
    if (rename.isPending) return;
    onOpenChange(nextOpen);
  }

  function handleSubmit() {
    if (!canSubmit || ordnerId === null) return;
    rename.mutate(
      { ordnerId, newName: trimmed, ueberOrdnerId },
      {
        onSuccess: () => {
          toast.success(t("ordner.rename.success", "Ordner umbenannt"));
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
            {t("ordner.rename.title", "Ordner umbenennen")}
          </DialogTitle>
          <DialogDescription>
            {t(
              "ordner.rename.description",
              "Gib einen neuen Namen für den Ordner ein.",
            )}
          </DialogDescription>
        </DialogHeader>
        <View className="gap-1.5 py-2">
          <Label nativeID="rename-ordner-input-label">
            {t("ordner.rename.label", "Neuer Name")}
          </Label>
          <Input
            aria-labelledby="rename-ordner-input-label"
            testID="rename-ordner-input"
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
            testID="rename-ordner-cancel"
            variant="outline"
            onPress={() => handleOpenChange(false)}
            disabled={rename.isPending}
          >
            <Text>{t("confirm.cancel", "Abbrechen")}</Text>
          </Button>
          <Button
            testID="rename-ordner-confirm"
            onPress={handleSubmit}
            disabled={!canSubmit}
          >
            {rename.isPending ? (
              <ActivityIndicator
                testID="rename-ordner-spinner"
                size="small"
                color="white"
              />
            ) : (
              <Text>{t("ordner.rename.confirm", "Umbenennen")}</Text>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
