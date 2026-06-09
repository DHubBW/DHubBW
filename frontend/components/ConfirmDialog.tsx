import * as React from "react";
import { ActivityIndicator } from "react-native";

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
import { t } from "@/lib/i18n/t";

/**
 * Props for the {@link ConfirmDialog} component.
 *
 * @public
 */
export type ConfirmDialogProps = {
  /** Controls dialog visibility. */
  open: boolean;
  /** Called when the dialog should open or close (e.g. backdrop tap, X button). */
  onOpenChange: (open: boolean) => void;
  /** Dialog heading. */
  title: string;
  /** Descriptive body text explaining the action. */
  message: string;
  /** Label for the confirm button. Defaults to `"Bestätigen"`. */
  confirmLabel?: string;
  /** Label for the cancel button. Defaults to `"Abbrechen"`. */
  cancelLabel?: string;
  /** Called when the user confirms. Can return a Promise; spinner shows while pending. */
  onConfirm: () => void | Promise<void>;
  /**
   * Visual variant of the confirm button.
   * - `'default'` — primary action (blue/primary colour)
   * - `'destructive'` — danger action (red)
   */
  variant?: "default" | "destructive";
};

/**
 * A reusable confirmation dialog with optional async support and loading state.
 *
 * Wraps the `Dialog` primitive family to present a titled message with confirm
 * and cancel buttons. When `onConfirm` returns a `Promise`, a spinner is shown
 * inside the confirm button and both buttons are disabled until the promise settles.
 *
 * The dialog cannot be dismissed (Cancel, X button, or backdrop) while an async
 * `onConfirm` is in flight — all close paths are blocked during the pending state.
 *
 * @param props - {@link ConfirmDialogProps}
 * @returns A controlled `Dialog` element.
 */
export function ConfirmDialog({
  open,
  onOpenChange,
  title,
  message,
  confirmLabel,
  cancelLabel,
  onConfirm,
  variant = "default",
}: ConfirmDialogProps): React.JSX.Element {
  const [isPending, setIsPending] = React.useState(false);

  // Reset isPending when the dialog is closed so stale spinner state
  // doesn't appear if the dialog is re-opened before the promise settles.
  React.useEffect(() => {
    if (!open) {
      setIsPending(false);
    }
  }, [open]);

  function handleOpenChange(nextOpen: boolean) {
    if (isPending) return;
    onOpenChange(nextOpen);
  }

  async function handleConfirm() {
    setIsPending(true);
    let succeeded = false;
    try {
      await Promise.resolve(onConfirm());
      succeeded = true;
    } catch {
      // Rejection handled by caller (toast / error state).
      // Dialog stays open on failure.
    } finally {
      setIsPending(false);
    }
    if (succeeded) {
      onOpenChange(false);
    }
  }

  const resolvedConfirmLabel =
    confirmLabel ?? t("confirm.confirm", "Bestätigen");
  const resolvedCancelLabel = cancelLabel ?? t("confirm.cancel", "Abbrechen");

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>{message}</DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button
            testID="confirm-dialog-cancel"
            variant="outline"
            onPress={() => handleOpenChange(false)}
            disabled={isPending}
          >
            <Text>{resolvedCancelLabel}</Text>
          </Button>
          <Button
            testID="confirm-dialog-confirm"
            variant={variant}
            onPress={handleConfirm}
            disabled={isPending}
          >
            {isPending ? (
              // ActivityIndicator.color cannot accept a NativeWind className.
              // --primary-foreground and --destructive-foreground both resolve
              // to white (#fff) in all themes, so this value is correct
              // regardless of the active theme.
              <ActivityIndicator
                testID="confirm-dialog-spinner"
                size="small"
                color="white"
              />
            ) : (
              <Text>{resolvedConfirmLabel}</Text>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
